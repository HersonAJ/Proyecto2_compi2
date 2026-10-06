package com.example.contacto_3xtrat3r3str3.piglatin;

import com.example.contacto_3xtrat3r3str3.piglatin.service.ResultadoCompilacionPig;
import com.example.contacto_3xtrat3r3str3.piglatin.service.ServicioCompilacionPig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PruebaPig {

    public static void main(String[] args) {
        Path ruta = Paths.get("src/main/resources/ejemplos/ejemplo.pig");
        Path carpetaRaiz = Paths.get("src/main/resources");

        if (!Files.exists(ruta)) {
            System.err.println("No existe: " + ruta.toAbsolutePath());
            return;
        }

        String codigo;
        try {
            codigo = Files.readString(ruta);
        } catch (IOException e) {
            System.err.println("Error al leer: " + e.getMessage());
            return;
        }

        ServicioCompilacionPig servicio = new ServicioCompilacionPig();
        ResultadoCompilacionPig resultado = servicio.analizar(codigo, carpetaRaiz);

        System.out.println("\n========================================");
        System.out.println("RESULTADO DE LA COMPILACION");
        System.out.println("========================================");
        System.out.println("Exitoso: " + resultado.isExitoso());

        if (!resultado.getErroresLexicos().isEmpty()) {
            System.out.println("--- ERRORES LEXICOS ---");
            resultado.getErroresLexicos().forEach(System.out::println);
        }
        if (!resultado.getErroresSintacticos().isEmpty()) {
            System.out.println("--- ERRORES SINTACTICOS ---");
            resultado.getErroresSintacticos().forEach(System.out::println);
        }
        if (!resultado.getErroresSemanticos().isEmpty()) {
            System.out.println("--- ERRORES SEMANTICOS ---");
            resultado.getErroresSemanticos().forEach(System.out::println);
        }
        if (!resultado.getMensajesInternos().isEmpty()) {
            System.out.println("--- MENSAJES INTERNOS ---");
            resultado.getMensajesInternos().forEach(System.out::println);
        }

        System.out.println("========================================");
    }
}