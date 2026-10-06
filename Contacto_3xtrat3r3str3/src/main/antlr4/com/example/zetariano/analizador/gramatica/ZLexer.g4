lexer grammar ZLexer;

//palabras reservadas de estructura
PUBLIC      : 'public';
CLASS       : 'class';
VOID        : 'void';
NEW         : 'new';
RETURN      : 'return';

//tipos primitivos
INT         : 'int';
DOUBLE      : 'double';
CHAR        : 'char';
BOOLEAN     : 'boolean';
STRING      : 'String';

//valores literales especiales
TRUE        : 'true';
FALSE       : 'false';
NULL_LIT    : 'null';

//condicionales
IF          : 'if';
ELSE        : 'else';
SWITCH      : 'switch';
CASE        : 'case';
DEFAULT     : 'default';

//ciclos y control de flujo
FOR         : 'for';
WHILE       : 'while';
DO          : 'do';
BREAK       : 'break';
CONTINUE    : 'continue';

//funciones especiales de E/S
PRINTLN     : 'println';
PRINT       : 'print';
READLN      : 'readln';

//operadores aritmeticos
MAS         : '+';
MENOS       : '-';
POR         : '*';
DIV         : '/';
MOD         : '%';

//incremento/decremento
INCREMENTO  : '++';
DECREMENTO  : '--';

//relacionales
IGUAL_IGUAL : '==';
DIFERENTE   : '!=';
MENOR_IGUAL : '<=';
MAYOR_IGUAL : '>=';
MENOR       : '<';
MAYOR       : '>';

//logicos
AND         : '&&';
OR          : '||';
NOT         : '!';

//asignacion (simple y compuesta; ver nota sobre /= y %= mas abajo)
MAS_IGUAL   : '+=';
MENOS_IGUAL : '-=';
POR_IGUAL   : '*=';
IGUAL       : '=';

//ternario
INTERROGACION : '?';
DOS_PUNTOS    : ':';

//simbolos
PUNTO_COMA  : ';';
COMA        : ',';
PUNTO       : '.';
PAR_IZQ     : '(';
PAR_DER     : ')';
LLAVE_IZQ   : '{';
LLAVE_DER   : '}';
COR_IZQ     : '[';
COR_DER     : ']';

//literales
DECIMAL_LIT  : [0-9]+ '.' [0-9]+;
ENTERO_LIT   : [0-9]+;
CADENA_LIT   : '"' (~["\r\n\\] | '\\' .)* '"';
CARACTER_LIT : '\'' (~['\r\n\\] | '\\' .) '\'';

//identificadores
ID  : [a-zA-Z_][a-zA-Z0-9_]*;

//comentarios y espacios en blanco
COMENTARIO_LINEA  : '//' ~[\r\n]* -> skip;
COMENTARIO_BLOQUE : '/*' .*? '*/' -> skip;
WS                : [ \t\r\n]+ -> skip;