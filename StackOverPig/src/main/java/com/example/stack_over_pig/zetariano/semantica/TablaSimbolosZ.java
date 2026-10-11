package com.example.stack_over_pig.zetariano.semantica;

import com.example.stack_over_pig.comun.JerarquiaClases;
import com.example.stack_over_pig.zetariano.nodo.Visibilidad;

import java.util.*;

public class TablaSimbolosZ {

    public enum Categoria { VARIABLE, ATRIBUTO, PARAMETRO, CONSTRUCTOR, METODO, CLASE }

    /** Un parametro con su tipo, para poder comparar firmas y resolver llamadas. */
    public record Parametro(String nombre, String tipo, int dimensiones) {}

    /** Una variable declarada en un scope (local o parametro ya registrado como variable). */
    public record SimboloVariable(String nombre, String tipo, int dimensiones, Integer tamanoConocido) {}

    /** Un atributo de la clase ('String nombre;') con su nivel de acceso y la clase que lo declara. */
    public record SimboloAtributo(String nombre, String tipo, int dimensiones,
                                  Visibilidad visibilidad, String claseDeclarante) {}

    /**
     * Una firma de constructor o metodo. 'nombre' se repite para todas las
     * sobrecargas de un mismo constructor/metodo; lo que las distingue es 'parametros'.
     * 'claseDeclarante' es la clase donde esta escrito (importa cuando hay herencia).
     */
    public record Firma(String nombre, List<Parametro> parametros, String tipoRetorno,
                        Visibilidad visibilidad, boolean esOverride, String claseDeclarante) {
        public List<String> tiposParametros() {
            return parametros.stream().map(Parametro::tipo).toList();
        }
    }

    public record EntradaSimbolo(int id, String nombre, Categoria categoria, String tipo,
                                 int numParametros, String ambito, int alcance) {}



    //scope interno (variables locales dentro de un bloque/metodo)
    private static class Scope {
        final String etiqueta;
        final Map<String, SimboloVariable> variables = new LinkedHashMap<>();

        Scope(String etiqueta) { this.etiqueta = etiqueta; }
    }

    private final Deque<Scope> pila = new ArrayDeque<>();

    //la clase unica del archivo
    private String nombreClase;
    private String superclase;              // null si la clase no usa 'extends'
    private Visibilidad visibilidadClase;
    private final Map<String, SimboloAtributo> atributos = new LinkedHashMap<>();

    //nombre -> lista de firmas (para constructores y metodos, ambos soportan sobrecarga)
    private final Map<String, List<Firma>> constructores = new LinkedHashMap<>();
    private final Map<String, List<Firma>> metodos = new LinkedHashMap<>();

    private final List<EntradaSimbolo> registro = new ArrayList<>();
    private int siguienteId = 1;

    //herencia: sabe la superclase de la clase actual y la de las clases externas
    private final JerarquiaClases jerarquia = new JerarquiaClases(this::superclaseDe);

    //paquete (carpeta) del archivo que se esta analizando; "" si esta en la raiz
    private String paqueteActual = "";

    public TablaSimbolosZ() {
        pila.push(new Scope("global"));
    }

    // CLASE
    public void declararClase(String nombre, String superclase, Visibilidad visibilidad) {
        this.nombreClase = nombre;
        this.superclase = superclase;
        this.visibilidadClase = visibilidad;
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.CLASE, nombre, 0, "global", alcanceActual()));
    }

    public String getNombreClase() {
        return nombreClase;
    }

    /** Nombre de la superclase de la clase actual, o null si no hereda de ninguna. */
    public String getSuperclase() {
        return superclase;
    }

    public Visibilidad getVisibilidadClase() {
        return visibilidadClase;
    }

    public void setPaqueteActual(String paquete) {
        this.paqueteActual = paquete == null ? "" : paquete;
    }

    public String getPaqueteActual() {
        return paqueteActual;
    }

    /** Paquete de una clase (la actual o una externa); "" si no se conoce. */
    public String paqueteDe(String clase) {
        if (clase.equals(nombreClase)) return paqueteActual;
        return buscarClaseExterna(clase).map(DefinicionClaseExterna::paquete).orElse("");
    }

    // ATRIBUTOS

    /** Devuelve false si ya existe un atributo con ese nombre (no se permite repetir, a diferencia de los metodos). */
    public boolean declararAtributo(String nombre, String tipo, int dimensiones, Visibilidad visibilidad) {
        if (atributos.containsKey(nombre)) {
            return false;
        }
        atributos.put(nombre, new SimboloAtributo(nombre, tipo, dimensiones, visibilidad, nombreClase));
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.ATRIBUTO, tipo, 0, "clase", alcanceActual()));
        return true;
    }

    /** Atributo declarado directamente en la clase actual (no mira los ancestros). */
    public Optional<SimboloAtributo> buscarAtributo(String nombre) {
        return Optional.ofNullable(atributos.get(nombre));
    }

    // SCOPES Y VARIABLES LOCALES
    public void entrarScope(String etiqueta) {
        pila.push(new Scope(etiqueta));
    }

    public void entrarScope() {
        entrarScope("bloque");
    }

    public void salirScope() {
        if (pila.size() == 1) {
            throw new IllegalStateException("No se puede salir del scope global");
        }
        pila.pop();
    }

    private String ambitoActual() {
        return pila.peek().etiqueta;
    }

    private int alcanceActual() {
        return pila.size();
    }

    public boolean declararVariable(String nombre, String tipo) {
        return declararVariable(nombre, tipo, 0);
    }

    public boolean declararVariable(String nombre, String tipo, int dimensiones) {
        return declararVariable(nombre, tipo, dimensiones, null);
    }

    public boolean declararVariable(String nombre, String tipo, int dimensiones, Integer tamanoConocido) {
        Scope actual = pila.peek();
        if (actual.variables.containsKey(nombre)) {
            return false;
        }
        actual.variables.put(nombre, new SimboloVariable(nombre, tipo, dimensiones, tamanoConocido));
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.VARIABLE, tipo, 0,
                ambitoActual(), alcanceActual()));
        return true;
    }

    /** true si 'nombre' es una variable o parametro local visible desde el scope actual (no un atributo). */
    public boolean existeVariableLocal(String nombre) {
        for (Scope s : pila) {
            if (s.variables.containsKey(nombre)) return true;
        }
        return false;
    }

    /**
     * Busca primero en los scopes locales (de adentro hacia afuera), y si no aparece,
     * revisa los atributos de la clase y de sus ancestros -- esto modela el 'this.' implicito.
     * (La visibilidad de lo heredado todavia no se revisa aqui.)
     */
    public Optional<SimboloVariable> buscarVariable(String nombre) {
        for (Scope s : pila) {
            SimboloVariable v = s.variables.get(nombre);
            if (v != null) return Optional.of(v);
        }
        return buscarAtributoEn(nombreClase, nombre)
                .map(a -> new SimboloVariable(a.nombre(), a.tipo(), a.dimensiones(), null));
    }

    // CONSTRUCTORES (con sobrecarga)

    /**
     * Devuelve false si ya existe una firma IDENTICA (mismos tipos de parametro en el mismo
     * orden) para este constructor -- eso si es error. Firmas distintas del mismo nombre
     * (sobrecarga real) se permiten y se acumulan en la lista.
     */
    public boolean declararConstructor(String nombre, List<Parametro> parametros, Visibilidad visibilidad) {
        List<Firma> firmas = constructores.computeIfAbsent(nombre, k -> new ArrayList<>());
        Firma nueva = new Firma(nombre, parametros, null, visibilidad, false, nombreClase);
        if (existeFirmaIdentica(firmas, nueva)) {
            return false;
        }
        firmas.add(nueva);
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.CONSTRUCTOR, null,
                parametros.size(), "clase", alcanceActual()));
        return true;
    }

    public List<Firma> getConstructores(String nombre) {
        return constructores.getOrDefault(nombre, List.of());
    }

    // METODOS (con sobrecarga)
    public boolean declararMetodo(String nombre, List<Parametro> parametros, String tipoRetorno,
                                  Visibilidad visibilidad, boolean esOverride) {
        List<Firma> firmas = metodos.computeIfAbsent(nombre, k -> new ArrayList<>());
        Firma nueva = new Firma(nombre, parametros, tipoRetorno, visibilidad, esOverride, nombreClase);
        if (existeFirmaIdentica(firmas, nueva)) {
            return false;
        }
        firmas.add(nueva);
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.METODO, tipoRetorno,
                parametros.size(), "clase", alcanceActual()));
        return true;
    }

    /** Metodos con ese nombre declarados directamente en la clase actual (no mira los ancestros). */
    public List<Firma> getMetodos(String nombre) {
        return metodos.getOrDefault(nombre, List.of());
    }

    // HELPERS DE RESOLUCION DE FIRMAS
    private boolean existeFirmaIdentica(List<Firma> firmas, Firma candidata) {
        for (Firma f : firmas) {
            if (f.tiposParametros().equals(candidata.tiposParametros())) {
                return true;
            }
        }
        return false;
    }

    // UI / DEPURACION
    public List<EntradaSimbolo> getRegistroCompleto() {
        return Collections.unmodifiableList(registro);
    }

    // CLASES EXTERNAS (para resolver referencias cruzadas entre archivos .z)

    /**
     * Definición "resumida" de una clase .z externa al archivo actual.
     * No es la clase completa (no tiene cuerpos de métodos), solo lo necesario
     * para resolver tipos, accesos a atributos, sobrecarga y herencia.
     *
     * 'superclase' es null si no hereda de nadie. 'paquete' es la carpeta del archivo
     * (relativa a la raiz del proyecto; "" si esta en la raiz).
     */
    public record DefinicionClaseExterna(
            String nombre,
            String superclase,
            Visibilidad visibilidad,
            String paquete,
            Map<String, SimboloAtributo> atributos,
            Map<String, List<Firma>> constructores,
            Map<String, List<Firma>> metodos) {}

    private final Map<String, DefinicionClaseExterna> clasesExternas = new LinkedHashMap<>();

    public void registrarClaseExterna(DefinicionClaseExterna def) {
        clasesExternas.put(def.nombre(), def);
    }

    public Optional<DefinicionClaseExterna> buscarClaseExterna(String nombre) {
        return Optional.ofNullable(clasesExternas.get(nombre));
    }

    /** true si 'nombre' es la clase del archivo actual o una clase externa registrada. */
    public boolean esClaseConocida(String nombre) {
        if (nombre == null) return false;
        return nombre.equals(nombreClase) || clasesExternas.containsKey(nombre);
    }

    // HERENCIA

    /** Superclase de una clase (la actual o una externa); null si no hereda de ninguna o no se conoce. */
    private String superclaseDe(String clase) {
        if (clase.equals(nombreClase)) return superclase;
        return buscarClaseExterna(clase).map(DefinicionClaseExterna::superclase).orElse(null);
    }

    /** La clase y sus ancestros, de la mas derivada a la raiz. */
    public List<String> cadenaDe(String clase) {
        return jerarquia.cadena(clase);
    }

    /** true si 'sub' es 'sup' o hereda de ella. */
    public boolean esSubtipo(String sub, String sup) {
        return jerarquia.esSubtipo(sub, sup);
    }

    /** true si la herencia de 'clase' es circular. */
    public boolean tieneHerenciaCircular(String clase) {
        return jerarquia.tieneCiclo(clase);
    }

    private Map<String, SimboloAtributo> atributosDe(String clase) {
        if (clase.equals(nombreClase)) return atributos;
        return buscarClaseExterna(clase).map(DefinicionClaseExterna::atributos).orElse(Map.of());
    }

    private Map<String, List<Firma>> metodosDe(String clase) {
        if (clase.equals(nombreClase)) return metodos;
        return buscarClaseExterna(clase).map(DefinicionClaseExterna::metodos).orElse(Map.of());
    }

    /** Busca un atributo en la clase indicada y, si no esta ahi, en sus ancestros (gana el mas cercano). */
    public Optional<SimboloAtributo> buscarAtributoEn(String clase, String atributo) {
        for (String c : jerarquia.cadena(clase)) {
            SimboloAtributo a = atributosDe(c).get(atributo);
            if (a != null) return Optional.of(a);
        }
        return Optional.empty();
    }

    /**
     * Metodos con ese nombre de la clase y de sus ancestros. Si una clase derivada declara
     * una firma identica a la de su padre, la del padre queda sobrescrita y no se incluye.
     */
    public List<Firma> getMetodosDe(String clase, String nombreMetodo) {
        List<Firma> resultado = new ArrayList<>();
        for (String c : jerarquia.cadena(clase)) {
            for (Firma f : metodosDe(c).getOrDefault(nombreMetodo, List.of())) {
                if (!existeFirmaIdentica(resultado, f)) resultado.add(f);
            }
        }
        return resultado;
    }

    /** Constructores de una clase (actual o externa). Los constructores no se heredan. */
    public List<Firma> getConstructoresDe(String clase) {
        if (clase == null) return List.of();
        if (clase.equals(nombreClase)) return getConstructores(clase);
        return buscarClaseExterna(clase)
                .map(d -> d.constructores().getOrDefault(clase, List.of()))
                .orElse(List.of());
    }
}