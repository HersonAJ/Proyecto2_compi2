package com.example.contacto_3xtrat3r3str3.c3d_v2.c.z;

/**
 * Traduce los tipos del lenguaje Z a tipos C.
 *
 * Tipos primitivos:
 *   int      -> int
 *   double   -> double
 *   char     -> char
 *   boolean  -> int    (0/1)
 *   String   -> char*
 *   NombreClase -> struct NombreClase*
 *
 * Un tipo de clase SIEMPRE es un puntero (los objetos viven en el heap).
 */
public final class TipoCZ {

    private TipoCZ() {}

    /** Traduce un tipo base (primitivo o clase) a C, SIN dimensiones. */
    public static String baseAC(String tipoZ, boolean esClase) {
        if (esClase) {
            return "struct " + tipoZ + "*";
        }
        return switch (tipoZ) {
            case "int"      -> "int";
            case "double"   -> "double";
            case "char"     -> "char";
            case "boolean"  -> "int";
            case "String"   -> "char*";
            default         -> "int"; // fallback defensivo
        };
    }

    /** Traduce un tipo base a C, SIN puntero. Útil para structs. */
    public static String baseValorAC(String tipoZ) {
        return switch (tipoZ) {
            case "int"      -> "int";
            case "double"   -> "double";
            case "char"     -> "char";
            case "boolean"  -> "int";
            case "String"   -> "char*";
            default         -> "struct " + tipoZ;
        };
    }

    /** ¿Es un tipo primitivo del lenguaje Z? */
    public static boolean esPrimitivo(String tipoZ) {
        return switch (tipoZ) {
            case "int", "double", "char", "boolean", "String" -> true;
            default -> false;
        };
    }
}