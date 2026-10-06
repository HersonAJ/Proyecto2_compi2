package com.example.stack_over_pig.y.analizador;

import java.util.ArrayList;
import java.util.List;

public class PreprocesadorIndentacion {

    // Un tab equivale a 4 espacios.
    private static final int ESPACIOS_POR_TAB = 4;

    private final List<Integer> pilaIndentacion = new ArrayList<>();

    public String preprocesar(String codigoFuente) {
        pilaIndentacion.clear();
        pilaIndentacion.add(0);

        StringBuilder salida = new StringBuilder();

        // Separar por saltos de linea.
        String[] lineas = codigoFuente.split("\\r?\\n", -1);

        for (String linea : lineas) {

            // Lineas vacias: emitir solamente el salto real
            // para conservar el conteo de lineas.
            if (linea.trim().isEmpty()) {
                salida.append("\n");
                continue;
            }

            // Lineas que solamente contienen comentarios:
            // emitir solamente el salto real.
            if (esSoloComentario(linea)) {
                salida.append("\n");
                continue;
            }

            // Calcular el nivel semantico de indentación
            // y la cantidad exacta de caracteres iniciales.
            int[] indentacion = calcularIndentacion(linea);

            int nivelActual = indentacion[0];
            int caracteresAQuitar = indentacion[1];

            // Nivel actualmente abierto.
            int nivelTope = pilaIndentacion.get(
                    pilaIndentacion.size() - 1
            );

            // Aumento de indentacion.
            if (nivelActual > nivelTope) {

                // Un cambio hacia una indentacion mayor
                // representa un único bloque nuevo.
                salida.append("<INDENT>");

                pilaIndentacion.add(nivelActual);

            }

            // Disminucion de indentacion.
            else if (nivelActual < nivelTope) {

                // Cierra todos los niveles necesarios.
                while (
                        pilaIndentacion.size() > 1
                                && pilaIndentacion.get(
                                pilaIndentacion.size() - 1
                        ) > nivelActual
                ) {
                    salida.append("<DEDENT>");

                    pilaIndentacion.remove(
                            pilaIndentacion.size() - 1
                    );
                }

                // Si despues de cerrar niveles no encontramos exactamente el nivel actual, la indentacion no corresponde a ningún bloque abierto.
                if (
                        pilaIndentacion.get(
                                pilaIndentacion.size() - 1
                        ) != nivelActual
                ) {
                    throw new RuntimeException(
                            "Indentacion invalida: el nivel "
                                    + nivelActual
                                    + " no coincide con ningun nivel abierto."
                    );
                }
            }

            // Quitar unicamente la indentación inicial.
            String contenido = linea
                    .substring(caracteresAQuitar)
                    .stripTrailing();

            salida.append(contenido)
                    .append("<NEWLINE>");

            // Mantener el salto físico para conservar
            // correctamente el conteo de líneas de ANTLR.
            salida.append("\n");
        }

        // Al finalizar el archivo, cerrar todos los niveles abiertos.
        while (pilaIndentacion.size() > 1) {

            salida.append("<DEDENT>");

            pilaIndentacion.remove(
                    pilaIndentacion.size() - 1
            );
        }

        return salida.toString();
    }

    private int[] calcularIndentacion(String linea) {

        int nivel = 0;
        int caracteres = 0;

        while (caracteres < linea.length()) {

            char caracter = linea.charAt(caracteres);

            if (caracter == ' ') {
                // Cada espacio representa una unidad.
                nivel++;
                caracteres++;

            } else if (caracter == '\t') {
                // Cada tab equivale a 4 espacios.
                nivel += ESPACIOS_POR_TAB;
                caracteres++;

            } else {
                break;
            }
        }

        return new int[]{nivel, caracteres};
    }

    // Determina si la línea contiene unicamente un comentario
    private boolean esSoloComentario(String linea) {

        String sinIndentacion = linea.stripLeading();

        return sinIndentacion.startsWith("//")
                || sinIndentacion.startsWith("/*");
    }
}