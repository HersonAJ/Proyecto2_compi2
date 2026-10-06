package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;


import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Cuarteta: read -> x
 *
 * Lee un valor de consola y lo guarda en el destino.
 * El formato de scanf se elige según el tipo. Los tipos primitivos
 * necesitan '&' (dirección); las cadenas no, porque ya son punteros.
 * Ejemplo en .y: leer()
 * C generado:    scanf("%d", &x);
 */
public class Leer extends Cuarteta {

    private final AccesoMemoria destino;
    private final String tipo;

    public Leer(AccesoMemoria destino, String tipo) {
        this.destino = destino;
        this.tipo = tipo;
    }

    public AccesoMemoria getDestino() { return destino; }
    public String getTipo()           { return tipo; }

    private String formato() {
        return switch (tipo) {
            case "flotante" -> "%f";
            case "caracter" -> "%c";
            case "cadena"   -> "%s";
            default         -> "%d";
        };
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    scanf(\"").append(formato()).append("\", ");
        if ("cadena".equals(tipo)) {
            destino.aCodigoC(sb);
        } else {
            sb.append('&');
            destino.aCodigoC(sb);
        }
        sb.append(");\n");
    }
}