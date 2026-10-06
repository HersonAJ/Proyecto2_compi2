package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;


import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Cuarteta: print x
 *
 * Imprime un valor en consola, con salto de línea al final.
 * El formato de printf se elige según el tipo:
 *   entero -> %d | flotante -> %f | caracter -> %c | cadena -> %s | bool -> %d
 * Ejemplo en .y: imprimir("Hola")
 * C generado:    printf("%s\n", "Hola");
 */
public class Imprimir1 extends Cuarteta {

    private final AccesoMemoria valor;
    private final String tipo;

    public Imprimir1(AccesoMemoria valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public AccesoMemoria getValor() { return valor; }
    public String getTipo()         { return tipo; }

    private String formato() {
        return switch (tipo) {
            case "flotante" -> "%f\\n";
            case "caracter" -> "%c\\n";
            case "cadena"   -> "%s\\n";
            case "bool"     -> "%d\\n";
            default         -> "%d\\n";
        };
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    printf(\"").append(formato()).append("\", ");
        valor.aCodigoC(sb);
        sb.append(");\n");
    }
}