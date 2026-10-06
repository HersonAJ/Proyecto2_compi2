package com.example.stack_over_pig.zetariano.nodo;

public sealed interface NodoAST permits NodoExpr, NodoSentencia,
        NodoPrograma, NodoClase, NodoAtributoZ, NodoParametroZ, NodoConstructor, NodoMetodo {

    int linea();
    int columna();
}