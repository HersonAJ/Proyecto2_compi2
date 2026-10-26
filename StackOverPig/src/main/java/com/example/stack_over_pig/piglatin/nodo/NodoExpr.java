package com.example.stack_over_pig.piglatin.nodo;

import com.example.stack_over_pig.riscv.VisitanteGeneradorCuartetas;

import java.util.List;

public sealed interface NodoExpr extends NodoAST permits
        NodoExpr.LiteralEntero,
        NodoExpr.LiteralDecimal,
        NodoExpr.LiteralTexto,
        NodoExpr.LiteralCaracter,
        NodoExpr.LiteralBool,
        NodoExpr.ListaLiteral,
        NodoExpr.Identificador,
        NodoExpr.AccesoArray,
        NodoExpr.AccesoAtributo,
        NodoExpr.Binaria,
        NodoExpr.IncrementoDecremento,
        NodoExpr.Unaria,
        NodoExpr.LlamadaFuncion,
        NodoExpr.LlamadaMetodo,
        NodoExpr.InstanciaObjeto,
        NodoExpr.LiteralNulo {

    TipoNodoExpr tipoNodo();

    void aceptar(VisitanteGeneradorCuartetas v);

    // LITERALES

    record LiteralEntero(int linea, int columna, int valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_ENTERO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LiteralDecimal(int linea, int columna, double valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_DECIMAL; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LiteralTexto(int linea, int columna, String valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_TEXTO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LiteralCaracter(int linea, int columna, char valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CARACTER; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LiteralBool(int linea, int columna, boolean valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_BOOL; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LiteralNulo(int linea, int columna) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_NULO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { /* Fase 3 */ }
    }

    record ListaLiteral(int linea, int columna, List<NodoExpr> elementos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LISTA_LITERAL; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { /* Fase 2 */ }
    }

    // ACCESOS

    record Identificador(int linea, int columna, String nombre) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.IDENTIFICADOR; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record AccesoArray(int linea, int columna, NodoExpr arreglo, NodoExpr indice) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ARRAY; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record AccesoAtributo(int linea, int columna, NodoExpr objeto, String atributo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ATRIBUTO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { /* Fase 3 */ }
    }

    // OPERACIONES

    record Binaria(int linea, int columna, String operador,
                   NodoExpr izquierda, NodoExpr derecha) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.BINARIA; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record Unaria(int linea, int columna, String operador, NodoExpr operando) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.UNARIA; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INCREMENTO_DECREMENTO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { /* no en Fase 1 */ }
    }

    // LLAMADAS Y OBJETOS

    record LlamadaFuncion(int linea, int columna, String nombre,
                          List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_FUNCION; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LlamadaMetodo(int linea, int columna, NodoExpr objeto,
                         String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_METODO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { /* Fase 3 */ }
    }

    record InstanciaObjeto(int linea, int columna, String tipoClase,
                           List<NodoExpr> argumentos, String tipoDeclarado) implements NodoExpr {
        public InstanciaObjeto(int linea, int columna, String tipoClase, List<NodoExpr> argumentos) {
            this(linea, columna, tipoClase, argumentos, null);
        }
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INSTANCIA_OBJETO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { /* Fase 3 */ }
    }
}