package com.example.contacto_3xtrat3r3str3.coloracion;
import com.example.y.analizador.gramatica.YLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class YHighlighter {

    // Clases CSS -> definidas en el stylesheet
    private static final String CSS_KEYWORD  = "y-keyword";
    private static final String CSS_SECTION  = "y-section";
    private static final String CSS_TYPE     = "y-type";
    private static final String CSS_STRING   = "y-string";
    private static final String CSS_NUMBER   = "y-number";
    private static final String CSS_COMMENT  = "y-comment";
    private static final String CSS_ID       = "y-id";
    private static final String CSS_OPERATOR = "y-operator";
    private static final String CSS_SYMBOL   = "y-symbol";

    private static final Map<Integer, String> CSS = new HashMap<>();

    static {
        CSS.put(YLexer.SEC_ESTRUCTURAS, CSS_SECTION);
        CSS.put(YLexer.SEC_FUNCIONES,   CSS_SECTION);

        // Palabras reservadas
        CSS.put(YLexer.ESTRUCTURA, CSS_KEYWORD);
        CSS.put(YLexer.DEFINIR,    CSS_KEYWORD);
        CSS.put(YLexer.RETORNAR,   CSS_KEYWORD);
        CSS.put(YLexer.SI,         CSS_KEYWORD);
        CSS.put(YLexer.ENTONCES,   CSS_KEYWORD);
        CSS.put(YLexer.SINO,       CSS_KEYWORD);
        CSS.put(YLexer.CONTRARIO,  CSS_KEYWORD);
        CSS.put(YLexer.ELEGIR,     CSS_KEYWORD);
        CSS.put(YLexer.CASO,       CSS_KEYWORD);
        CSS.put(YLexer.ROMPER,     CSS_KEYWORD);
        CSS.put(YLexer.SIEMPRE,    CSS_KEYWORD);
        CSS.put(YLexer.PARA,       CSS_KEYWORD);
        CSS.put(YLexer.MIENTRAS,   CSS_KEYWORD);
        CSS.put(YLexer.HACER,      CSS_KEYWORD);
        CSS.put(YLexer.CONTINUAR,  CSS_KEYWORD);
        CSS.put(YLexer.IMPRIMIR,   CSS_KEYWORD);
        CSS.put(YLexer.LEER,       CSS_KEYWORD);

        // Tipos primitivos
        CSS.put(YLexer.ENTERO,   CSS_TYPE);
        CSS.put(YLexer.FLOTANTE, CSS_TYPE);
        CSS.put(YLexer.CARACTER, CSS_TYPE);
        CSS.put(YLexer.CADENA,   CSS_TYPE);
        CSS.put(YLexer.BOOL,     CSS_TYPE);

        // Booleanos
        CSS.put(YLexer.VERDADERO, CSS_KEYWORD);
        CSS.put(YLexer.FALSO,     CSS_KEYWORD);

        // Operadores
        CSS.put(YLexer.INCREMENTO,  CSS_OPERATOR);
        CSS.put(YLexer.DECREMENTO,  CSS_OPERATOR);
        CSS.put(YLexer.IGUAL_IGUAL, CSS_OPERATOR);
        CSS.put(YLexer.DIFERENTE,   CSS_OPERATOR);
        CSS.put(YLexer.MENOR_IGUAL, CSS_OPERATOR);
        CSS.put(YLexer.MAYOR_IGUAL, CSS_OPERATOR);
        CSS.put(YLexer.AND,         CSS_OPERATOR);
        CSS.put(YLexer.OR,          CSS_OPERATOR);
        CSS.put(YLexer.MAS,         CSS_OPERATOR);
        CSS.put(YLexer.MENOS,       CSS_OPERATOR);
        CSS.put(YLexer.POR,         CSS_OPERATOR);
        CSS.put(YLexer.DIV,         CSS_OPERATOR);
        CSS.put(YLexer.MENOR,       CSS_OPERATOR);
        CSS.put(YLexer.MAYOR,       CSS_OPERATOR);
        CSS.put(YLexer.NOT,         CSS_OPERATOR);

        // Símbolos
        CSS.put(YLexer.FLECHA,     CSS_SYMBOL);
        CSS.put(YLexer.IGUAL,      CSS_SYMBOL);
        CSS.put(YLexer.DOS_PUNTOS, CSS_SYMBOL);
        CSS.put(YLexer.PUNTO,      CSS_SYMBOL);
        CSS.put(YLexer.COMA,       CSS_SYMBOL);
        CSS.put(YLexer.PUNTO_COMA, CSS_SYMBOL);
        CSS.put(YLexer.PAR_IZQ,    CSS_SYMBOL);
        CSS.put(YLexer.PAR_DER,    CSS_SYMBOL);
        CSS.put(YLexer.COR_IZQ,    CSS_SYMBOL);
        CSS.put(YLexer.COR_DER,    CSS_SYMBOL);
        CSS.put(YLexer.LLAVE_IZQ,  CSS_SYMBOL);
        CSS.put(YLexer.LLAVE_DER,  CSS_SYMBOL);

        // Literales
        CSS.put(YLexer.FLOTANTE_LIT, CSS_NUMBER);
        CSS.put(YLexer.ENTERO_LIT,   CSS_NUMBER);
        CSS.put(YLexer.CADENA_LIT,   CSS_STRING);
        CSS.put(YLexer.CARACTER_LIT, CSS_STRING);

        // Identificadores
        CSS.put(YLexer.ID, CSS_ID);

        // Comentarios
        CSS.put(YLexer.COMENTARIO_LINEA,  CSS_COMMENT);
        CSS.put(YLexer.COMENTARIO_BLOQUE, CSS_COMMENT);
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String codigoOriginal) {
        YLexer lexer = new YLexer(CharStreams.fromString(codigoOriginal));
        lexer.removeErrorListeners();

        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        int lastEnd = 0;

        Token tok;
        while ((tok = lexer.nextToken()).getType() != Token.EOF) {
            int type = tok.getType();
            if (type == YLexer.NEWLINE || type == YLexer.INDENT || type == YLexer.DEDENT) {
                continue;
            }

            int start = tok.getStartIndex();
            int stop  = tok.getStopIndex() + 1;
            if (start > lastEnd) {
                builder.add(Collections.emptyList(), start - lastEnd);
            }

            String cssClass = CSS.get(type);
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