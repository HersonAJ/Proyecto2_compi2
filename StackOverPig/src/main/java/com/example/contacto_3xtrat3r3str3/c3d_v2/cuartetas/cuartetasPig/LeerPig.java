package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Lectura de PigLatin: 'var <<' o '<<'.
 *   numerus   -> %d
 *   decimalis -> %f
 *   littera   -> %c
 *   textum    -> %s
 *   bool      -> %d
 */
public class LeerPig extends Cuarteta {

    private final AccesoMemoria destino;
    private final String tipo;

    public LeerPig(AccesoMemoria destino, String tipo) {
        this.destino = destino;
        this.tipo = tipo;
    }

    public AccesoMemoria getDestino() { return destino; }
    public String getTipo()           { return tipo; }

    private String formato() {
        return switch (tipo) {
            case "decimalis" -> "%f";
            case "littera"   -> "%c";
            case "textum"    -> "%s";
            default          -> "%d";
        };
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    scanf(\"").append(formato()).append("\", ");
        if ("textum".equals(tipo)) {
            // Para cadenas el destino ya es un buffer (char tN[256]).
            destino.aCodigoC(sb);
        } else {
            sb.append('&');
            destino.aCodigoC(sb);
        }
        sb.append(");\n");
    }
}