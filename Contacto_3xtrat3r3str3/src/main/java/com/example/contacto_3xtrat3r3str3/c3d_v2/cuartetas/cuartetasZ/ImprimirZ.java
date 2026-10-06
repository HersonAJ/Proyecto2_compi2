package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Escritura del lenguaje Z.
 *   print(x)    -> printf("%d", x);
 *   println(x)  -> printf("%d\n", x);
 * El formato se elige según el tipo del valor.
 */
public class ImprimirZ extends Cuarteta {

    private final AccesoMemoria valor;
    private final String tipo;          // "int", "double", "char", "boolean", "String"
    private final boolean saltoDeLinea;

    public ImprimirZ(AccesoMemoria valor, String tipo, boolean saltoDeLinea) {
        this.valor = valor;
        this.tipo = tipo;
        this.saltoDeLinea = saltoDeLinea;
    }

    public AccesoMemoria getValor()  { return valor; }
    public String getTipo()          { return tipo; }
    public boolean isSaltoDeLinea()  { return saltoDeLinea; }

    private String formato() {
        String base = switch (tipo) {
            case "double"  -> "%f";
            case "char"    -> "%c";
            case "boolean" -> "%d";
            case "String"  -> "%s";
            default        -> "%d";   // int
        };
        return saltoDeLinea ? base + "\\n" : base;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    printf(\"").append(formato()).append("\", ");
        valor.aCodigoC(sb);
        sb.append(");\n");
    }
}