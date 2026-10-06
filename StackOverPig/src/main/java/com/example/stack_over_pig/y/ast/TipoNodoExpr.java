package com.example.stack_over_pig.y.ast;

public enum TipoNodoExpr {
    //LITERALES
    LITERAL_ENTERO,
    LITERAL_FLOTANTE,
    LITERAL_CADENA,
    LITERAL_CARACTER,
    LITERAL_BOOL,

    //ACCESOS
    IDENTIFICADOR,
    ACCESO_ARRAY,
    ACCESO_ATRIBUTO,

    //OPERACIONES
    BINARIA,
    UNARIA,

    //LAMADAS
    LLAMADA_FUNCION,
    LEER
}
