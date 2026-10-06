package com.example.contacto_3xtrat3r3str3.zetariano.service;

import com.example.contacto_3xtrat3r3str3.y.errores.ErrorPosicional;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.Builder.ASTBuilderZ;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.NodoAST;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.NodoPrograma;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.ValidadorSemanticoZ;
import com.example.zetariano.analizador.gramatica.ZLexer;
import com.example.zetariano.analizador.gramatica.ZParser;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

public class ServicioCompilacionZ {

    public ResultadoCompilacionZ analizar(String codigoFuente) {

        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return new ResultadoCompilacionZ(
                    false, null, null, null,
                    List.of(), List.of(), List.of(),
                    List.of("El código está vacío")
            );
        }

        try {
            return analizarInterno(codigoFuente);
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

    private ResultadoCompilacionZ analizarInterno(String codigoFuente) {

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
            erroresSemanticos = validador.analizar(programa);
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
}
