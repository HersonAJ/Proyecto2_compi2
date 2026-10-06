package com.example.contacto_3xtrat3r3str3.zetariano.semantica;

import java.util.*;

public class TablaSimbolosZ {

    public enum Categoria { VARIABLE, ATRIBUTO, PARAMETRO, CONSTRUCTOR, METODO, CLASE }

    /** Un parametro con su tipo, para poder comparar firmas y resolver llamadas. */
    public record Parametro(String nombre, String tipo, int dimensiones) {}

    /** Una variable declarada en un scope (local o parametro ya registrado como variable). */
    public record SimboloVariable(String nombre, String tipo, int dimensiones, Integer tamanoConocido) {}

    /** Un atributo publico de la clase: 'String nombre;'. */
    public record SimboloAtributo(String nombre, String tipo, int dimensiones) {}

    /**
     * Una firma de constructor o metodo. 'nombre' se repite para todas las
     * sobrecargas de un mismo constructor/metodo; lo que las distingue es 'parametros'.
     */
    public record Firma(String nombre, List<Parametro> parametros, String tipoRetorno) {
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
    private final Map<String, SimboloAtributo> atributos = new LinkedHashMap<>();

    //nombre -> lista de firmas (para constructores y metodos, ambos soportan sobrecarga)
    private final Map<String, List<Firma>> constructores = new LinkedHashMap<>();
    private final Map<String, List<Firma>> metodos = new LinkedHashMap<>();

    private final List<EntradaSimbolo> registro = new ArrayList<>();
    private int siguienteId = 1;

    public TablaSimbolosZ() {
        pila.push(new Scope("global"));
    }

    // CLASE
    public void declararClase(String nombre) {
        this.nombreClase = nombre;
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.CLASE, nombre, 0, "global", alcanceActual()));
    }

    public String getNombreClase() {
        return nombreClase;
    }

    // ATRIBUTOS

    /** Devuelve false si ya existe un atributo con ese nombre (no se permite repetir, a diferencia de los metodos). */
    public boolean declararAtributo(String nombre, String tipo, int dimensiones) {
        if (atributos.containsKey(nombre)) {
            return false;
        }
        atributos.put(nombre, new SimboloAtributo(nombre, tipo, dimensiones));
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.ATRIBUTO, tipo, 0, "clase", alcanceActual()));
        return true;
    }

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

    /**
     * Busca primero en los scopes locales (de adentro hacia afuera), y si no aparece,
     * revisa los atributos de la clase -- esto modela el 'this.' implicito de Zetariano.
     */
    public Optional<SimboloVariable> buscarVariable(String nombre) {
        for (Scope s : pila) {
            SimboloVariable v = s.variables.get(nombre);
            if (v != null) return Optional.of(v);
        }
        return buscarAtributo(nombre)
                .map(a -> new SimboloVariable(a.nombre(), a.tipo(), a.dimensiones(), null));
    }

    // CONSTRUCTORES (con sobrecarga)

    /**
     * Devuelve false si ya existe una firma IDENTICA (mismos tipos de parametro en el mismo
     * orden) para este constructor -- eso si es error. Firmas distintas del mismo nombre
     * (sobrecarga real) se permiten y se acumulan en la lista.
     */
    public boolean declararConstructor(String nombre, List<Parametro> parametros) {
        List<Firma> firmas = constructores.computeIfAbsent(nombre, k -> new ArrayList<>());
        Firma nueva = new Firma(nombre, parametros, null);
        if (existeFirmaIdentica(firmas, nueva)) {
            return false;
        }
        firmas.add(nueva);
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.CONSTRUCTOR, null,
                parametros.size(), "clase", alcanceActual()));
        return true;
    }

    /** Resuelve cual sobrecarga de constructor aplica segun la cantidad y tipos de argumentos dados. */
    public Optional<Firma> buscarConstructor(String nombre, List<String> tiposArgumentos) {
        return buscarFirma(constructores.get(nombre), tiposArgumentos);
    }

    public List<Firma> getConstructores(String nombre) {
        return constructores.getOrDefault(nombre, List.of());
    }

    // METODOS (con sobrecarga)
    public boolean declararMetodo(String nombre, List<Parametro> parametros, String tipoRetorno) {
        List<Firma> firmas = metodos.computeIfAbsent(nombre, k -> new ArrayList<>());
        Firma nueva = new Firma(nombre, parametros, tipoRetorno);
        if (existeFirmaIdentica(firmas, nueva)) {
            return false;
        }
        firmas.add(nueva);
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.METODO, tipoRetorno,
                parametros.size(), "clase", alcanceActual()));
        return true;
    }

    public Optional<Firma> buscarMetodo(String nombre, List<String> tiposArgumentos) {
        return buscarFirma(metodos.get(nombre), tiposArgumentos);
    }

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

    /**
     * Compara por cantidad y tipo EXACTO de argumentos.
     * ( llamar con un 'int' cuando la firma declarada espera 'double')
     */
    private Optional<Firma> buscarFirma(List<Firma> firmas, List<String> tiposArgumentos) {
        if (firmas == null) return Optional.empty();
        for (Firma f : firmas) {
            if (f.tiposParametros().equals(tiposArgumentos)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    // UI / DEPURACION
    public List<EntradaSimbolo> getRegistroCompleto() {
        return Collections.unmodifiableList(registro);
    }
}