package com.example.stack_over_pig.piglatin.semantica;

import com.example.stack_over_pig.comun.JerarquiaClases;

import java.util.*;

public class TablaSimbolosPig {

    //cateogiras y records

    public enum Categoria { VARIABLE, ARREGLO, ESTRUCTURA_VAR, OBJETO_VAR, ESTRUCTURA_DEF, FUNCION, CLASE}

    //Parametro de funcion o metodo
    public record Parametro(String nombre, String tipo, int dimensiones) {}

    //una variable local o global
    public record SimboloVariable(String nombre, String tipo, int dimensiones,
                                  Integer tamanoConocido, boolean esEstructura,
                                  boolean esObjeto, String tipoOriginal, int offset) {}

    //definicion de una estructura importada .y (dimensiones: 0 = escalar, 1 = arreglo)
    public record DefinicionEstructura(String nombre, Map<String, String> atributos,
                                       Map<String, Integer> dimensiones) {
        // Constructor anterior, para no romper el código que ya lo usa.
        public DefinicionEstructura(String nombre, Map<String, String> atributos) {
            this(nombre, atributos, Map.of());
        }

        public int dimensionesDe(String atributo) {
            return dimensiones.getOrDefault(atributo, 0);
        }
    }

    //definicion e una funcion importada .y
    public record DefinicionFuncion(String nombre, List<Parametro> parametros, String tipoRetorno) {}

    //definicion de un atributo de clase importada de .z
    public record AtributoClase(String nombre, String tipo, int dimensiones) {}

    //definicion de firma (constructor o metodo) importada de .z
    public record Firma(String nombre, List<Parametro> parametros, String tipoRetorno) {}

    //definicion de una clase importada .z
    //'superclase' es null si la clase no hereda de ninguna (o si el .z no declaro 'extends')
    public record DefinicionClase(String nombre, String superclase,
                                  Map<String, AtributoClase> atributos,
                                  List<Firma> constructores, List<Firma> metodos) {}

    //entrad del registro para la UI
    public record EntradaSimbolo(int id, String nombre, Categoria categoria, String tipo, int numParametros, String ambito, int alcance) {}

    //
    //Scope interno
    private static class Scope {
        final String etiqueta;
        final Map<String, SimboloVariable> variables = new LinkedHashMap<>();

        Scope(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    private final Deque<Scope> pila = new ArrayDeque<>();

    //simbolos importados (globales, no dependen de scope)
    private final Map<String, DefinicionEstructura> estructuras = new LinkedHashMap<>();
    private final Map<String, DefinicionFuncion> funciones = new LinkedHashMap<>();
    private final Map<String, DefinicionClase> clases = new LinkedHashMap<>();

    //jerarquia de clases para responder esSubtipo / ciclo
    private final JerarquiaClases jerarquia = new JerarquiaClases(this::superclaseDe);

    private final List<EntradaSimbolo> registro = new ArrayList<>();
    private int siguienteId = 1;
    private int siguienteOffset =16;
    private final Map<String, Integer> offsetsTemporales = new LinkedHashMap<>();

    public TablaSimbolosPig() {
        pila.push(new Scope("global"));
    }

    //scope
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

    // variable

    public Optional<SimboloVariable> buscarVariable(String nombre) {
        for (Scope scope : pila) {
            SimboloVariable variable = scope.variables.get(nombre);
            if (variable != null) return Optional.of(variable);
        }
        return Optional.empty();
    }

    //estructuras importadas de .y
    public void declararEstructura(String nombre, Map<String, String> atributos,
                                   Map<String, Integer> dimensiones) {
        estructuras.put(nombre, new DefinicionEstructura(nombre, atributos, dimensiones));
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.ESTRUCTURA_DEF,
                "estructura", atributos.size(), "importado", 1));
    }

    public void declararEstructura(String nombre, Map<String, String> atributos) {
        declararEstructura(nombre, atributos, Map.of());
    }

    public Optional<DefinicionEstructura> buscarEstructura(String nombre) {
        return Optional.ofNullable(estructuras.get(nombre));
    }

    //Funciones importadas de .y
    public void declararFuncion(String nombre, List<Parametro> parametros, String tipoRetorno) {
        funciones.put(nombre, new DefinicionFuncion(nombre, parametros, tipoRetorno));
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.FUNCION, tipoRetorno, parametros.size(), "importado", 1));
    }

    public Optional<DefinicionFuncion> buscarFuncion(String nombre) {
        return Optional.ofNullable(funciones.get(nombre));
    }

    //Clases importadas de .z
    public void declararClase(DefinicionClase clase) {
        clases.put(clase.nombre, clase);
        registro.add(new EntradaSimbolo(siguienteId++, clase.nombre(), Categoria.CLASE,
                "clase", clase.metodos().size(), "importado", 1));
    }

    public Optional<DefinicionClase> buscarClase(String nombre) {
        return Optional.ofNullable(clases.get(nombre));
    }

    // HERENCIA

    /** Superclase de una clase importada; null si no hereda de nadie o no se conoce. */
    private String superclaseDe(String clase) {
        DefinicionClase def = clases.get(clase);
        return def != null ? def.superclase() : null;
    }

    /** true si 'sub' es 'sup' o hereda de ella (directa o indirectamente). */
    public boolean esSubtipo(String sub, String sup) {
        return jerarquia.esSubtipo(sub, sup);
    }

    //Consultas

    //devuelve tru si el nombre corresponde a algun simbolo importado
    public boolean esSimboloImportado(String nombre) {
        return estructuras.containsKey(nombre) || funciones.containsKey(nombre) || clases.containsKey(nombre);
    }

    public Map<String, DefinicionEstructura> getEstructuras() {
        return Collections.unmodifiableMap(estructuras);
    }

    public Map<String, DefinicionFuncion> getFunciones() {
        return Collections.unmodifiableMap(funciones);
    }

    public Map<String, DefinicionClase> getClases() {
        return Collections.unmodifiableMap(clases);
    }

    public List<EntradaSimbolo> getRegistroCompleto() {
        return Collections.unmodifiableList(registro);
    }

    // Devuelve true si el nombre ya está en uso por una estructura o una clase
    public boolean nombreEnUso(String nombre) {
        return estructuras.containsKey(nombre) || clases.containsKey(nombre);
    }

    // OFFSETS DE MEMORIA

    /** Asigna un offset (fp-relativo) a una variable escalar y lo devuelve. */
    public int asignarOffsetVariable(String nombre) {
        int offset = -siguienteOffset;
        siguienteOffset += 8; // RV64: 8 bytes por slot
        return offset;
    }

    /** Asigna un offset (fp-relativo) a un arreglo de 'tamano' elementos. */
    public int asignarOffsetVariableConTamano(String nombre, int tamano) {
        int offset = -(siguienteOffset + 8 * (tamano - 1));
        siguienteOffset += 8 * tamano;
        return offset;
    }

    /** Asigna un offset (fp-relativo) a un struct de 'numCampos' campos (8 bytes cada uno). */
    public int asignarOffsetStruct(String nombre, int numCampos) {
        int n = Math.max(numCampos, 1);
        int offset = -(siguienteOffset + 8 * (n - 1));  // campo 0 en la dirección más baja
        siguienteOffset += 8 * n;
        return offset;
    }

    /** Asigna un offset (fp-relativo) a un temporal. */
    public int asignarOffsetTemporal(int idTemporal) {
        String key = "t" + idTemporal;
        Integer existente = offsetsTemporales.get(key);
        if (existente != null) return existente;
        int offset = -siguienteOffset;
        siguienteOffset += 8;
        offsetsTemporales.put(key, offset);
        return offset;
    }

    /** Tamaño total del marco (antes de redondear a múltiplo de 16). */
    public int getSiguienteOffset() {
        return siguienteOffset;
    }

    public boolean declararVariable(String nombre, String tipo, int dimensiones,
                                    Integer tamano, boolean esEstructura,
                                    boolean esObjeto, String tipoOriginal) {
        return declararVariable(nombre, tipo, dimensiones, tamano, esEstructura,
                esObjeto, tipoOriginal, 0);
    }

    public boolean declararVariable(String nombre, String tipo, int dimensiones,
                                    Integer tamano, boolean esEstructura,
                                    boolean esObjeto, String tipoOriginal, int offset) {
        Scope actual = pila.peek();
        if (actual.variables.containsKey(nombre)) {
            return false;
        }

        SimboloVariable v = new SimboloVariable(nombre, tipo, dimensiones, tamano,
                esEstructura, esObjeto, tipoOriginal, offset);
        actual.variables.put(nombre, v);

        Categoria cat = esObjeto ? Categoria.OBJETO_VAR :
                esEstructura ? Categoria.ESTRUCTURA_VAR :
                        (dimensiones > 0 ? Categoria.ARREGLO : Categoria.VARIABLE);

        registro.add(new EntradaSimbolo(siguienteId++, nombre, cat, tipo, 0,
                ambitoActual(), alcanceActual()));
        return true;
    }

    public int getOffsetTemporal(int idTemporal) {
        Integer offset = offsetsTemporales.get("t" + idTemporal);
        return offset != null ? offset : 0;
    }

    public void actualizarOffsetVariable(String nombre, int offset) {
        for (Scope scope : pila) {
            SimboloVariable v = scope.variables.get(nombre);
            if (v != null) {
                scope.variables.put(nombre, new SimboloVariable(v.nombre(), v.tipo(), v.dimensiones(),
                        v.tamanoConocido(), v.esEstructura(), v.esObjeto(), v.tipoOriginal(), offset));
                return;
            }
        }
    }
}