package com.example.contacto_3xtrat3r3str3.coloracion;

import com.example.piglatin.analizador.gramatica.PigLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PigHighlighter {

    private static final String CSS_SECTION  = "pig-section";   // VARIABILES, MAIOR, FINIS (mayúscula)
    private static final String CSS_KEYWORD  = "pig-keyword";   // esto, series, si, dum, etc.
    private static final String CSS_TYPE     = "pig-type";      // numerus, textum, etc.
    private static final String CSS_BOOL     = "pig-bool";      // verum, falsus
    private static final String CSS_IMPORT   = "pig-import";    // import
    private static final String CSS_STRING   = "pig-string";
    private static final String CSS_NUMBER   = "pig-number";
    private static final String CSS_COMMENT  = "pig-comment";
    private static final String CSS_ID       = "pig-id";
    private static final String CSS_OPERATOR = "pig-operator";
    private static final String CSS_SYMBOL   = "pig-symbol";

    private static final Map<Integer, String> CSS = new HashMap<>();

    static {
        // Secciones del programa
        CSS.put(PigLexer.VARIABILES,   CSS_SECTION);
        CSS.put(PigLexer.MAIOR,        CSS_SECTION);
        CSS.put(PigLexer.FIN_PROGRAMA, CSS_SECTION);

        // Importaciones
        CSS.put(PigLexer.IMPORT, CSS_IMPORT);

        // Palabras reservadas
        CSS.put(PigLexer.ESTO,       CSS_KEYWORD);
        CSS.put(PigLexer.SERIES,     CSS_KEYWORD);
        CSS.put(PigLexer.NOVUS,      CSS_KEYWORD);
        CSS.put(PigLexer.FINIS,      CSS_KEYWORD);
        CSS.put(PigLexer.SI,         CSS_KEYWORD);
        CSS.put(PigLexer.ALITER,     CSS_KEYWORD);
        CSS.put(PigLexer.DUM,        CSS_KEYWORD);
        CSS.put(PigLexer.FACERE,     CSS_KEYWORD);
        CSS.put(PigLexer.PER,        CSS_KEYWORD);
        CSS.put(PigLexer.PERGE,      CSS_KEYWORD);
        CSS.put(PigLexer.INTERRUMPE, CSS_KEYWORD);

        // Tipos primitivos
        CSS.put(PigLexer.NUMERUS,   CSS_TYPE);
        CSS.put(PigLexer.TEXTUM,    CSS_TYPE);
        CSS.put(PigLexer.DECIMALIS, CSS_TYPE);
        CSS.put(PigLexer.LITTERA,   CSS_TYPE);
        CSS.put(PigLexer.BOOL,      CSS_TYPE);

        // Valores booleanos
        CSS.put(PigLexer.VERUM,  CSS_BOOL);
        CSS.put(PigLexer.FALSUS, CSS_BOOL);

        // Operadores de dos caracteres
        CSS.put(PigLexer.LEER,     CSS_OPERATOR);
        CSS.put(PigLexer.ESCRIBIR, CSS_OPERATOR);
        CSS.put(PigLexer.INC,      CSS_OPERATOR);
        CSS.put(PigLexer.DEC,      CSS_OPERATOR);
        CSS.put(PigLexer.IGUAL,    CSS_OPERATOR);
        CSS.put(PigLexer.DISTINTO, CSS_OPERATOR);
        CSS.put(PigLexer.MENIG,    CSS_OPERATOR);
        CSS.put(PigLexer.MAYIG,    CSS_OPERATOR);
        CSS.put(PigLexer.AND,      CSS_OPERATOR);
        CSS.put(PigLexer.OR,       CSS_OPERATOR);

        // Operadores y símbolos de un caracter
        CSS.put(PigLexer.ASIGNAR,   CSS_OPERATOR);
        CSS.put(PigLexer.MAS,       CSS_OPERATOR);
        CSS.put(PigLexer.MENOS,     CSS_OPERATOR);
        CSS.put(PigLexer.MULT,      CSS_OPERATOR);
        CSS.put(PigLexer.DIV,       CSS_OPERATOR);
        CSS.put(PigLexer.MENOR,     CSS_OPERATOR);
        CSS.put(PigLexer.MAYOR,     CSS_OPERATOR);

        CSS.put(PigLexer.DOSPUNTOS, CSS_SYMBOL);
        CSS.put(PigLexer.PUNTOCOMA, CSS_SYMBOL);
        CSS.put(PigLexer.COMA,      CSS_SYMBOL);
        CSS.put(PigLexer.PUNTO,     CSS_SYMBOL);
        CSS.put(PigLexer.LLAVE_A,   CSS_SYMBOL);
        CSS.put(PigLexer.LLAVE_C,   CSS_SYMBOL);
        CSS.put(PigLexer.CORCH_A,   CSS_SYMBOL);
        CSS.put(PigLexer.CORCH_C,   CSS_SYMBOL);
        CSS.put(PigLexer.PAR_A,     CSS_SYMBOL);
        CSS.put(PigLexer.PAR_C,     CSS_SYMBOL);

        // Literales
        CSS.put(PigLexer.FLOAT,  CSS_NUMBER);
        CSS.put(PigLexer.INT,    CSS_NUMBER);
        CSS.put(PigLexer.STRING, CSS_STRING);
        CSS.put(PigLexer.CHAR,   CSS_STRING);

        // Identificadores
        CSS.put(PigLexer.ID, CSS_ID);

        // Comentarios
        CSS.put(PigLexer.LINEA_COMENTARIO,  CSS_COMMENT);
        CSS.put(PigLexer.BLOQUE_COMENTARIO, CSS_COMMENT);
    }

    public static StyleSpans<Collection<String>> computeHighlighting(String codigoOriginal) {
        PigLexer lexer = new PigLexer(CharStreams.fromString(codigoOriginal));
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