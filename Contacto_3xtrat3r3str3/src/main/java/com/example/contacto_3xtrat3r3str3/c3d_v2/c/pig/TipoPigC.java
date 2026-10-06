package com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig;

/**
 * Traduce tipos de PigLatin a tipos C.
 *   numerus   -> int
 *   decimalis -> float
 *   littera   -> char
 *   textum    -> char*
 *   bool      -> int
 *   NombreStruct / NombreClase -> struct Nombre*  (los objetos viven en heap)
 */
public final class TipoPigC {

    private TipoPigC() {}

    public static boolean esPrimitivo(String tipoPig) {
        return switch (tipoPig) {
            case "numerus", "decimalis", "littera", "textum", "bool" -> true;
            default -> false;
        };
    }

    public static String baseValorAC(String tipoPig) {
        return switch (tipoPig) {
            case "numerus"   -> "int";
            case "decimalis" -> "float";
            case "littera"   -> "char";
            case "textum"    -> "char*";
            case "bool"      -> "int";
            default          -> "struct " + tipoPig;
        };
    }

    /** Para variables de tipo struct (por valor en stack) o clase (puntero). */
    public static String baseAC(String tipoPig, boolean esObjeto) {
        if (esPrimitivo(tipoPig)) {
            return baseValorAC(tipoPig);
        }
        return esObjeto ? "struct " + tipoPig + "*" : "struct " + tipoPig;
    }
}