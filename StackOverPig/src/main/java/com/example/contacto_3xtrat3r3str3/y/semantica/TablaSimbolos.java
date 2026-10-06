package com.example.contacto_3xtrat3r3str3.y.semantica;

import java.util.*;

public class TablaSimbolos {

    public enum Categoria{ VARIABLE, ARREGLO, PARAMETRO, ESTRUCTURA_DEF, FUNCION}

    public record Parametro(String nombre, String tipo, boolean esArreglo, boolean esEstructura, String tipoEstructura) {}

    //un atributo de estructura, usado al declarar (para poder detectar duplicados antes de armar el mapa)
    public record AtributoEstructura(String nombre, String tipo) {}

    //tamanos: 1 elemento para arreglo 1D ([n]), 2 elementos para matriz ([n][m]); vacio si no es arreglo/matriz
    public record SimboloVariable(String nombre, String tipo, boolean esArreglo, int dimensiones,
                                  boolean esEstructura, String tipoEstructura, List<Integer> tamanos) {}

    public record DefinicionEstructura(String nombre, Map<String, String> atributos) {}
    public record DefinicionFuncion(String nombre, List<Parametro> parametros, String tipoRetorno) {
        public List<String> tipoParametros() {
            return parametros.stream().map(Parametro::tipo).toList();
        }
    }

    public record EntradaSimbolo(int id, String nombre, Categoria categoria, String tipo, int numParametros, List<Parametro> parametros, String ambito, int alcance) {}

    //scope interno
    private static class Scope {
        final String etiqueta;
        final Map<String, SimboloVariable> variables = new LinkedHashMap<>();
        final Map<String, DefinicionEstructura> estructuras = new LinkedHashMap<>();

        Scope(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }

    private final Deque<Scope> pila = new ArrayDeque<>();
    private final Map<String, DefinicionFuncion> funciones = new LinkedHashMap<>();

    private final List<EntradaSimbolo> registro = new ArrayList<>();
    private int siguienteId = 1;

    public TablaSimbolos() {
        pila.push(new Scope("global"));
    }

    //manejo de scopes
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

    //variables y arreglos

    //declara una variable primitiva simple en el scope actual / devuelve false si ya existe un simbolo con ese nombre en el scope
    public boolean declararVariable(String nombre, String tipo) {
        return declararVariable(nombre, tipo, false, 0, false, null, List.of());
    }

    //declara una variable completa (variable simple, arreglo 1D, matriz o instancia de estructura)
    public boolean declararVariable(String nombre, String tipo, boolean esArreglo, int dimensiones,
                                    boolean esEstructura, String tipoEstructura, List<Integer> tamanos) {
        Scope actual = pila.peek();
        if (actual.variables.containsKey(nombre)) {
            return false; //duplicada en el mismo scope
        }

        SimboloVariable simbolo = new SimboloVariable(
                nombre, tipo, esArreglo, dimensiones, esEstructura, tipoEstructura, tamanos);
        actual.variables.put(nombre, simbolo);

        Categoria categoria = esArreglo ? Categoria.ARREGLO : Categoria.VARIABLE;
        registro.add(new EntradaSimbolo(
                siguienteId++, nombre, categoria, tipo, 0, List.of(),
                ambitoActual(), alcanceActual()
        ));
        return true;
    }

    //buscar una variable desde el scope actual hacia arriba
    public Optional<SimboloVariable> buscarVariable(String nombre) {
        for (Scope s: pila) {
            SimboloVariable v = s.variables.get(nombre);
            if (v != null) return Optional.of(v);
        }
        return Optional.empty();
    }

    //estructuras

    //declara una estructura en el scope actual; recibe la lista de atributos (no un mapa ya armado)
    //para poder detectar atributos duplicados antes de perderlos en un Map
    public boolean declararEstructura(String nombre, List<AtributoEstructura> atributosLista) {
        Scope actual = pila.peek();
        if (actual.estructuras.containsKey(nombre)) {
            return false; //estructura duplicada
        }

        Map<String, String> atributos = new LinkedHashMap<>();
        for (AtributoEstructura a : atributosLista) {
            if (atributos.containsKey(a.nombre())) {
                return false; //atributo duplicado dentro de la misma estructura
            }
            atributos.put(a.nombre(), a.tipo());
        }

        actual.estructuras.put(nombre, new DefinicionEstructura(nombre, atributos));

        List<Parametro> atributosComoParametros = atributosLista.stream()
                .map(a -> new Parametro(a.nombre(), a.tipo(), false, false, null))
                .toList();

        registro.add(new EntradaSimbolo(
                siguienteId++, nombre, Categoria.ESTRUCTURA_DEF, "estructura",
                atributosComoParametros.size(), atributosComoParametros, ambitoActual(), alcanceActual()
        ));
        return true;
    }

    //buscar una estructura desde el scope actual hacia arriba (incluye estructuras declaradas dentro de funciones)
    public Optional<DefinicionEstructura> buscarEstructura(String nombre) {
        for (Scope s : pila) {
            DefinicionEstructura d = s.estructuras.get(nombre);
            if (d != null) return Optional.of(d);
        }
        return Optional.empty();
    }

    //funciones -> siempre son globales
    public boolean declararFuncion(String nombre, List<Parametro> parametros, String tipoRetorno) {
        if (funciones.containsKey(nombre)) {
            return false;
        }
        funciones.put(nombre, new DefinicionFuncion(nombre, parametros, tipoRetorno));

        registro.add(new EntradaSimbolo(siguienteId++, nombre, Categoria.FUNCION, tipoRetorno,
                parametros.size(), parametros, "global", alcanceActual()));
        return true;
    }

    public Optional<DefinicionFuncion> buscarFuncion(String nombre) {
        return Optional.ofNullable(funciones.get(nombre));
    }

    //elementos para la UI
    public Map<String, DefinicionFuncion> getFunciones() {
        return Collections.unmodifiableMap(funciones);
    }

    public List<EntradaSimbolo> getRegistroCompleto() {
        return Collections.unmodifiableList(registro);
    }
}
