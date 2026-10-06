parser grammar PigParser;

options { tokenVocab = PigLexer; }

// PROGRAMA
programa
    : importacion* seccionVariables? seccionMain FIN_PROGRAMA PUNTOCOMA EOF
    ;

// IMPORTACIONES
importacion
    : IMPORT rutaArchivo
    ;

rutaArchivo
    : ID (PUNTO ID)* (PUNTO tipoArchivo)?
    ;

tipoArchivo
    : ID
    ;

// VARIABLES GLOBALES (OPCIONAL)
seccionVariables
    : VARIABILES MAYOR declaracionVar*
    ;

// SECCION PRINCIPAL (OBLIGATORIA)
seccionMain
    : MAIOR MAYOR sentencia*
    ;

// DECLARACIONES
declaracionVar
    : variable
    | arreglo
    | structInstancia
    ;

variable
    : ESTO ID DOSPUNTOS tipoPrimitivo expr? PUNTOCOMA
    | ESTO ID DOSPUNTOS expr PUNTOCOMA
    ;

tipoPrimitivo
    : NUMERUS | TEXTUM | DECIMALIS | LITTERA | BOOL
    ;

tipo
    : tipoPrimitivo
    | ID
    ;

arreglo
    : SERIES ID CORCH_A INT CORCH_C DOSPUNTOS tipo (LLAVE_A listaExpr LLAVE_C)? PUNTOCOMA
    ;

listaExpr
    : expr (COMA expr)*
    ;

structInstancia
    : ESTO ID DOSPUNTOS ID literalStruct
    ;

literalStruct
    : LLAVE_A listaExpr? LLAVE_C
    ;

// SENTENCIAS
sentencia
    : declaracionVar
    | asignacion
    | condicional
    | cicloDum
    | cicloFacere
    | cicloPer
    | lectura
    | escritura
    | interrupcionCiclo
    | llamadaFuncion PUNTOCOMA
    | llamadaMetodo PUNTOCOMA
    | incrementoDecremento
    ;

asignacion
    : referencia ASIGNAR expr PUNTOCOMA
    ;

referencia
    : ID                                           # referenciaBase
    | referencia PUNTO ID                          # accesoAtributo
    | referencia CORCH_A expr CORCH_C              # accesoArray
    ;

//llamada a metodo usada como sentencia suelta, ej. 'misObjetos[9].hablar(miObjeto.getNombre());'
llamadaMetodo
    : referencia PUNTO ID PAR_A listaArgumentos? PAR_C
    ;

incrementoDecremento
    : (referencia (INC | DEC) | (INC | DEC) referencia) PUNTOCOMA
    ;

condicional
    : SI PAR_A expr PAR_C bloqueSentencias
      ramaAliter*
      ramaElse?
      FINIS PUNTOCOMA
    ;

bloqueSentencias
    : LLAVE_A sentencia* LLAVE_C
    ;

ramaAliter
    : ALITER PAR_A expr PAR_C bloqueSentencias
    ;

ramaElse
    : ALITER bloqueSentencias
    ;

cicloDum
    : DUM PAR_A expr PAR_C LLAVE_A sentencia* LLAVE_C FINIS PUNTOCOMA
    ;

cicloFacere
    : FACERE LLAVE_A sentencia* LLAVE_C DUM PAR_A expr PAR_C PUNTOCOMA
    ;

cicloPer
    : PER PAR_A variable expr PUNTOCOMA incremento PAR_C LLAVE_A sentencia* LLAVE_C
    ;

incremento
    : referencia (INC | DEC)
    | referencia ASIGNAR expr
    ;

interrupcionCiclo
    : (PERGE | INTERRUMPE) PUNTOCOMA
    ;

lectura
    : ID? LEER
    ;

escritura
    : ESCRIBIR expr (ESCRIBIR expr)* PUNTOCOMA
    ;

llamadaFuncion
    : ID PAR_A listaArgumentos? PAR_C
    ;

listaArgumentos
    : expr (COMA expr)*
    ;

// EXPRESIONES
expr
    : PAR_A expr PAR_C                                          # exprParentesis
    | MENOS expr                                                # exprNegacionUnaria
    | NOVUS ID PAR_A listaArgumentos? PAR_C                     # exprInstanciaObjeto
    | LLAVE_A listaExpr? LLAVE_C                                # exprLiteralCompuesto
    | (INC | DEC) ID                                            # exprIncDecPrefijo
    | ID (INC | DEC)                                            # exprIncDecPostfijo
    | expr op=(MULT | DIV) expr                                 # exprMulDiv
    | expr op=(MAS | MENOS) expr                                # exprSumaResta
    | expr op=(MENOR | MAYOR | MENIG | MAYIG) expr              # exprRelacional
    | expr op=(IGUAL | DISTINTO) expr                           # exprIgualdad
    | expr AND expr                                             # exprAnd
    | expr OR expr                                              # exprOr
    | expr PUNTO ID PAR_A listaArgumentos? PAR_C                # exprLlamadaMetodo
    | llamadaFuncion                                            # exprLlamada
    | referencia                                                # exprReferencia
    | INT                                                       # exprEntero
    | FLOAT                                                     # exprDecimal
    | STRING                                                    # exprTexto
    | CHAR                                                      # exprCaracter
    | VERUM                                                     # exprVerum
    | FALSUS                                                    # exprFalsus
    ;