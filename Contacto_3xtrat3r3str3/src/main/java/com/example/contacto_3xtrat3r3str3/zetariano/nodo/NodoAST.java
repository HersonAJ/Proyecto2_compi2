package com.example.contacto_3xtrat3r3str3.zetariano.nodo;

public sealed interface NodoAST permits NodoExpr, NodoSentencia,
        NodoPrograma, NodoClase, NodoAtributoZ, NodoParametroZ, NodoConstructor, NodoMetodo {

    int linea();
    int columna();
}