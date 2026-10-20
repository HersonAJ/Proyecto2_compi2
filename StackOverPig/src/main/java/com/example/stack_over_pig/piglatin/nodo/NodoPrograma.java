package com.example.stack_over_pig.piglatin.nodo;

import com.example.stack_over_pig.riscv.VisitanteGeneradorCuartetas;

import java.util.List;

public record NodoPrograma(int linea, int columna,
                           List<NodoImportacion> importaciones,
                           List<NodoSentencia> variablesGlobales,
                           List<NodoSentencia> cuerpoMain) implements NodoAST {

    public void aceptar(VisitanteGeneradorCuartetas v) {
        for (NodoSentencia s : variablesGlobales) s.aceptar(v);
        for (NodoSentencia s : cuerpoMain) s.aceptar(v);
    }
}