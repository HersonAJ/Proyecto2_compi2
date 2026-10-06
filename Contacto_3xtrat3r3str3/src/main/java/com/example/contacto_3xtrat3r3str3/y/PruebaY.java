package com.example.contacto_3xtrat3r3str3.y;

import com.example.contacto_3xtrat3r3str3.y.errores.ErrorPosicional;
import com.example.contacto_3xtrat3r3str3.y.errores.ResultadoCompilacionY;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.y.service.ServicioCompilacionY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Clase de prueba del pipeline de compilación de Y?.
 * Lee un archivo .y, lo procesa y muestra el resultado en consola.
 */
public class PruebaY {

    public static void main(String[] args) {
        // Ruta del archivo .y de ejemplo
        Path ruta = Paths.get("src/main/resources/ejemplos/ejemploError.y");

        if (!Files.exists(ruta)) {
            System.err.println("No existe el archivo: " + ruta.toAbsolutePath());
            return;
        }

        String codigo;
        try {
            codigo = Files.readString(ruta);
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            return;
        }

        // Ejecutar el servicio
        ServicioCompilacionY servicio = new ServicioCompilacionY();
        ResultadoCompilacionY resultado = servicio.analizar(codigo);

        // Mostrar resultados
        System.out.println("\n========================================");
        System.out.println("RESULTADO DE LA COMPILACION");
        System.out.println("========================================");
        System.out.println("Exitoso: " + resultado.isExitoso());
        System.out.println();

        if (!resultado.getErroresLexicos().isEmpty()) {
            System.out.println("--- ERRORES LEXICOS ---");
            for (ErrorPosicional e : resultado.getErroresLexicos()) {
                System.out.println("  " + e);
            }
        }

        if (!resultado.getErroresSintacticos().isEmpty()) {
            System.out.println("--- ERRORES SINTACTICOS ---");
            for (ErrorPosicional e : resultado.getErroresSintacticos()) {
                System.out.println("  " + e);
            }
        }

        if (!resultado.getErroresSemanticos().isEmpty()) {
            System.out.println("--- ERRORES SEMANTICOS ---");
            for (ErrorSemantico e : resultado.getErroresSemanticos()) {
                System.out.println("  " + e);
            }
        }

        if (!resultado.getMensajesInternos().isEmpty()) {
            System.out.println("--- MENSAJES INTERNOS ---");
            for (String m : resultado.getMensajesInternos()) {
                System.out.println("  " + m);
            }
        }

        if (resultado.isExitoso() && resultado.getPrograma() != null) {
            System.out.println("--- AST CONSTRUIDO ---");
            System.out.println("  Estructuras: " + resultado.getPrograma().estructuras().size());
            System.out.println("  Funciones: " + resultado.getPrograma().funciones().size());
        }

        System.out.println("\n========================================");
    }
}