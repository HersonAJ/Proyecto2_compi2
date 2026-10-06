package com.example.contacto_3xtrat3r3str3.piglatin.service;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.GeneradorArchivoC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.GeneradorC;
import com.example.contacto_3xtrat3r3str3.piglatin.builder.ASTBuilderPig;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoAST;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoPrograma;
import com.example.contacto_3xtrat3r3str3.piglatin.semantica.ValidadorSemanticoPig;
import com.example.contacto_3xtrat3r3str3.y.errores.ErrorPosicional;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.piglatin.analizador.gramatica.PigLexer;
import com.example.piglatin.analizador.gramatica.PigParser;
import org.antlr.v4.runtime.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ServicioCompilacionPig {

    private ValidadorSemanticoPig ultimoValidador;

    // ANALIZAR (sin generar C)
    public ResultadoCompilacionPig analizar(String codigoFuente, Path carpetaRaiz) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("El código está vacío"),
                    null, false, null
            );
        }

        try {
            return analizarInterno(codigoFuente, carpetaRaiz);
        } catch (StackOverflowError soe) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("Estructura demasiado profunda o inválida."),
                    null, false, null
            );
        } catch (Exception e) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("Error interno inesperado: " + e.getMessage()),
                    null, false, null
            );
        }
    }

    // COMPILAR (analizar + generar C + gcc)
    public ResultadoCompilacionPig compilar(String codigoFuente,
                                            Path carpetaRaiz,
                                            Path rutaDestinoC) {
        ResultadoCompilacionPig resultado = analizar(codigoFuente, carpetaRaiz);

        if (!resultado.isExitoso()) {
            return resultado;
        }

        ValidadorSemanticoPig validador = ultimoValidador;
        if (validador == null) {
            return new ResultadoCompilacionPig(
                    false, resultado.getPrograma(),
                    List.of(), List.of(),
                    List.of(new ErrorSemantico(-1, -1, "Compilación C",
                            "No se pudo recuperar el validador semántico.")),
                    List.of(),
                    null, false, null
            );
        }

        String codigoC;
        boolean compilacionOk = false;
        String rutaExe = null;
        List<ErrorSemantico> errores = new ArrayList<>(resultado.getErroresSemanticos());

        try {
            codigoC = generarCodigoC(resultado.getPrograma(), validador);

            // Ruta del ejecutable: mismo nombre que el .c, sin extensión.
            String nombreSinExt = rutaDestinoC.getFileName().toString();
            int punto = nombreSinExt.lastIndexOf('.');
            if (punto > 0) nombreSinExt = nombreSinExt.substring(0, punto);
            Path rutaExePath = rutaDestinoC.getParent().resolve(nombreSinExt);

            GeneradorArchivoC gen = new GeneradorArchivoC();
            compilacionOk = gen.generarYCompilar(codigoC, rutaDestinoC, rutaExePath);

            if (compilacionOk) {
                rutaExe = rutaExePath.toAbsolutePath().toString();
            }
        } catch (Exception e) {
            errores.add(new ErrorSemantico(-1, -1, "Generación C",
                    "Error al generar/compilar: " + e.getMessage()));
            return new ResultadoCompilacionPig(
                    false,
                    resultado.getPrograma(),
                    resultado.getErroresLexicos(),
                    resultado.getErroresSintacticos(),
                    errores,
                    resultado.getMensajesInternos(),
                    null, false, null
            );
        }

        return new ResultadoCompilacionPig(
                true,
                resultado.getPrograma(),
                resultado.getErroresLexicos(),
                resultado.getErroresSintacticos(),
                errores,
                resultado.getMensajesInternos(),
                codigoC,
                compilacionOk,
                rutaExe
        );
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
                    null, false, null
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
                    null, false, null
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
                    null, false, null
            );
        }

        if (programa == null) {
            return new ResultadoCompilacionPig(
                    false, null,
                    List.of(), List.of(), List.of(),
                    List.of("El AST resultante es nulo"),
                    null, false, null
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
                    null, false, null
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
                false,
                null
        );
    }

    // GENERACION DE C
    private String generarCodigoC(NodoPrograma programa, ValidadorSemanticoPig validador) {
        var importaciones = validador.getImportaciones();
        var funcionesImportadas = importaciones.getFuncionesImportadas();
        var estructurasImportadas = importaciones.getEstructurasImportadas();

        var tabla = validador.getTabla();

        var variablesGlobales = programa.aVariablesGlobalesC(tabla);
        var mainC = programa.aMainC(tabla);

        var funciones = new ArrayList<>(funcionesImportadas);
        funciones.add(mainC);

        var estructuras = new ArrayList<EstructuraC>();
        var nombresVistos = new java.util.HashSet<String>();
        for (var e : estructurasImportadas) {
            if (nombresVistos.add(e.getNombre())) {
                estructuras.add(e);
            }
        }

        return new GeneradorC().generar(funciones, estructuras, variablesGlobales, false);
    }
}