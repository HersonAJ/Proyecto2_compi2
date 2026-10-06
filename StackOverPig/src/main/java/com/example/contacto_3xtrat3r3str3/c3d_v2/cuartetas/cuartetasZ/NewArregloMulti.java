package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

import java.util.List;

/**
 * Cuarteta: t0 = new int[a][b]
 *
 * Reserva una matriz 2D: un arreglo de punteros y luego cada fila.
 * Ejemplo en .z: int[][] m = new int[3][3]
 * C generado:
 *     int** t0 = malloc(3 * sizeof(int*));
 *     for (int i = 0; i < 3; i++) {
 *         t0[i] = malloc(3 * sizeof(int));
 *     }
 */
public class NewArregloMulti extends Cuarteta {

    private final AccesoMemoria destino;      // el temporal que recibe el arreglo
    private final String tipoElementoC;       // "int", "double", "struct Persona"
    private final List<AccesoMemoria> tamanos;

    public NewArregloMulti(AccesoMemoria destino,
                           String tipoElementoC,
                           List<AccesoMemoria> tamanos) {
        this.destino = destino;
        this.tipoElementoC = tipoElementoC;
        this.tamanos = tamanos;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        int n = tamanos.size();
        if (n < 2) {
            throw new IllegalStateException(
                    "NewArregloMulti1 requiere al menos 2 dimensiones");
        }

        String tipoPuntero = tipoElementoC + "*";

        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = malloc(");
        tamanos.get(0).aCodigoC(sb);
        sb.append(" * sizeof(").append(tipoPuntero).append("));\n");

        sb.append("    for (int i = 0; i < ");
        tamanos.get(0).aCodigoC(sb);
        sb.append("; i++) {\n");

        sb.append("        ");
        destino.aCodigoC(sb);
        sb.append("[i] = malloc(");
        tamanos.get(1).aCodigoC(sb);
        sb.append(" * sizeof(").append(tipoElementoC).append("));\n");

        sb.append("    }\n");
    }
}