package com.example.stack_over_pig.y.ast;

import com.example.stack_over_pig.c3d_v2.c.ContextoTraduccion;
import com.example.stack_over_pig.c3d_v2.c.TipoC;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;

import java.util.ArrayList;
import java.util.List;

// Nodos de sentencia del AST del lenguaje .y. Cada record sabe traducirse a cuartetas con aCodigoIntermedio()
public sealed interface NodoSentencia extends NodoAST permits
        NodoSentencia.DeclaracionVariable,
        NodoSentencia.DeclaracionArreglo,
        NodoSentencia.DeclaracionMatriz,
        NodoSentencia.DeclaracionEstructura,
        NodoSentencia.DeclaracionEstructuraLocal,
        NodoSentencia.Asignacion,
        NodoSentencia.IncrementoDecremento,
        NodoSentencia.Condicional,
        NodoSentencia.Elegir,
        NodoSentencia.CasoElegir,
        NodoSentencia.SiempreElegir,
        NodoSentencia.CicloPara,
        NodoSentencia.CicloMientras,
        NodoSentencia.CicloHacerMientras,
        NodoSentencia.Retorno,
        NodoSentencia.Imprimir,
        NodoSentencia.Leer,
        NodoSentencia.Romper,
        NodoSentencia.Continuar {
    TipoNodoSentencia tipoNodo();


    // DECLARACIONES

    // 'entero x = 5'  ->  emite una asignación si hay inicializador
    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               NodoExpr inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_VARIABLE;
        }
    }

    // 'entero a[5] = {1,2,3,4,5}'  ->  emite una asignación por elemento
    record DeclaracionArreglo(int linea, int columna, String tipo, String nombre,
                              int tamano, List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ARREGLO;
        }

    }

    // 'entero m[3][3]'  ->  sin cuarteta: la declaración la maneja el recogedor.
    record DeclaracionMatriz(int linea, int columna, String tipo, String nombre,
                             int filas, int columnas,
                             List<List<NodoExpr>> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_MATRIZ;
        }
    }

    // 'Punto p = {10, 20}'  ->  asigna cada campo en orden posicional.
    record DeclaracionEstructura(int linea, int columna, String tipoEstructura, String nombre,
                                 List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ESTRUCTURA;
        }
    }

    // Estructura declarada dentro de una función. No emite cuarteta
    record DeclaracionEstructuraLocal(int linea, int columna,
                                      NodoEstructura.Estructura estructura) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ESTRUCTURA_LOCAL;
        }
    }

    // ASIGNACION E INCREMENTO/DECREMENTO
    // 'x = 5'  ->  emite 'x = 5'
    record Asignacion(int linea, int columna, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ASIGNACION;
        }

    }

    // 'destino++' / 'destino--'  ->  se traduce como 'destino = destino + 1'
    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr destino) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.INCREMENTO_DECREMENTO;
        }

    }

    // CONDICIONAL
// Una rama 'sino (cond) entonces ...' dentro de un condicional.
    record RamaSino(int linea, int columna, NodoExpr condicion, List<NodoSentencia> cuerpo) { }

    // 'si ... sino ... sino ... contrario ...'  ->  etiquetas + saltos.
    record Condicional(int linea, int columna,
                       NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       List<RamaSino> ramasSino,
                       List<NodoSentencia> cuerpoContrario) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CONDICIONAL;
        }

    }

    // ELEGIR
    // 'elegir (x): caso 1: ... siempre: ...'  ->  etiquetas + saltos.
    record Elegir(int linea, int columna, NodoExpr expresion,
                  List<CasoElegir> casos, SiempreElegir siempre) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ELEGIR;
        }

    }

    // Caso individual dentro de 'elegir'. Lo gestiona Elegir.
    record CasoElegir(int linea, int columna, NodoExpr valor,
                      List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ELEGIR;
        }
    }

    // 'siempre' (default) dentro de 'elegir'. Lo gestiona Elegir
    record SiempreElegir(int linea, int columna,
                         List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ELEGIR;
        }

    }

    // CICLOS
    // 'para(entero i=0; i<10; i++): ...'  -> </10;>  init + etiquetas + salto atrás.
    record CicloPara(int linea, int columna,
                     String tipoInicializacion, String nombreVariable,
                     NodoExpr valorInicial, NodoExpr condicion,
                     String operadorActualizacion,
                     List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_PARA;
        }
    }

    // 'mientras(cond) hacer ...'  ->  etiqueta inicio + cond + cuerpo + salto atrás.
    record CicloMientras(int linea, int columna,
                         NodoExpr condicion,
                         List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_MIENTRAS;
        }

    }

    // 'hacer: ... mientras(cond)'  ->  cuerpo + etiqueta cond + salto si verdad.
    record CicloHacerMientras(int linea, int columna,
                              List<NodoSentencia> cuerpo,
                              NodoExpr condicion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_HACER_MIENTRAS;
        }
    }

    // RETORNO
    // 'retornar expr'  ->  emite 'return expr'.
    record Retorno(int linea, int columna, NodoExpr valor) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.RETORNO;
        }
    }

    // FUNCIONES ESPECIALES
    // 'imprimir(expr)'  ->  emite 'print expr'
    record Imprimir(int linea, int columna, NodoExpr expresion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.IMPRIMIR;
        }

    }

    // 'leer()'  ->  sin destino no emite nada
    record Leer(int linea, int columna) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.LEER;
        }
    }

    // CONTROL DE CICLOS
    // 'romper'  ->  goto a la etiqueta de fin del ciclo activo.
    record Romper(int linea, int columna) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ROMPER;
        }
    }

    // 'continuar'  ->  goto a la etiqueta de continuación del ciclo activo.
    record Continuar(int linea, int columna) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CONTINUAR;
        }

    }
}