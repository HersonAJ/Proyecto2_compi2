package com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.GeneradorArchivoC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.GeneradorC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.ParametroC;
import com.example.contacto_3xtrat3r3str3.piglatin.builder.ASTBuilderPig;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoAST;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoPrograma;
import com.example.contacto_3xtrat3r3str3.piglatin.semantica.TablaSimbolosPig;
import com.example.contacto_3xtrat3r3str3.piglatin.semantica.ValidadorSemanticoPig;
import com.example.piglatin.analizador.gramatica.PigLexer;
import com.example.piglatin.analizador.gramatica.PigParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompiladorPig {

    private final Path carpetaRaiz;

    public CompiladorPig(Path carpetaRaiz) {
        this.carpetaRaiz = carpetaRaiz;
    }

    public void compilar(Path rutaPig) throws IOException {
        // 1. Parsear el .pig
        NodoPrograma programa = parsearPig(rutaPig);

        // 2. Semántico (carga imports internamente)
        ValidadorSemanticoPig semantica = new ValidadorSemanticoPig(carpetaRaiz);
        var errores = semantica.analizar(programa);

        if (!errores.isEmpty()) {
            System.err.println("Errores semánticos: " + errores.size());
            for (var e : errores) System.err.println(e);
            return;
        }

        TablaSimbolosPig tabla = semantica.getTabla();

        // 3. Recoger imports
        var importaciones = semantica.getImportaciones();
        List<FuncionC> funcionesImportadas = importaciones.getFuncionesImportadas();
        List<EstructuraC> estructurasImportadas = importaciones.getEstructurasImportadas();

        // 4. Variables globales y main del .pig
        List<ParametroC> variablesGlobales = programa.aVariablesGlobalesC(tabla);
        FuncionC mainC = programa.aMainC(tabla);

        // 5. Combinar
        List<FuncionC> funciones = new ArrayList<>(funcionesImportadas);
        funciones.add(mainC);
        List<EstructuraC> estructuras = new ArrayList<>(estructurasImportadas);

        // 6. Generar C
        String codigoC = new GeneradorC().generar(
                funciones, estructuras, variablesGlobales, false);

        // 7. Escribir y compilar
        new GeneradorArchivoC().generarYCompilar(codigoC);
    }

    private NodoPrograma parsearPig(Path ruta) throws IOException {
        String codigo = Files.readString(ruta);
        PigLexer lexer = new PigLexer(CharStreams.fromString(codigo));
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PigParser parser = new PigParser(tokens);
        PigParser.ProgramaContext tree = parser.programa();
        ASTBuilderPig builder = new ASTBuilderPig();
        NodoAST nodo = builder.visit(tree);
        if (nodo instanceof NodoPrograma p) return p;
        throw new IllegalStateException("AST del .pig inválido");
    }
}