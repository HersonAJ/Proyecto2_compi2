package com.example.contacto_3xtrat3r3str3.y.analizador;

import java.util.ArrayList;
import java.util.List;

public class PreprocesadorIndentacion {

    private final List<Integer> pilaIndentacion = new ArrayList<>();

    public String preprocesar(String codigoFuente) {
        pilaIndentacion.clear();
        pilaIndentacion.add(0);

        StringBuilder salida = new StringBuilder();

        // separar por saltos de linea
        String[] lineas = codigoFuente.split("\\r?\\n", -1);

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];

            // lineas vacias: emitir solo el salto real para conservar el conteo de lineas
            if (linea.trim().isEmpty()) {
                salida.append("\n");
                continue;
            }

            // lineas que solo son comentarios: emitir solo el salto real
            if (esSoloComentario(linea)) {
                salida.append("\n");
                continue;
            }

            // calcular el nivel de indentacion (conteo de tabs)
            int nivelActual = contarTabsIniciales(linea);

            // validar que no haya espacios al inicio solo tabs
            validarSinEspaciosIniciales(linea);

            // comparar con el tope de la pila
            int nivelTope = pilaIndentacion.get(pilaIndentacion.size() - 1);

            if (nivelActual > nivelTope) {
                // Aumento de indentacion: UN SOLO INDENT, guardamos el nuevo nivel.
                salida.append("<INDENT>");
                pilaIndentacion.add(nivelActual);
            } else if (nivelActual < nivelTope) {
                // Disminucion: un DEDENT por cada nivel que se cierra.
                while (pilaIndentacion.get(pilaIndentacion.size() - 1) > nivelActual) {
                    salida.append("<DEDENT>");
                    pilaIndentacion.remove(pilaIndentacion.size() - 1);
                }
                // Si el nivel actual no coincide con ningun nivel abierto,
                // es un error de indentacion (nivel no alineado).
                if (pilaIndentacion.get(pilaIndentacion.size() - 1) != nivelActual) {
                    throw new RuntimeException(
                            "Indentacion invalida: el nivel " + nivelActual
                                    + " no coincide con ningun nivel abierto");
                }
            }

            // quitar la indentacion inicial y agregar el contenido
            String contenido = linea.substring(nivelActual).stripTrailing();
            salida.append(contenido).append("<NEWLINE>");

            // salto real para conservar el conteo de lineas de ANTLR
            salida.append("\n");
        }

        // al final del archivo, cerrar todos los niveles abiertos
        while (pilaIndentacion.size() > 1) {
            salida.append("<DEDENT>");
            pilaIndentacion.remove(pilaIndentacion.size() - 1);
        }

        return salida.toString();
    }

    //contador de tabs al inicio
    private int contarTabsIniciales(String linea) {
        int contador = 0;
        while (contador < linea.length() && linea.charAt(contador) == '\t') {
            contador++;
        }
        return contador;
    }

    //verificacion de espacios en blanco antes del primer tab
    private void validarSinEspaciosIniciales(String linea) {
        int i = 0;

        while (i < linea.length() && linea.charAt(i) == '\t') {
            i++;
        }

        //si despues de los tabs viene un espacio, es una mezcla invalida
        if (i < linea.length() && linea.charAt(i) == ' ') {
            throw new RuntimeException(
                    "Indentacion invalida: se encontro espacio despues de tab. " +
                            "Solo se permiten tabulaciones al inicio de linea."
            );
        }
    }

    //determinar si la linea tiene unicamente comentarios
    private boolean esSoloComentario(String linea) {
        String sinIndentacion = linea.stripLeading();
        return sinIndentacion.startsWith("//") || sinIndentacion.startsWith("/*");
    }
}
