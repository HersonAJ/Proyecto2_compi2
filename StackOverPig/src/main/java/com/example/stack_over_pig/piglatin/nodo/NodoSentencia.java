package com.example.stack_over_pig.piglatin.nodo;


import com.example.stack_over_pig.riscv.VisitanteGeneradorCuartetas;

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

    void aceptar(VisitanteGeneradorCuartetas v);

    // DECLARACIONES

    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               NodoExpr inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_VARIABLE; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record DeclaracionArreglo(int linea, int columna, String tipo, String nombre,
                              int tamano, List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_ARREGLO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record DeclaracionStruct(int linea, int columna, String tipo, String nombre,
                             List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_STRUCT; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    // ASIGNACIONES

    record Asignacion(int linea, int columna, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ASIGNACION; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.INCREMENTO_DECREMENTO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    // CONDICIONAL

    record Condicional(int linea, int columna, NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       List<RamaAliter> ramasAliter,
                       List<NodoSentencia> cuerpoAliter) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CONDICIONAL; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record RamaAliter(int linea, int columna, NodoExpr condicion, List<NodoSentencia> cuerpo) {
    }

    // CICLOS

    record CicloDum(int linea, int columna, NodoExpr condicion,
                    List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_DUM; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record CicloFacere(int linea, int columna, List<NodoSentencia> cuerpo,
                       NodoExpr condicion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_FACERE; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record CicloPer(int linea, int columna, NodoSentencia inicializacion,
                    NodoExpr condicion, NodoSentencia actualizacion,
                    List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_PER; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    // LECTURA / ESCRITURA

    record Lectura(int linea, int columna, String variable) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LECTURA; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record Escritura(int linea, int columna, List<NodoExpr> valores) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ESCRITURA; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    // INTERRUPCION DE CICLO

    record InterrupcionCiclo(int linea, int columna, String tipo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.INTERRUPCION_CICLO; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    // LLAMADAS COMO SENTENCIA

    record LlamadaFuncionSentencia(int linea, int columna, NodoExpr.LlamadaFuncion llamada) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LLAMADA_FUNCION_SENTENCIA; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }

    record LlamadaMetodoSentencia(int linea, int columna, NodoExpr.LlamadaMetodo llamada) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LLAMADA_METODO_SENTENCIA; }
        @Override public void aceptar(VisitanteGeneradorCuartetas v) { v.visitar(this); }
    }
}