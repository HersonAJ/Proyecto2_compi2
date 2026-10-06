package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Escritura de PigLatin: '>> expr;' -> printf sin salto de línea.
 *   numerus   -> %d
 *   decimalis -> %f
 *   littera   -> %c
 *   textum    -> %s
 *   bool      -> %d
 */
public class ImprimirPig extends Cuarteta {

    private final AccesoMemoria valor;
    private final String tipo;

    public ImprimirPig(AccesoMemoria valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public AccesoMemoria getValor() { return valor; }
    public String getTipo()         { return tipo; }

    private String formato() {
        return switch (tipo) {
            case "decimalis" -> "%f";
            case "littera"   -> "%c";
            case "textum"    -> "%s";
            case "bool"      -> "%d";
            case "numerus"   -> "%d";
            default          -> "%p";
        };
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    printf(\"").append(formato()).append("\", ");
        valor.aCodigoC(sb);
        sb.append(");\n");
    }
}