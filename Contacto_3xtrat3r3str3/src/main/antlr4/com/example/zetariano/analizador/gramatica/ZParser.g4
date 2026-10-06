parser grammar ZParser;

options { tokenVocab = ZLexer; }

//un archivo .z contiene una unica clase publica (debe llamarse igual que el archivo)
programa : claseDefinicion EOF
         ;

claseDefinicion : PUBLIC CLASS ID LLAVE_IZQ miembroClase* LLAVE_DER
                ;

miembroClase : atributo
             | constructor
             | metodo
             ;

//atributos publicos, sin valor por defecto
atributo : tipo (COR_IZQ COR_DER)* ID PUNTO_COMA
         ;

//el constructor no lleva tipo de retorno
constructor : PUBLIC ID PAR_IZQ parametros? PAR_DER bloque
            ;

metodo : PUBLIC (VOID | tipo) ID PAR_IZQ parametros? PAR_DER bloque
       ;

parametros : parametro (COMA parametro)*
           ;

parametro : tipo ID
          ;

//tipo base: primitivo, String, o el nombre de una clase (para atributos/parametros tipo objeto)
tipo : INT | DOUBLE | CHAR | BOOLEAN | STRING | ID
     ;

bloque : LLAVE_IZQ sentencia* LLAVE_DER
       ;

sentencia   : declaracion
            | asignacion
            | expresionSentencia
            | condicional
            | switchSentencia
            | cicloFor
            | cicloWhile
            | cicloDoWhile
            | retorno
            | imprimir
            | leer
            | romper
            | continuar
            ;

//declaraciones: variable simple, objeto, o arreglo (1 o mas dimensiones via corchetes repetidos)
declaracion : tipoDeclaracion ID (IGUAL inicializador)? PUNTO_COMA
            ;

tipoDeclaracion : tipo (COR_IZQ COR_DER)*
                ;

//separado porque los arreglos aceptan la forma literal
inicializador : LLAVE_IZQ listaExpresiones? LLAVE_DER
              | expresion
              ;

listaExpresiones : expresion (COMA expresion)*
                 ;

asignacion : expresion (IGUAL | MAS_IGUAL | MENOS_IGUAL | POR_IGUAL) expresion PUNTO_COMA
           ;

expresionSentencia : expresion PUNTO_COMA
                   ;

//el 'else' se asocia automaticamente al 'if' mas cercano sin resolver
condicional : IF PAR_IZQ expresion PAR_DER sentenciaOBloque (ELSE sentenciaOBloque)?
            ;

//permite omitir llaves si el cuerpo es una sola instruccion
sentenciaOBloque : bloque
                 | sentencia
                 ;

switchSentencia : SWITCH PAR_IZQ expresion PAR_DER LLAVE_IZQ casoSwitch* casoDefault? LLAVE_DER
                ;

//el 'break' es opcional dentro del cuerpo
casoSwitch  : CASE literal DOS_PUNTOS sentencia*
            ;

casoDefault : DEFAULT DOS_PUNTOS sentencia*
            ;

//las 3 partes del for son opcionales; 'for(;;)' es valido
cicloFor : FOR PAR_IZQ forInit? PUNTO_COMA expresion? PUNTO_COMA forActualizacion? PAR_DER sentenciaOBloque
         ;

forInit : tipo ID IGUAL expresion
        | expresion
        ;

forActualizacion : expresion (INCREMENTO | DECREMENTO)
                  | expresion
                  ;

cicloWhile : WHILE PAR_IZQ expresion PAR_DER sentenciaOBloque
           ;

cicloDoWhile : DO bloque WHILE PAR_IZQ expresion PAR_DER PUNTO_COMA
             ;

retorno : RETURN expresion? PUNTO_COMA
        ;

imprimir : (PRINTLN | PRINT) PAR_IZQ expresion PAR_DER PUNTO_COMA
         ;

leer : READLN PAR_IZQ PAR_DER PUNTO_COMA
     ;

romper : BREAK PUNTO_COMA
       ;

continuar : CONTINUE PUNTO_COMA
          ;

//expresiones, de mayor a menor precedencia
expresion  : PAR_IZQ expresion PAR_DER                                          # ExprParentesis
           | NEW ID PAR_IZQ argumentos? PAR_DER                                 # ExprInstanciacionObjeto
           | NEW tipo (COR_IZQ expresion? COR_DER)+                             # ExprArregloNuevo
           | literal                                                            # ExprLiteral
           | ID                                                                 # ExprIdentificador
           | expresion PUNTO ID PAR_IZQ argumentos? PAR_DER                     # ExprLlamadaMetodo
           | expresion PUNTO ID                                                 # ExprAccesoAtributo
           | expresion COR_IZQ expresion COR_DER                                # ExprAccesoArray
           | ID PAR_IZQ argumentos? PAR_DER                                     # ExprLlamadaFuncion
           | expresion (INCREMENTO | DECREMENTO)                                # ExprPostIncrementoDecremento
           | (INCREMENTO | DECREMENTO) expresion                                # ExprPreIncrementoDecremento
           | (MENOS | NOT) expresion                                            # ExprUnaria
           | expresion (POR | DIV | MOD) expresion                              # ExprMultiplicacion
           | expresion (MAS | MENOS) expresion                                  # ExprSumaResta
           | expresion (MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL) expresion    # ExprRelacional
           | expresion (IGUAL_IGUAL | DIFERENTE) expresion                      # ExprIgualdad
           | expresion AND expresion                                            # ExprAnd
           | expresion OR expresion                                             # ExprOr
           | expresion INTERROGACION expresion DOS_PUNTOS expresion             # ExprTernaria
           ;

argumentos : expresion (COMA expresion)*
           ;

literal : ENTERO_LIT
        | DECIMAL_LIT
        | CADENA_LIT
        | CARACTER_LIT
        | TRUE
        | FALSE
        | NULL_LIT
        ;