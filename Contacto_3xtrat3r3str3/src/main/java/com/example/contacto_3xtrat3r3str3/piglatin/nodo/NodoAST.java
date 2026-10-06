package com.example.contacto_3xtrat3r3str3.piglatin.nodo;

public sealed interface NodoAST permits NodoExpr, NodoSentencia, NodoPrograma, NodoImportacion {
    int linea();
    int columna();
}