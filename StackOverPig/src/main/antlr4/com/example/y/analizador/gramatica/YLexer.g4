lexer grammar YLexer;

//tokens de identacion procesados antes
NEWLINE : '<NEWLINE>';
INDENT  : '<INDENT>';
DEDENT  : '<DEDENT>';

//SECCIONES
SEC_ESTRUCTURAS : '%estructuras';
SEC_FUNCIONES   : '%funciones';

//palabras reservadas
ESTRUCTURA  : 'estructura';
DEFINIR     : 'definir';
RETORNAR    : 'retornar';
SI          : 'si';
ENTONCES    : 'entonces';
SINO        : 'sino';
CONTRARIO   : 'contrario';
ELEGIR      : 'elegir';
CASO        : 'caso';
ROMPER      : 'romper';
SIEMPRE     : 'siempre';
PARA        : 'para';
MIENTRAS    : 'mientras';
HACER       : 'hacer';
CONTINUAR   : 'continuar';
IMPRIMIR    : 'imprimir';
LEER        : 'leer';

//tipos primitivos
ENTERO      : 'entero';
FLOTANTE    : 'flotante';
CARACTER    : 'caracter';
CADENA      : 'cadena';
BOOL        : 'bool';

//valores booleanos
VERDADERO   : 'verdadero';
FALSO       : 'falso';

//operadores
INCREMENTO  : '++';
DECREMENTO  : '--';
IGUAL_IGUAL : '==';
DIFERENTE   : '!=';
MENOR_IGUAL : '<=';
MAYOR_IGUAL : '>=';
AND         : '&&';
OR          : '||';
MAS         : '+';
MENOS       : '-';
POR         : '*';
DIV         : '/';
MENOR       : '<';
MAYOR       : '>';
NOT         : '!';

//simbolos
FLECHA      : '->';
IGUAL       : '=';
DOS_PUNTOS  :':';
PUNTO       : '.';
COMA        : ',';
PUNTO_COMA  : ';';
PAR_IZQ     : '(';
PAR_DER     : ')';
COR_IZQ     : '[';
COR_DER     : ']';
LLAVE_IZQ   : '{';
LLAVE_DER   : '}';

//Literales
FLOTANTE_LIT    : [0-9]+ '.' [0-9]+;
ENTERO_LIT      : [0-9]+;
CADENA_LIT      : '"' (~["\r\n\\] | '\\' .)* '"';
CARACTER_LIT    : '\'' (~['\r\n\\] | '\\' .) '\'';

//Identificadores
ID  : [a-zA-Z_][a-zA-Z0-9]*;

//comentarios y espacios en blanco
COMENTARIO_LINEA    : '//' ~[\r\n]*             -> channel(HIDDEN);
COMENTARIO_BLOQUE   : '/*' .*? '*/'             -> channel(HIDDEN);
WS                  : [ \t\r\n]+                -> channel(HIDDEN);