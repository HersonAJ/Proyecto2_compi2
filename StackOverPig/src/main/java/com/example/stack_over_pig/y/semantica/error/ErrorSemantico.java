package com.example.stack_over_pig.y.semantica.error;

public record ErrorSemantico(int linea, int columna, String categoria, String mensaje) {

    @Override
    public String toString() {
        return "[" + categoria + "] línea " + linea + ", columna " + columna + ": " + mensaje;
    }
}