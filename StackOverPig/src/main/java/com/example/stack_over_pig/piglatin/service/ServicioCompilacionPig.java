package com.example.stack_over_pig.piglatin.service;

import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.ContadorIds;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.Cuarteta;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.stack_over_pig.piglatin.builder.ASTBuilderPig;
import com.example.stack_over_pig.piglatin.nodo.NodoAST;
import com.example.stack_over_pig.piglatin.nodo.NodoExpr;
import com.example.stack_over_pig.piglatin.nodo.NodoPrograma;
import com.example.stack_over_pig.piglatin.nodo.NodoSentencia;
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;
import com.example.stack_over_pig.piglatin.semantica.ValidadorSemanticoPig;
import com.example.stack_over_pig.riscv.DisposicionObjetos;
import com.example.stack_over_pig.riscv.EmisorRiscV;
import com.example.stack_over_pig.riscv.FuncionRiscV;
import com.example.stack_over_pig.riscv.TablaMetodosVirtuales;
import com.example.stack_over_pig.riscv.piglatin.GeneradorCuartetasPig;
import com.example.stack_over_pig.riscv.y.GeneradorCuartetasY;
import com.example.stack_over_pig.riscv.zetariano.GeneradorCuartetasZ;
import com.example.stack_over_pig.y.errores.ErrorPosicional;
import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;
import com.example.piglatin.analizador.gramatica.PigLexer;
import com.example.piglatin.analizador.gramatica.PigParser;
import com.example.stack_over_pig.zetariano.nodo.NodoClase;
import com.example.stack_over_pig.zetariano.nodo.NodoConstructor;
import com.example.stack_over_pig.zetariano.nodo.NodoMetodo;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import org.antlr.v4.runtime.*;
import com.example.stack_over_pig.y.ast.NodoFuncion;
import com.example.stack_over_pig.y.ast.NodoProgramaY;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            ContadorIds.reiniciarEtiquetasGlobales();
            TablaSimbolosPig tabla = ultimoValidador.getTabla();

            // 1. Cuartetas del .pig (main)
            GestorCodigoIntermedio gestorPig = new GestorCodigoIntermedio();
            GeneradorCuartetasPig generadorPig = new GeneradorCuartetasPig(gestorPig, tabla);
            resultado.getPrograma().aceptar(generadorPig);
            List<Cuarteta> cuartetasMain = gestorPig.getCuartetas();
            List<FuncionRiscV> funciones = new ArrayList<>();
            System.out.println("=== DEBUG clasesImportadas ===");
            for (NodoClase c : ultimoValidador.getImportaciones().getClasesImportadas()) {
                System.out.println("  clase: " + c.nombre() + " (super: " + c.superclase() + ")");
            }
            System.out.println("=== DEBUG tablasZImportadas keys ===");
            for (String k : ultimoValidador.getImportaciones().getTablasZImportadas().keySet()) {
                System.out.println("  tabla: " + k);
            }
            for (NodoClase clase : ultimoValidador.getImportaciones().getClasesImportadas()) {
                TablaSimbolosZ tablaZ = obtenerTablaZ(clase);
                if (tablaZ == null) continue;
                if (clase.constructores().isEmpty()) {
                    funciones.add(new GeneradorCuartetasZ(tablaZ, clase.nombre()).generarConstructorPorDefecto());
                }
                for (NodoConstructor c : clase.constructores()) {
                    funciones.add(new GeneradorCuartetasZ(tablaZ, clase.nombre()).generarConstructor(c));
                }
                for (NodoMetodo m : clase.metodos()) {
                    if (clase.nombre().equals("CuentaCorriente")) {
                        System.out.println("=== DEBUG CuentaCorriente: métodos de Cuenta ===");
                        System.out.println("  getMetodosDe(\"CuentaCorriente\", \"ajustarSaldo\") = " +
                                tablaZ.getMetodosDe("CuentaCorriente", "ajustarSaldo").size());
                        System.out.println("  getMetodosDe(\"Cuenta\", \"ajustarSaldo\") = " +
                                tablaZ.getMetodosDe("Cuenta", "ajustarSaldo").size());
                        System.out.println("  buscarClaseExterna(\"Cuenta\").isPresent() = " +
                                tablaZ.buscarClaseExterna("Cuenta").isPresent());
                    }
                    funciones.add(new GeneradorCuartetasZ(tablaZ, clase.nombre()).generarMetodo(m));
                }
            }
            // 2.b Funciones de los .y importados: una FuncionRiscV por cada función Y.
            var importaciones = ultimoValidador.getImportaciones();
            if (importaciones != null) {
                List<NodoProgramaY.Programa> programasY = importaciones.getProgramasYImportados();
                List<com.example.stack_over_pig.y.semantica.TablaSimbolos> tablasY = importaciones.getTablasYImportadas();
                for (int i = 0; i < programasY.size(); i++) {
                    NodoProgramaY.Programa progY = programasY.get(i);
                    var tablaY = tablasY.get(i);
                    for (NodoFuncion nf : progY.funciones()) {
                        if (nf instanceof NodoFuncion.Funcion f) {
                            funciones.add(new GeneradorCuartetasY(tablaY, tabla).generarFuncion(f));
                        }
                    }
                }
            }

            // 3. Calcular tamaño del heap (instancias de objetos en el .pig)
            int tamanoHeap = calcularTamanoHeap(resultado.getPrograma(), tabla);

            // 4. Emitir
            EmisorRiscV emisor = new EmisorRiscV(tabla, tamanoHeap);
            TablaMetodosVirtuales virtuales =
                    TablaMetodosVirtuales.construir(ultimoValidador.getImportaciones().getClasesImportadas());
            String codigoRiscV = emisor.emitir(cuartetasMain, funciones, virtuales);

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

    private int calcularTamanoHeap(NodoPrograma programa, TablaSimbolosPig tabla) {
        // Contar instancias de cada clase en el .pig
        Map<String, Integer> instancias = new LinkedHashMap<>();
        contarInstanciasEnSentencias(programa.variablesGlobales(), instancias);
        contarInstanciasEnSentencias(programa.cuerpoMain(), instancias);

        int total = 0;
        for (var e : instancias.entrySet()) {
            String clase = e.getKey();
            int num = e.getValue();
            var def = tabla.buscarClase(clase);
            if (def.isPresent()) {
                //int tamanoClase = 8 * def.get().atributos().size();
                int tamanoClase = new DisposicionObjetos(tabla).tamano(clase);
                total += num * tamanoClase;
            }
        }
        //return total;
        return Math.max(total + 1024, 65536);
    }

    private void contarInstanciasEnSentencias(List<NodoSentencia> sentencias,
                                              Map<String, Integer> instancias) {
        for (NodoSentencia s : sentencias) {
            contarInstanciasEnSentencia(s, instancias);
        }
    }

    private void contarInstanciasEnSentencia(NodoSentencia s, Map<String, Integer> instancias) {
        switch (s.tipoNodo()) {
            case DECLARACION_VARIABLE -> {
                NodoSentencia.DeclaracionVariable d = (NodoSentencia.DeclaracionVariable) s;
                if (d.inicializacion() != null) contarInstanciasEnExpr(d.inicializacion(), instancias);
            }
            case ASIGNACION -> {
                NodoSentencia.Asignacion a = (NodoSentencia.Asignacion) s;
                contarInstanciasEnExpr(a.valor(), instancias);
            }
            case CONDICIONAL -> {
                NodoSentencia.Condicional c = (NodoSentencia.Condicional) s;
                contarInstanciasEnExpr(c.condicion(), instancias);
                contarInstanciasEnSentencias(c.cuerpoSi(), instancias);
                for (NodoSentencia.RamaAliter rama : c.ramasAliter()) {
                    contarInstanciasEnSentencias(rama.cuerpo(), instancias);
                }
                if (c.cuerpoAliter() != null) {
                    contarInstanciasEnSentencias(c.cuerpoAliter(), instancias);
                }
            }
            case CICLO_DUM -> {
                NodoSentencia.CicloDum c = (NodoSentencia.CicloDum) s;
                contarInstanciasEnSentencias(c.cuerpo(), instancias);
            }
            case CICLO_FACERE -> {
                NodoSentencia.CicloFacere c = (NodoSentencia.CicloFacere) s;
                contarInstanciasEnSentencias(c.cuerpo(), instancias);
            }
            case CICLO_PER -> {
                NodoSentencia.CicloPer c = (NodoSentencia.CicloPer) s;
                contarInstanciasEnSentencias(c.cuerpo(), instancias);
            }
            case ESCRITURA -> {
                NodoSentencia.Escritura e = (NodoSentencia.Escritura) s;
                for (NodoExpr v : e.valores()) contarInstanciasEnExpr(v, instancias);
            }
            case LLAMADA_FUNCION_SENTENCIA -> {
                NodoSentencia.LlamadaFuncionSentencia l = (NodoSentencia.LlamadaFuncionSentencia) s;
                contarInstanciasEnExpr(l.llamada(), instancias);
            }
            case LLAMADA_METODO_SENTENCIA -> {
                NodoSentencia.LlamadaMetodoSentencia l = (NodoSentencia.LlamadaMetodoSentencia) s;
                contarInstanciasEnExpr(l.llamada(), instancias);
            }
            default -> {}
        }
    }

    private void contarInstanciasEnExpr(NodoExpr e, Map<String, Integer> instancias) {
        if (e == null) return;
        switch (e.tipoNodo()) {
            case INSTANCIA_OBJETO -> {
                NodoExpr.InstanciaObjeto inst = (NodoExpr.InstanciaObjeto) e;
                instancias.merge(inst.tipoClase(), 1, Integer::sum);
                for (NodoExpr arg : inst.argumentos()) contarInstanciasEnExpr(arg, instancias);
            }
            case BINARIA -> {
                NodoExpr.Binaria b = (NodoExpr.Binaria) e;
                contarInstanciasEnExpr(b.izquierda(), instancias);
                contarInstanciasEnExpr(b.derecha(), instancias);
            }
            case UNARIA -> contarInstanciasEnExpr(((NodoExpr.Unaria) e).operando(), instancias);
            case LLAMADA_FUNCION -> {
                for (NodoExpr arg : ((NodoExpr.LlamadaFuncion) e).argumentos())
                    contarInstanciasEnExpr(arg, instancias);
            }
            case LLAMADA_METODO -> {
                NodoExpr.LlamadaMetodo l = (NodoExpr.LlamadaMetodo) e;
                contarInstanciasEnExpr(l.objeto(), instancias);
                for (NodoExpr arg : l.argumentos()) contarInstanciasEnExpr(arg, instancias);
            }
            default -> {}
        }
    }

    private TablaSimbolosZ obtenerTablaZ(NodoClase clase) {
        return ultimoValidador.getImportaciones()
                .getTablasZImportadas()
                .get(clase.nombre());
    }
}