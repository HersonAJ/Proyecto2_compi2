package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

import java.util.List;

/**
 * Cuarteta: switch (opcion) { case 1: ... case 2: ... default: ... }
 *
 * Traduce un 'switch' de .z a un switch de C con sus casos y default.
 * Ejemplo en .z: switch (opcion) { case 1: ... break; default: ... }
 * C generado:
 *     switch (opcion) {
 *         case 1:
 *             ...
 *         default:
 *             ...
 *     }
 */
public class Switch1 extends Cuarteta {

    /** Un case del switch: valor literal + cuerpo ya traducido. */
    public record Caso(AccesoMemoria valor, List<Cuarteta> cuerpo) {}

    private final AccesoMemoria expresion;
    private final List<Caso> casos;
    private final List<Cuarteta> cuerpoDefault;

    public Switch1(AccesoMemoria expresion,
                   List<Caso> casos,
                   List<Cuarteta> cuerpoDefault) {
        this.expresion = expresion;
        this.casos = casos;
        this.cuerpoDefault = cuerpoDefault;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    switch (");
        expresion.aCodigoC(sb);
        sb.append(") {\n");

        for (Caso c : casos) {
            sb.append("        case ");
            c.valor().aCodigoC(sb);
            sb.append(":\n");
            for (Cuarteta cuarteta : c.cuerpo()) {
                escribirIndentado(sb, cuarteta, "            ");
            }
        }

        if (cuerpoDefault != null) {
            sb.append("        default:\n");
            for (Cuarteta cuarteta : cuerpoDefault) {
                escribirIndentado(sb, cuarteta, "            ");
            }
        }

        sb.append("    }\n");
    }

    /** Escribe una cuarteta con indentación extra dentro del case. */
    private void escribirIndentado(StringBuilder sb, Cuarteta cuarteta, String prefijo) {
        StringBuilder sub = new StringBuilder();
        cuarteta.aCodigoC(sub);
        for (String linea : sub.toString().split("\n", -1)) {
            if (!linea.isEmpty()) {
                sb.append(prefijo).append(linea).append('\n');
            }
        }
    }
}