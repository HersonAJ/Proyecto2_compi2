package com.example.contacto_3xtrat3r3str3.c3d_v2.c;

import com.example.contacto_3xtrat3r3str3.y.semantica.TablaSimbolos.SimboloVariable;

/**
 * Traduce los tipos del lenguaje Y a tipos C.
 * También decide si un símbolo es puntero (arreglo, struct pasado por referencia, etc.).
 */
public final class TipoC {

    private TipoC() {}

    /** Traduce un tipo primitivo del lenguaje Y a su equivalente en C. */
    public static String primitivoAC(String tipoY) {
        return switch (tipoY) {
            case "entero"   -> "int";
            case "flotante" -> "float";
            case "caracter" -> "char";
            case "cadena"   -> "char*";
            case "bool"     -> "int"; // bool en C = int (0/1)
            default         -> "int"; // fallback defensivo
        };
    }

    /** Traduce un SimboloVariable a su tipo C, considerando arreglos y structs. */
    public static String aTipoC(SimboloVariable s) {
        if (s.esArreglo()) {
            // En C los arreglos locales se declaran tipo nombre[N], pero
            // cuando se usan como operando el tipo es puntero.
            return primitivoAC(s.tipo()) + "*";
        }
        if (s.esEstructura()) {
            return structAC(s.tipoEstructura());
        }
        return primitivoAC(s.tipo());
    }

    /** Nombre C del struct asociado a una estructura definida por el usuario. */
    public static String structAC(String nombreEstructura) {
        return "struct " + nombreEstructura;
    }
}
