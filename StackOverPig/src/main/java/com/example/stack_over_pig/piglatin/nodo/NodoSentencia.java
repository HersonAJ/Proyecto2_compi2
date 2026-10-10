package com.example.stack_over_pig.piglatin.nodo;

import com.example.stack_over_pig.c3d_v2.c.pig.ContextoTraduccionPig;
import com.example.stack_over_pig.c3d_v2.c.pig.TipoPigC;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.ImprimirPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.LeerPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.LiteralPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionAtributo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;

import java.util.ArrayList;
import java.util.List;

public sealed interface NodoSentencia extends NodoAST permits
        NodoSentencia.DeclaracionVariable,
        NodoSentencia.DeclaracionArreglo,
        NodoSentencia.DeclaracionStruct,
        NodoSentencia.Asignacion,
        NodoSentencia.IncrementoDecremento,
        NodoSentencia.Condicional,
        NodoSentencia.CicloDum,
        NodoSentencia.CicloFacere,
        NodoSentencia.CicloPer,
        NodoSentencia.Lectura,
        NodoSentencia.Escritura,
        NodoSentencia.InterrupcionCiclo,
        NodoSentencia.LlamadaFuncionSentencia,
        NodoSentencia.LlamadaMetodoSentencia {

    TipoNodoSentencia tipoNodo();


    // DECLARACIONES

    /**
     * 'esto x : tipo valor'  ->  emite una asignación si hay inicializador.
     */
    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               NodoExpr inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_VARIABLE;
        }

    }

    /**
     * 'series a[N] : tipo { ... }'  ->  la declaración la maneja el recogedor.
     */
    record DeclaracionArreglo(int linea, int columna, String tipo, String nombre,
                              int tamano, List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ARREGLO;
        }

    }

    /**
     * 'esto x : Tipo { ... }'  ->  asigna cada campo en orden posicional.
     */
    record DeclaracionStruct(int linea, int columna, String tipo, String nombre,
                             List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_STRUCT;
        }

    }

    // ASIGNACIONES

    /**
     * 'x = 5'  ->  emite 'x = 5'.
     */
    record Asignacion(int linea, int columna, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ASIGNACION;
        }

    }

    /**
     * 'x++' / 'x--'  ->  emite 'x = x ± 1'.
     */
    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.INCREMENTO_DECREMENTO;
        }
    }

    // CONDICIONAL

    /**
     * 'si ... aliter ... aliter ...'  ->  etiquetas + saltos.
     */
    record Condicional(int linea, int columna, NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       List<RamaAliter> ramasAliter,
                       List<NodoSentencia> cuerpoAliter) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CONDICIONAL;
        }

    }

    /**
     * Rama 'aliter (cond)' con su cuerpo. La gestiona Condicional.
     */
    record RamaAliter(int linea, int columna, NodoExpr condicion, List<NodoSentencia> cuerpo) {
    }

    // CICLOS

    /**
     * 'dum (cond) { ... } finis'  ->  etiqueta inicio + cond + cuerpo + salto atrás.
     */
    record CicloDum(int linea, int columna, NodoExpr condicion,
                    List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_DUM;
        }
    }

    /**
     * 'facere { ... } dum (cond);'  ->  cuerpo + etiqueta cond + salto si verdad.
     */
    record CicloFacere(int linea, int columna, List<NodoSentencia> cuerpo,
                       NodoExpr condicion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_FACERE;
        }

    }

    /**
     * 'per (init; cond; act) { ... }'  ->  init + etiquetas + salto atrás.
     */
    record CicloPer(int linea, int columna, NodoSentencia inicializacion,
                    NodoExpr condicion, NodoSentencia actualizacion,
                    List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_PER;
        }

    }

    // LECTURA / ESCRITURA

    /**
     * 'var <<'  o  '<<'  ->  LeerPig (con destino o a temporal descartable).
     */
    record Lectura(int linea, int columna, String variable) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.LECTURA;
        }

    }

    /**
     * '>> a >> b >> c;'  ->  un ImprimirPig por cada valor.
     */
    record Escritura(int linea, int columna, List<NodoExpr> valores) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ESCRITURA;
        }


    }

    // INTERRUPCION DE CICLO

    /**
     * 'perge' o 'interrumpe'  ->  goto a la etiqueta del ciclo activo.
     */
    record InterrupcionCiclo(int linea, int columna, String tipo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.INTERRUPCION_CICLO;
        }

    }

    // LLAMADAS COMO SENTENCIA

    /**
     * Llamada a función suelta  ->  evalúa y descarta.
     */
    record LlamadaFuncionSentencia(int linea, int columna, NodoExpr.LlamadaFuncion llamada) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.LLAMADA_FUNCION_SENTENCIA;
        }

    }

    /**
     * Llamada a metodo suelta  ->  evalúa y descarta.
     */
    record LlamadaMetodoSentencia(int linea, int columna, NodoExpr.LlamadaMetodo llamada) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.LLAMADA_METODO_SENTENCIA;
        }

    }
}