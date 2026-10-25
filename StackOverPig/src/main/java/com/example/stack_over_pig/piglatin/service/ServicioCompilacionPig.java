package com.example.stack_over_pig.piglatin.service;

import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.Cuarteta;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.stack_over_pig.piglatin.builder.ASTBuilderPig;
import com.example.stack_over_pig.piglatin.nodo.NodoAST;
import com.example.stack_over_pig.piglatin.nodo.NodoPrograma;
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;
import com.example.stack_over_pig.piglatin.semantica.ValidadorSemanticoPig;
import com.example.stack_over_pig.riscv.EmisorRiscV;
import com.example.stack_over_pig.riscv.piglatin.GeneradorCuartetasPig;
import com.example.stack_over_pig.y.errores.ErrorPosicional;
import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;
import com.example.piglatin.analizador.gramatica.PigLexer;
import com.example.piglatin.analizador.gramatica.PigParser;
import org.antlr.v4.runtime.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServicioCompilacionPig {

    private ValidadorSemanticoPig ultimoValidador;

    // ANALIZAR (sin generar RISC-V)
    public ResultadoCompilacionPig analizar(String codigoFuente, Path carpetaRaiz) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("El código está vacío"),
                    null, null
            );
        }

        try {
            return analizarInterno(codigoFuente, carpetaRaiz);
        } catch (StackOverflowError soe) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("Estructura demasiado profunda o inválida."),
                    null, null
            );
        } catch (Exception e) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("Error interno inesperado: " + e.getMessage()),
                    null, null
            );
        }
    }

    // COMPILAR A RISC-V (analizar + generar cuartetas + emitir assembler)
    public ResultadoCompilacionPig compilarRiscV(String codigoFuente, Path carpetaRaiz) {
        ResultadoCompilacionPig resultado = analizar(codigoFuente, carpetaRaiz);

        if (!resultado.isExitoso()) {
            return resultado;
        }

        try {
            TablaSimbolosPig tabla = ultimoValidador.getTabla();
            GestorCodigoIntermedio gestor = new GestorCodigoIntermedio();
            GeneradorCuartetasPig generador = new GeneradorCuartetasPig(gestor, tabla);
            resultado.getPrograma().aceptar(generador);

            List<Cuarteta> cuartetas = gestor.getCuartetas();

            EmisorRiscV emisor = new EmisorRiscV();
            String codigoRiscV = emisor.emitir(cuartetas);

            return new ResultadoCompilacionPig(
                    true,
                    resultado.getPrograma(),
                    resultado.getErroresLexicos(),
                    resultado.getErroresSintacticos(),
                    resultado.getErroresSemanticos(),
                    resultado.getMensajesInternos(),
                    codigoRiscV,
                    null
            );
        } catch (Exception e) {
            return new ResultadoCompilacionPig(
                    false,
                    resultado.getPrograma(),
                    resultado.getErroresLexicos(),
                    resultado.getErroresSintacticos(),
                    List.of(new ErrorSemantico(-1, -1, "RISC-V",
                            "Error al generar RISC-V: " + e.getMessage())),
                    resultado.getMensajesInternos(),
                    null, null
            );
        }
    }

    // IMPLEMENTACIÓN INTERNA
    private ResultadoCompilacionPig analizarInterno(String codigoFuente, Path carpetaRaiz) {

        // 1. LEXER
        List<ErrorPosicional> erroresLexicos = new ArrayList<>();

        PigLexer lexer = new PigLexer(CharStreams.fromString(codigoFuente));
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
            return new ResultadoCompilacionPig(
                    false, null,
                    erroresLexicos, List.of(), List.of(), List.of(),
                    null, null
            );
        }

        // 2. PARSER
        PigParser parser = new PigParser(tokens);
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
                    "Error sintáctico no recuperable: " + re.getMessage()));
        }

        if (!erroresSintacticos.isEmpty()) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), erroresSintacticos, List.of(), List.of(),
                    null, null
            );
        }

        // 3. AST
        NodoPrograma programa;
        try {
            ASTBuilderPig builder = new ASTBuilderPig();
            NodoAST nodo = builder.visit(tree);
            programa = (nodo instanceof NodoPrograma np) ? np : null;
        } catch (Exception e) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("Error al construir el AST: " + e.getMessage()),
                    null, null
            );
        }

        if (programa == null) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("El AST resultante es nulo"),
                    null, null
            );
        }

        // 4. VALIDACIÓN SEMÁNTICA
        List<ErrorSemantico> erroresSemanticos = new ArrayList<>();
        try {
            ValidadorSemanticoPig validador = new ValidadorSemanticoPig(carpetaRaiz);
            erroresSemanticos = validador.analizar(programa);
            ultimoValidador = validador;
        } catch (Exception e) {
            return new ResultadoCompilacionPig(
                    false, programa,
                    List.of(), List.of(), List.of(),
                    List.of("Error en validación semántica: " + e.getMessage()),
                    null, null
            );
        }

        boolean exitoso = erroresSemanticos.isEmpty();

        return new ResultadoCompilacionPig(
                exitoso,
                programa,
                List.of(),
                List.of(),
                erroresSemanticos,
                List.of(),
                null,
                null
        );
    }
}