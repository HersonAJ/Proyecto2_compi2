package com.example.stack_over_pig.piglatin.nodo;

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
        NodoExpr.InstanciaObjeto {

    TipoNodoExpr tipoNodo();

    // LITERALES

    /** Entero (numerus). */
    record LiteralEntero(int linea, int columna, int valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_ENTERO; }
    }

    /** Decimal (decimalis). */
    record LiteralDecimal(int linea, int columna, double valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_DECIMAL; }
    }

    /** Texto (textum). */
    record LiteralTexto(int linea, int columna, String valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_TEXTO; }
    }

    /** Caracter (littera). */
    record LiteralCaracter(int linea, int columna, char valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CARACTER; }
    }

    /** 'verum' / 'falsus' (bool). */
    record LiteralBool(int linea, int columna, boolean valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_BOOL; }
    }

    /** '{1, 2, 3}': solo válido como inicializador de arreglo o struct. */
    record ListaLiteral(int linea, int columna, List<NodoExpr> elementos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LISTA_LITERAL; }
    }

    // ACCESOS

    /** 'x' */
    record Identificador(int linea, int columna, String nombre) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.IDENTIFICADOR; }
    }

    /** 'a[i]' */
    record AccesoArray(int linea, int columna, NodoExpr arreglo, NodoExpr indice) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ARRAY; }
    }

    /** 'obj.campo' */
    record AccesoAtributo(int linea, int columna, NodoExpr objeto, String atributo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ATRIBUTO; }
    }

    // OPERACIONES

    /** 'a + b', 'a == b', ... */
    record Binaria(int linea, int columna, String operador,
                   NodoExpr izquierda, NodoExpr derecha) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.BINARIA; }
    }

    /** '-x', '!x' */
    record Unaria(int linea, int columna, String operador, NodoExpr operando) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.UNARIA; }
    }

    /** 'x++', 'x--', '++x', '--x' */
    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INCREMENTO_DECREMENTO; }
    }

    // LLAMADAS Y OBJETOS

    /** 'calcular(x)': llamada a función de .y */
    record LlamadaFuncion(int linea, int columna, String nombre,
                          List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_FUNCION; }
    }

    /** 'p.metodo(args)' */
    record LlamadaMetodo(int linea, int columna, NodoExpr objeto,
                         String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_METODO; }
    }

    /** 'novus Clase(args)' */
    record InstanciaObjeto(int linea, int columna, String tipoClase,
                           List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INSTANCIA_OBJETO; }
    }
}