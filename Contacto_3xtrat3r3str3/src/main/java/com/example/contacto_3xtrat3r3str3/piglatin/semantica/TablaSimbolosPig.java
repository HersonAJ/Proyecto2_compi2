package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import java.util.*;

public class TablaSimbolosPig {

    //cateogiras y records

    public enum Categoria { VARIABLE, ARREGLO, ESTRUCTURA_VAR, OBJETO_VAR, ESTRUCTURA_DEF, FUNCION, CLASE}

    //Parametro de funcion o metodo
    public record Parametro(String nombre, String tipo, int dimensiones) {}

    //una variable local o global
    public record SimboloVariable(String nombre, String tipo, int dimensiones, Integer tamanoConocido,
                                  boolean esEstructura, boolean esObjeto, String tipoOriginal) {}

    //definicion de una estructura importada .y
    public record DefinicionEstructura(String nombre, Map<String, String> atributos) {}

    //definicion e una funcion importada .y
    public record DefinicionFuncion(String nombre, List<Parametro> parametros, String tipoRetorno) {}

    //definicion de un atributo de clase importada de .z
    public record AtributoClase(String nombre, String tipo, int dimensiones) {}

    //definicion de firma (constructor o metodo) importada de .z
    public record Firma(String nombre, List<Parametro> parametros, String tipoRetorno) {}

    //definicion de una clase importada .z
    public record DefinicionClase(String nombre, Map<String, AtributoClase> atributos,
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

    private final List<EntradaSimbolo> registro = new ArrayList<>();
    private int siguienteId = 1;

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
    public boolean declararVariable(String nombre, String tipo, int dimensiones,
                                    Integer tamano, boolean esEstructura,
                                    boolean esObjeto, String tipoOriginal) {
        Scope actual = pila.peek();
        if (actual.variables.containsKey(nombre)) {
            return false;
        }

        SimboloVariable v = new SimboloVariable(nombre, tipo, dimensiones, tamano, esEstructura, esObjeto, tipoOriginal);
        actual.variables.put(nombre, v);

        Categoria cat = esObjeto ? Categoria.OBJETO_VAR :
                esEstructura ? Categoria.ESTRUCTURA_VAR :
                        (dimensiones > 0 ? Categoria.ARREGLO : Categoria.VARIABLE);

        registro.add(new EntradaSimbolo(siguienteId++, nombre, cat, tipo,0, ambitoActual(), alcanceActual()));
        return true;
    }

    public Optional<SimboloVariable> buscarVariable(String nombre) {
        for (Scope scope : pila) {
            SimboloVariable variable = scope.variables.get(nombre);
            if (variable != null) return Optional.of(variable);
        }
        return Optional.empty();
    }

    //estructuras importadas de .y
    public void declararEstructura(String nombre, Map<String, String> atributos) {
        estructuras.put(nombre, new DefinicionEstructura(nombre, atributos));
        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.ESTRUCTURA_DEF, "estructura", atributos.size(), "importado", 1));
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
}