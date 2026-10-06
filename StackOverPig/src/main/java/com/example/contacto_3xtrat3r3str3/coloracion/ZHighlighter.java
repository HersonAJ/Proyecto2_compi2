package com.example.contacto_3xtrat3r3str3.coloracion;

import com.example.zetariano.analizador.gramatica.ZLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ZHighlighter {

    private static final String CSS_KEYWORD  = "z-keyword";
    private static final String CSS_MODIFIER = "z-modifier";   // public, class, void, new, return
    private static final String CSS_TYPE     = "z-type";
    private static final String CSS_BOOL     = "z-bool";
    private static final String CSS_NULL     = "z-null";
    private static final String CSS_STRING   = "z-string";
    private static final String CSS_NUMBER   = "z-number";
    private static final String CSS_COMMENT  = "z-comment";
    private static final String CSS_ID       = "z-id";
    private static final String CSS_OPERATOR = "z-operator";
    private static final String CSS_SYMBOL   = "z-symbol";

    private static final Map<Integer, String> CSS = new HashMap<>();

    static {
        // Estructura / modificadores
        CSS.put(ZLexer.PUBLIC, CSS_MODIFIER);
        CSS.put(ZLexer.CLASS,  CSS_MODIFIER);
        CSS.put(ZLexer.VOID,   CSS_MODIFIER);
        CSS.put(ZLexer.NEW,    CSS_MODIFIER);
        CSS.put(ZLexer.RETURN, CSS_MODIFIER);

        // Tipos primitivos
        CSS.put(ZLexer.INT,     CSS_TYPE);
        CSS.put(ZLexer.DOUBLE,  CSS_TYPE);
        CSS.put(ZLexer.CHAR,    CSS_TYPE);
        CSS.put(ZLexer.BOOLEAN, CSS_TYPE);
        CSS.put(ZLexer.STRING,  CSS_TYPE);

        // Literales especiales
        CSS.put(ZLexer.TRUE,     CSS_BOOL);
        CSS.put(ZLexer.FALSE,    CSS_BOOL);
        CSS.put(ZLexer.NULL_LIT, CSS_NULL);

        // Condicionales
        CSS.put(ZLexer.IF,      CSS_KEYWORD);
        CSS.put(ZLexer.ELSE,    CSS_KEYWORD);
        CSS.put(ZLexer.SWITCH,  CSS_KEYWORD);
        CSS.put(ZLexer.CASE,    CSS_KEYWORD);
        CSS.put(ZLexer.DEFAULT, CSS_KEYWORD);

        // Ciclos y control
        CSS.put(ZLexer.FOR,      CSS_KEYWORD);
        CSS.put(ZLexer.WHILE,    CSS_KEYWORD);
        CSS.put(ZLexer.DO,       CSS_KEYWORD);
        CSS.put(ZLexer.BREAK,    CSS_KEYWORD);
        CSS.put(ZLexer.CONTINUE, CSS_KEYWORD);

        // E/S
        CSS.put(ZLexer.PRINTLN, CSS_KEYWORD);
        CSS.put(ZLexer.PRINT,   CSS_KEYWORD);
        CSS.put(ZLexer.READLN,  CSS_KEYWORD);

        // Operadores aritméticos
        CSS.put(ZLexer.MAS,   CSS_OPERATOR);
        CSS.put(ZLexer.MENOS, CSS_OPERATOR);
        CSS.put(ZLexer.POR,   CSS_OPERATOR);
        CSS.put(ZLexer.DIV,   CSS_OPERATOR);
        CSS.put(ZLexer.MOD,   CSS_OPERATOR);

        // Incremento / decremento
        CSS.put(ZLexer.INCREMENTO, CSS_OPERATOR);
        CSS.put(ZLexer.DECREMENTO, CSS_OPERATOR);

        // Relacionales
        CSS.put(ZLexer.IGUAL_IGUAL, CSS_OPERATOR);
        CSS.put(ZLexer.DIFERENTE,   CSS_OPERATOR);
        CSS.put(ZLexer.MENOR_IGUAL, CSS_OPERATOR);
        CSS.put(ZLexer.MAYOR_IGUAL, CSS_OPERATOR);
        CSS.put(ZLexer.MENOR,       CSS_OPERATOR);
        CSS.put(ZLexer.MAYOR,       CSS_OPERATOR);

        // Lógicos
        CSS.put(ZLexer.AND, CSS_OPERATOR);
        CSS.put(ZLexer.OR,  CSS_OPERATOR);
        CSS.put(ZLexer.NOT, CSS_OPERATOR);

        // Asignación
        CSS.put(ZLexer.MAS_IGUAL,   CSS_OPERATOR);
        CSS.put(ZLexer.MENOS_IGUAL, CSS_OPERATOR);
        CSS.put(ZLexer.POR_IGUAL,   CSS_OPERATOR);
        CSS.put(ZLexer.IGUAL,       CSS_OPERATOR);

        // Ternario
        CSS.put(ZLexer.INTERROGACION, CSS_OPERATOR);
        CSS.put(ZLexer.DOS_PUNTOS,    CSS_SYMBOL);

        // Símbolos
        CSS.put(ZLexer.PUNTO_COMA, CSS_SYMBOL);
        CSS.put(ZLexer.COMA,       CSS_SYMBOL);
        CSS.put(ZLexer.PUNTO,      CSS_SYMBOL);
        CSS.put(ZLexer.PAR_IZQ,    CSS_SYMBOL);
        CSS.put(ZLexer.PAR_DER,    CSS_SYMBOL);
        CSS.put(ZLexer.LLAVE_IZQ,  CSS_SYMBOL);
        CSS.put(ZLexer.LLAVE_DER,  CSS_SYMBOL);
        CSS.put(ZLexer.COR_IZQ,    CSS_SYMBOL);
        CSS.put(ZLexer.COR_DER,    CSS_SYMBOL);

        // Literales
        CSS.put(ZLexer.DECIMAL_LIT,  CSS_NUMBER);
        CSS.put(ZLexer.ENTERO_LIT,   CSS_NUMBER);
        CSS.put(ZLexer.CADENA_LIT,   CSS_STRING);
        CSS.put(ZLexer.CARACTER_LIT, CSS_STRING);

        // Identificadores
        CSS.put(ZLexer.ID, CSS_ID);

        // Comentarios
        CSS.put(ZLexer.COMENTARIO_LINEA,  CSS_COMMENT);
        CSS.put(ZLexer.COMENTARIO_BLOQUE, CSS_COMMENT);
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String codigoOriginal) {
        ZLexer lexer = new ZLexer(CharStreams.fromString(codigoOriginal));
        lexer.removeErrorListeners();

        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        Token tok;
        while ((tok = lexer.nextToken()).getType() != Token.EOF) {
            int start = tok.getStartIndex();
            int stop  = tok.getStopIndex() + 1;

            if (start > lastEnd) {
                builder.add(Collections.emptyList(), start - lastEnd);
            }

            String cssClass = CSS.get(tok.getType());
            Collection<String> style = (cssClass == null)
                    ? Collections.emptyList()
                    : Collections.singletonList(cssClass);

            builder.add(style, stop - start);
            lastEnd = stop;
        }

        if (lastEnd < codigoOriginal.length()) {
            builder.add(Collections.emptyList(), codigoOriginal.length() - lastEnd);
        }

        return builder.create();
    }
}