package com.example.stack_over_pig.y.ast;

import com.example.stack_over_pig.c3d_v2.c.ContextoTraduccion;
import com.example.stack_over_pig.c3d_v2.c.TipoC;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.PromocionTipos;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.OperacionBinaria;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.ConversionTipo;

import java.util.List;

//Nodos de expresión del AST de .y. Cada expresión devuelve el AccesoMemoria que representa su resultado.
public sealed interface NodoExpr extends NodoAST permits
        NodoExpr.LiteralEntero,
        NodoExpr.LiteralFlotante,
        NodoExpr.LiteralCadena,
        NodoExpr.LiteralCaracter,
        NodoExpr.LiteralBool,
        NodoExpr.Identificador,
        NodoExpr.AccesoArray,
        NodoExpr.AccesoAtributo,
        NodoExpr.Binaria,
        NodoExpr.Unaria,
        NodoExpr.LlamadaFuncion,
        NodoExpr.Leer {

    TipoNodoExpr tipoNodo();

    // LITERALES
    // '10'  ->  operando literal entero
    record LiteralEntero(int linea, int columna, int valor) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_ENTERO; }

    }

    // '3.14'  ->  operando literal flotante
    record LiteralFlotante(int linea, int columna, double valor) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_FLOTANTE; }

    }

    // '"hola"'  ->  operando literal cadena
    record LiteralCadena(int linea, int columna, String valor) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CADENA; }

    }

    // 'a'  ->  operando literal caracter
    record LiteralCaracter(int linea, int columna, char valor) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CARACTER; }
    }

    // 'verdadero' / 'falso'  ->  operando literal bool (1 o 0)
    record LiteralBool(int linea, int columna, boolean valor) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_BOOL; }

    }

    // ACCESOS
    // 'x'  ->  acceso a variable usando el tipo de la tabla
    record Identificador(int linea, int columna, String nombre) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.IDENTIFICADOR; }

    }

    // 'a[i]'  ->  operando AccesoArreglo.
    record AccesoArray(int linea, int columna, NodoExpr arreglo, NodoExpr indice) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ARRAY; }
    }

    // 'p.campo'  ->  operando AccesoAtributo (con '.').
    record AccesoAtributo(int linea, int columna, NodoExpr objeto, String atributo) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ATRIBUTO; }
    }

    // OPERACIONES
    // 'a + b', 'a == b', ...  ->  promociones + OperacionBinaria
    record Binaria(int linea, int columna, String operador, NodoExpr izquierda, NodoExpr derecha) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.BINARIA; }
    }

    // '-x', '!x', '++x', '--x'  ->  OperacionUnaria o x = x ± 1.
    record Unaria(int linea, int columna, String operador, NodoExpr operando, boolean prefijo) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.UNARIA; }

    }

    // LLAMADAS A FUNCION
    // 'suma(a, b)'  ->  Llamada con temporal de retorno
    record LlamadaFuncion(int linea, int columna, String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_FUNCION; }
    }

    // LEER COMO EXPRESION
    // 'leer()'  ->  temporal de tipo cadena + cuarteta Leer
    record Leer(int linea, int columna) implements NodoExpr {
        @Override
        public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LEER; }
    }
}