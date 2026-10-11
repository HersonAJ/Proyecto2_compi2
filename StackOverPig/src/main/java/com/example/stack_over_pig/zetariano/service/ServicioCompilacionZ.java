package com.example.stack_over_pig.zetariano.service;

import com.example.stack_over_pig.y.errores.ErrorPosicional;
import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;
import com.example.stack_over_pig.zetariano.Builder.ASTBuilderZ;
import com.example.stack_over_pig.zetariano.nodo.*;
import com.example.stack_over_pig.zetariano.semantica.ExtractorFirmasZ;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.semantica.ValidadorSemanticoZ;
import com.example.zetariano.analizador.gramatica.ZLexer;
import com.example.zetariano.analizador.gramatica.ZParser;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServicioCompilacionZ {

    public ResultadoCompilacionZ analizar(String codigoFuente) {
        return analizar(codigoFuente, java.util.Map.of());
    }

    public ResultadoCompilacionZ analizar(String codigoFuente,
                                          java.util.Map<String, TablaSimbolosZ.DefinicionClaseExterna> externas) {

        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return new ResultadoCompilacionZ(
                    false, null, null, null,
                    List.of(), List.of(), List.of(),
                    List.of("El código está vacío")
            );
        }

        try {
            return analizarInterno(codigoFuente, externas);
        } catch (StackOverflowError soe) {
            return new ResultadoCompilacionZ(
                    false, null, null, null,
                    List.of(), List.of(), List.of(),
                    List.of("El código produjo una estructura demasiado profunda o inválida.")
            );
        } catch (Exception e) {
            return new ResultadoCompilacionZ(
                    false, null, null, null,
                    List.of(), List.of(), List.of(),
                    List.of("Error interno inesperado: " + e.getMessage())
            );
        }
    }

    private ResultadoCompilacionZ analizarInterno(String codigoFuente,
                                                  java.util.Map<String, TablaSimbolosZ.DefinicionClaseExterna> externas) {

        // 1. LEXER (Z no necesita preprocesador de indentacion: usa llaves y ';' como Java)
        List<ErrorPosicional> erroresLexicos = new ArrayList<>();

        ZLexer lexer = new ZLexer(CharStreams.fromString(codigoFuente));
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                erroresLexicos.add(new ErrorPosicional(line, charPositionInLine, msg));
            }
        });

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        if (!erroresLexicos.isEmpty()) {
            return new ResultadoCompilacionZ(
                    false, null, codigoFuente, null,
                    erroresLexicos, List.of(), List.of(), List.of()
            );
        }

        // 2. PARSER
        ZParser parser = new ZParser(tokens);

        List<ErrorPosicional> erroresSintacticos = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg,
                                    RecognitionException e) {
                erroresSintacticos.add(new ErrorPosicional(line, charPositionInLine, msg));
            }
        });

        parser.setErrorHandler(new DefaultErrorStrategy());

        ParserRuleContext tree = null;
        try {
            tree = parser.programa();
        } catch (RecognitionException re) {
            erroresSintacticos.add(new ErrorPosicional(-1, -1,
                    "Error sintactico no recuperable: " + re.getMessage()));
        }

        if (!erroresSintacticos.isEmpty()) {
            return new ResultadoCompilacionZ(
                    false, null, codigoFuente, null,
                    List.of(), erroresSintacticos, List.of(), List.of()
            );
        }

        // 3. AST
        NodoPrograma programa;
        try {
            ASTBuilderZ builder = new ASTBuilderZ();
            NodoAST nodo = builder.visit(tree);
            programa = (nodo instanceof NodoPrograma np) ? np : null;
        } catch (Exception e) {
            return new ResultadoCompilacionZ(
                    false, null, codigoFuente, null,
                    List.of(), List.of(), List.of(),
                    List.of("Error al construir el AST: " + e.getMessage())
            );
        }

        if (programa == null) {
            return new ResultadoCompilacionZ(
                    false, null, codigoFuente, null,
                    List.of(), List.of(), List.of(),
                    List.of("El AST resultante es nulo")
            );
        }

        // 4. VALIDACION SEMANTICA
        ValidadorSemanticoZ validador = new ValidadorSemanticoZ();
        List<ErrorSemantico> erroresSemanticos;
        try {
            erroresSemanticos = validador.analizar(programa, externas);
        } catch (Exception e) {
            return new ResultadoCompilacionZ(
                    false, programa, codigoFuente, null,
                    List.of(), List.of(), List.of(),
                    List.of("Error en validacion semantica: " + e.getMessage())
            );
        }

        // 5. RESULTADO
        TablaSimbolosZ tabla = validador.getTabla();
        boolean exitoso = erroresSemanticos.isEmpty();

        return new ResultadoCompilacionZ(
                exitoso,
                programa,
                codigoFuente,
                tabla,                     // ← ahora ya no es null
                List.of(),
                List.of(),
                erroresSemanticos,
                List.of()
        );
    }

    /**
     * Parsea un archivo .z y extrae SOLO las firmas (atributos, constructores, métodos)
     * sin ejecutar ninguna validación semántica. Devuelve Optional.empty() si el archivo
     * no parsea o el AST no es un NodoPrograma.
     */

    public static Optional<TablaSimbolosZ.DefinicionClaseExterna> recolectarFirmas(String codigoFuente) {
        return recolectarFirmas(codigoFuente, "");
    }

    public static Optional<TablaSimbolosZ.DefinicionClaseExterna> recolectarFirmas(String codigoFuente, String paquete) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) return java.util.Optional.empty();

        try {
            // 1. Lexer (silencioso: si hay errores, abortamos)
            ZLexer lexer = new ZLexer(CharStreams.fromString(codigoFuente));
            lexer.removeErrorListeners();
            final boolean[] huboError = {false};
            lexer.addErrorListener(new BaseErrorListener() {
                @Override public void syntaxError(Recognizer<?, ?> r, Object s,
                                                  int l, int c, String m, RecognitionException e) {
                    huboError[0] = true;
                }
            });

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();
            if (huboError[0]) return java.util.Optional.empty();

            // 2. Parser (silencioso)
            ZParser parser = new ZParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new BaseErrorListener() {
                @Override public void syntaxError(Recognizer<?, ?> r, Object s,
                                                  int l, int c, String m, RecognitionException e) {
                    huboError[0] = true;
                }
            });

            ParserRuleContext tree;
            try {
                tree = parser.programa();
            } catch (Exception e) {
                return java.util.Optional.empty();
            }
            if (huboError[0]) return java.util.Optional.empty();

            // 3. AST
            ASTBuilderZ builder = new ASTBuilderZ();
            NodoAST nodo = builder.visit(tree);
            if (!(nodo instanceof NodoPrograma np)) return java.util.Optional.empty();


            return java.util.Optional.of(ExtractorFirmasZ.extraer(np.clase(), paquete));

        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }
}
