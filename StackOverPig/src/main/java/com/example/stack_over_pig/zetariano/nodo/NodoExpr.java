package com.example.stack_over_pig.zetariano.nodo;

import com.example.stack_over_pig.c3d_v2.c.z.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import java.util.ArrayList;
import java.util.List;

public sealed interface NodoExpr extends NodoAST permits
        NodoExpr.LiteralEntero,
        NodoExpr.LiteralDecimal,
        NodoExpr.LiteralCadena,
        NodoExpr.LiteralCaracter,
        NodoExpr.LiteralBool,
        NodoExpr.LiteralNulo,
        NodoExpr.ListaLiteral,
        NodoExpr.Identificador,
        NodoExpr.AccesoArray,
        NodoExpr.AccesoAtributo,
        NodoExpr.Binaria,
        NodoExpr.Unaria,
        NodoExpr.IncrementoDecremento,
        NodoExpr.Ternaria,
        NodoExpr.LlamadaFuncion,
        NodoExpr.LlamadaMetodo,
        NodoExpr.InstanciaObjeto,
        NodoExpr.ArregloNuevo,
        NodoExpr.ObjetoActual{

    TipoNodoExpr tipoNodo();


    // LITERALES
    /** '10'  ->  operando literal entero. */
    record LiteralEntero(int linea, int columna, int valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_ENTERO; }
    }

    /** '3.14'  ->  operando literal double. */
    record LiteralDecimal(int linea, int columna, double valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_DECIMAL; }
    }

    /** '"hola"'  ->  operando literal String. */
    record LiteralCadena(int linea, int columna, String valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CADENA; }
    }

    /** 'a'  ->  operando literal char. */
    record LiteralCaracter(int linea, int columna, char valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CARACTER; }
    }

    /** 'true' / 'false'  ->  operando literal boolean. */
    record LiteralBool(int linea, int columna, boolean valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_BOOL; }
    }

    /** 'null'  ->  operando LiteralNuloZ. */
    record LiteralNulo(int linea, int columna) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_NULO; }
    }

    /** '{10, 20, 30}'  ->  solo válido como inicializador de declaración. */
    record ListaLiteral(int linea, int columna, List<NodoExpr> elementos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LISTA_LITERAL; }
    }

    // ACCESOS
    /** 'x'  ->  acceso a atributo  */
    record Identificador(int linea, int columna, String nombre) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.IDENTIFICADOR; }
    }

    /** 'a[i]'  ->  operando AccesoArreglo (con su tipo de elemento inferido). */
    record AccesoArray(int linea, int columna, NodoExpr arreglo, NodoExpr indice) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ARRAY; }
    }

    /** 'p1.edad'  ->  operando AccesoAtributo1 (siempre con '->' en .z). */
    record AccesoAtributo(int linea, int columna, NodoExpr objeto, String atributo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ATRIBUTO; }
    }

    // OPERACIONES
    /** 'a + b', 'a == b', ...  ->  promociones / strcmp / concat + OperacionBinaria. */
    record Binaria(int linea, int columna, String operador, NodoExpr izquierda, NodoExpr derecha) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.BINARIA; }
    }
    /** '-x', '!x'  ->  OperacionUnaria con temporal. */
    record Unaria(int linea, int columna, String operador, NodoExpr operando) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.UNARIA; }
    }

    /** '++x', '--x', 'x++', 'x--'  ->  x = x ± 1 (como OperacionBinaria). */
    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INCREMENTO_DECREMENTO; }
    }

    /** 'cond ? a : b'  ->  if/else con temporal que recibe a o b. */
    record Ternaria(int linea, int columna, NodoExpr condicion,
                    NodoExpr siVerdadero, NodoExpr siFalso) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.TERNARIA; }
    }

    // LLAMADAS Y CREACION DE OBJETOS
    /** 'calcular(x)'  ->  Llamada con 'this' como primer argumento. */
    record LlamadaFuncion(int linea, int columna, String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_FUNCION; }
    }

    /** 'p1.saludar()'  ->  LlamadaMetodo1 con el receptor como primer argumento. */
    record LlamadaMetodo(int linea, int columna, NodoExpr objeto,
                         String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_METODO; }
    }

    /** 'new Persona(...)'  ->  NewObjeto (malloc + constructor). */
    record InstanciaObjeto(int linea, int columna, String tipoClase, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INSTANCIA_OBJETO; }
    }

    /** 'new int[N]'  ->  NewArreglo (malloc).  'new int[A][B]'  ->  NewArregloMulti. */
    record ArregloNuevo(int linea, int columna, String tipoBase, List<NodoExpr> dimensiones) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ARREGLO_NUEVO; }
    }
    /** 'this' */
    record ObjetoActual(int linea, int columna) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.OBJETO_ACTUAL; }
    }
}