package com.example.stack_over_pig.zetariano.nodo;

import com.example.stack_over_pig.c3d_v2.c.z.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.ImprimirZ;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.LiteralZ;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.RomperSwitch;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.Switch1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;

import java.util.ArrayList;
import java.util.List;

//Nodos de sentencia del AST del lenguaje .z Cada record sabe traducirse a cuartetas con aCodigoIntermedio()
public sealed interface NodoSentencia extends NodoAST permits
        NodoSentencia.DeclaracionVariable,
        NodoSentencia.Asignacion,
        NodoSentencia.ExpresionComoSentencia,
        NodoSentencia.Condicional,
        NodoSentencia.Switch,
        NodoSentencia.CasoSwitch,
        NodoSentencia.CasoDefault,
        NodoSentencia.CicloPara,
        NodoSentencia.CicloMientras,
        NodoSentencia.CicloHacerMientras,
        NodoSentencia.Retorno,
        NodoSentencia.Imprimir,
        NodoSentencia.Leer,
        NodoSentencia.Romper,
        NodoSentencia.Continuar {

    TipoNodoSentencia tipoNodo();

    // DECLARACION
    /** 'int x = 25;'  ->  emite una asignación si hay inicializador. */
    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               int dimensiones, NodoExpr inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_VARIABLE; }


    }

    // ASIGNACION
    /** 'x = 5;', 'x += 3;', 'x -= 2;', 'x *= 2;'  ->  emite la cuarteta correspondiente. */
    record Asignacion(int linea, int columna, String operador, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ASIGNACION; }
    }

    // EXPRESION COMO SENTENCIA
    /** Expresión suelta: 'p1.saludar();', 'a++;', 'calcular(x);'  ->  evalúa y descarta. */
    record ExpresionComoSentencia(int linea, int columna, NodoExpr expresion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.EXPRESION_COMO_SENTENCIA;
        }
    }

    // CONDICIONAL
    /** 'if (cond) {...} else {...}'  ->  etiquetas + saltos. */
    record Condicional(int linea, int columna, NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       List<NodoSentencia> cuerpoSino) implements NodoSentencia { // null si no hay 'else'
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CONDICIONAL; }
    }

    // SWITCH
    /** 'switch (expr) { case ... default ... }'  ->  cuarteta Switch1. */
    record Switch(int linea, int columna, NodoExpr expresion,
                  List<CasoSwitch> casos, CasoDefault casoDefault) implements NodoSentencia { // casoDefault puede ser null
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.SWITCH; }
    }

    /** Caso individual dentro de 'switch'. Lo gestiona Switch. */
    record CasoSwitch(int linea, int columna, NodoExpr valor, List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CASO_SWITCH; }
    }

    /** Caso 'default' dentro de 'switch'. Lo gestiona Switch. */
    record CasoDefault(int linea, int columna, List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CASO_DEFAULT; }
    }

    // CICLOS
    /** 'for (init; cond; act) { ... }'  ->  init + etiquetas + salto atrás. */
    record CicloPara(int linea, int columna,
                     NodoSentencia inicializacion,
                     NodoExpr condicion,
                     NodoSentencia actualizacion,
                     List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_PARA; }
    }

    /** 'while (cond) { ... }'  ->  etiqueta inicio + cond + cuerpo + salto atrás. */
    record CicloMientras(int linea, int columna, NodoExpr condicion, List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_MIENTRAS; }
    }

    /** 'do { ... } while (cond);'  ->  cuerpo + etiqueta cond + salto si verdad. */
    record CicloHacerMientras(int linea, int columna, List<NodoSentencia> cuerpo, NodoExpr condicion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_HACER_MIENTRAS; }
    }

    // RETORNO
    /** 'return valor;' o 'return;'  ->  emite 'return ...'. */
    record Retorno(int linea, int columna, NodoExpr valor) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.RETORNO; }
    }

    // IMPRESION / LECTURA
    /** 'println(expr)' / 'print(expr)'  ->  ImprimirZ (con o sin \n). */
    record Imprimir(int linea, int columna, boolean saltoDeLinea, NodoExpr expresion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.IMPRIMIR; }
    }

    /** 'readln();'  ->  sin destino no emite nada. */
    record Leer(int linea, int columna) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LEER; }
    }

    /** 'break;'  ->  break de switch o goto de fin de ciclo. */
    record Romper(int linea, int columna) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ROMPER; }
    }

    /** 'continue;'  ->  goto de continuación del ciclo activo. */
    record Continuar(int linea, int columna) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CONTINUAR; }
    }
}