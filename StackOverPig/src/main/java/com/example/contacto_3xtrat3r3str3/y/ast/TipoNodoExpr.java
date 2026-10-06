package com.example.contacto_3xtrat3r3str3.y.ast;

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
