package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;


/**
 * Promoción de tipos para el lenguaje Z.
 * Jerarquía: char < int < double
 * boolean se trata como int.
 * String no promociona (solo admite == y !=).
 */
public final class PromocionTiposZ {

    private PromocionTiposZ() {}

    public record Resultado(String tipoResultado, String conversionIzq, String conversionDer) {}

    public static Resultado promover(String tipoIzq, String tipoDer) {
        String izq = normalizar(tipoIzq);
        String der = normalizar(tipoDer);

        if (izq.equals(der)) {
            return new Resultado(izq, null, null);
        }

        // double gana a todo
        if (izq.equals("double") || der.equals("double")) {
            String convIzq = izq.equals("double") ? null : "double";
            String convDer = der.equals("double") ? null : "double";
            return new Resultado("double", convIzq, convDer);
        }

        // int gana a char
        if (izq.equals("int") || der.equals("int")) {
            String convIzq = izq.equals("int") ? null : "int";
            String convDer = der.equals("int") ? null : "int";
            return new Resultado("int", convIzq, convDer);
        }

        // char + char (ya cubierto arriba)
        return new Resultado(izq, null, null);
    }

    private static String normalizar(String tipo) {
        return "boolean".equals(tipo) ? "int" : tipo;
    }
}