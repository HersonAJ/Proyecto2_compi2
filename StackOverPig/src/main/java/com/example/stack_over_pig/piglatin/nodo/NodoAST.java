package com.example.stack_over_pig.piglatin.nodo;

public sealed interface NodoAST permits NodoExpr, NodoSentencia, NodoPrograma, NodoImportacion {
    int linea();
    int columna();
}