package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig;

/**
 * Promoción de tipos para PigLatin.
 * Jerarquía: littera < numerus < decimalis
 * bool se trata como numerus.
 * textum solo admite == y != (strcmp).
 */
public final class PromocionTiposPig {

    private PromocionTiposPig() {}

    public record Resultado(String tipoResultado, String conversionIzq, String conversionDer) {}

    public static Resultado promover(String tipoIzq, String tipoDer) {
        String izq = normalizar(tipoIzq);
        String der = normalizar(tipoDer);

        if (izq.equals(der)) {
            return new Resultado(izq, null, null);
        }
        if (izq.equals("decimalis") || der.equals("decimalis")) {
            String cIzq = izq.equals("decimalis") ? null : "decimalis";
            String cDer = der.equals("decimalis") ? null : "decimalis";
            return new Resultado("decimalis", cIzq, cDer);
        }
        if (izq.equals("numerus") || der.equals("numerus")) {
            String cIzq = izq.equals("numerus") ? null : "numerus";
            String cDer = der.equals("numerus") ? null : "numerus";
            return new Resultado("numerus", cIzq, cDer);
        }
        return new Resultado(izq, null, null);
    }

    private static String normalizar(String tipo) {
        return "bool".equals(tipo) ? "numerus" : tipo;
    }
}