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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServicioCompilacionZ {

    public ResultadoCompilacionZ analizar(String codigoFuente) {
        return analizar(codigoFuente, Map.of());
    }

    /** Analiza un codigo cuyo archivo esta en la raiz del proyecto (paquete ""). */
    public ResultadoCompilacionZ analizar(String codigoFuente,
                                          Map<String, TablaSimbolosZ.DefinicionClaseExterna> externas) {
        return analizar(codigoFuente, externas, "");
    }

    /**
     * Analiza un codigo dado el resto de clases que puede usar ('externas', p. ej. sus superclases)
     * y el paquete (carpeta) en el que esta su archivo.
     */
    public ResultadoCompilacionZ analizar(String codigoFuente,
                                          Map<String, TablaSimbolosZ.DefinicionClaseExterna> externas,
                                          String paquete) {

        if (codigoFuente == null || codigoFuente.trim().isEmpty()) {
            return new ResultadoCompilacionZ(
                    false, null, null, null,
                    List.of(), List.of(), List.of(),
                    List.of("El código está vacío")
            );
        }

        try {
            return analizarInterno(codigoFuente, externas, paquete);
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

    /**
     * Analiza un archivo .z buscando en su carpeta las superclases que necesite.
     * El paquete es la carpeta del archivo relativa a 'carpetaRaiz'.
     */
    public ResultadoCompilacionZ analizarArchivo(Path archivo, Path carpetaRaiz) {
        String contenido;
        try {
            contenido = Files.readString(archivo);
        } catch (IOException e) {
            return new ResultadoCompilacionZ(
                    false, null, null, null,
                    List.of(), List.of(), List.of(),
                    List.of("No se pudo leer '" + archivo + "': " + e.getMessage())
            );
        }
        CargadorClasesZ cargador = new CargadorClasesZ(carpetaRaiz);
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> externas =
                cargador.cargarTodasLasClases(archivo);
        return analizar(contenido, externas, "");
    }

    /** Igual que arriba, tomando como raiz la carpeta del propio archivo (paquete ""). */
    public ResultadoCompilacionZ analizarArchivo(Path archivo) {
        return analizarArchivo(archivo, archivo.toAbsolutePath().getParent());
    }

    private ResultadoCompilacionZ analizarInterno(String codigoFuente,
                                                  Map<String, TablaSimbolosZ.DefinicionClaseExterna> externas,
                                                  String paquete) {

        // 1. LEXER
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
            erroresSemanticos = validador.analizar(programa, externas, paquete);
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
                tabla,
                List.of(),
                List.of(),
                erroresSemanticos,
                List.of()
        );
    }

    /**
     * Parsea un archivo .z y extrae SOLO las firmas (atributos, constructores, métodos)
     * sin ejecutar ninguna validación semántica. Devuelve Optional.empty() si el archivo
     * no parsea o el AST no es un NodoPrograma. El paquete es "" (raiz del proyecto).
     */
    public static Optional<TablaSimbolosZ.DefinicionClaseExterna> recolectarFirmas(String codigoFuente) {
        return recolectarFirmas(codigoFuente, "");
    }

    /** Igual que arriba, indicando el paquete (carpeta) en el que esta el archivo. */
    public static Optional<TablaSimbolosZ.DefinicionClaseExterna> recolectarFirmas(String codigoFuente,
                                                                                   String paquete) {
        if (codigoFuente == null || codigoFuente.trim().isEmpty()) return Optional.empty();

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
            if (huboError[0]) return Optional.empty();

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
                return Optional.empty();
            }
            if (huboError[0]) return Optional.empty();

            // 3. AST
            ASTBuilderZ builder = new ASTBuilderZ();
            NodoAST nodo = builder.visit(tree);
            if (!(nodo instanceof NodoPrograma np)) return Optional.empty();

            // 4. Definicion resumida de la clase
            return Optional.of(ExtractorFirmasZ.extraer(np.clase(), paquete));

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}