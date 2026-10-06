package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;


/**
 * Promociones de tipo para operaciones binarias en .y.
 * Decide el tipo resultado y las conversiones necesarias.
 *
 * Jerarquía: caracter < entero < flotante.   bool se trata como entero.
 *
 * Ejemplo: entero + flotante  →  resultado flotante, se convierte el entero.
 */
public final class PromocionTipos {

    private PromocionTipos() {}

    /**
     * Resultado de la promoción.
     *   tipoResultado: tipo final de la operación.
     *   conversionIzq / conversionDer: tipo al que convertir cada operando (null si no aplica).
     */
    public record Resultado(String tipoResultado, String conversionIzq, String conversionDer) {}

    public static Resultado promover(String tipoIzq, String tipoDer) {
        // Normalizamos bool a entero para el cálculo de promociones.
        String izq = normalizar(tipoIzq);
        String der = normalizar(tipoDer);

        // Caso ideal: mismos tipos, sin conversiones.
        if (izq.equals(der)) {
            return new Resultado(tipoResultadoBase(izq), null, null);
        }

        // Si alguno es flotante, el resultado es flotante.
        if (izq.equals("flotante") || der.equals("flotante")) {
            String convIzq = izq.equals("flotante") ? null : "flotante";
            String convDer = der.equals("flotante") ? null : "flotante";
            return new Resultado("flotante", convIzq, convDer);
        }

        // Si alguno es entero (y el otro caracter), el resultado es entero.
        if (izq.equals("entero") || der.equals("entero")) {
            String convIzq = izq.equals("entero") ? null : "entero";
            String convDer = der.equals("entero") ? null : "entero";
            return new Resultado("entero", convIzq, convDer);
        }

        // Solo quedan caracter + caracter  o casos raros.
        return new Resultado(tipoResultadoBase(izq), null, null);
    }

    private static String normalizar(String tipo) {
        return "bool".equals(tipo) ? "entero" : tipo;
    }

    private static String tipoResultadoBase(String tipo) {
        return "bool".equals(tipo) ? "entero" : tipo;
    }
}