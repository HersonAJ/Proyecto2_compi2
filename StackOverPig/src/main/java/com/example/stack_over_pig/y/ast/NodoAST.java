package com.example.stack_over_pig.y.ast;

//nodo raizn para todos los nodos del AST del lenguaje Y
public sealed interface NodoAST permits NodoExpr,
        NodoSentencia, NodoEstructura, NodoFuncion,
        NodoPrograma, NodoAtributo, NodoParametro {

    int linea();
    int columna();
}