package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Creación de arreglo con 'new':
 *   new int[5]  ->  int* t0 = malloc(5 * sizeof(int));
 * Por ahora solo 1D.
 */
public class NewArreglo extends Cuarteta {

    private final AccesoMemoria destino;    // temporal que recibe el puntero
    private final String tipoC;             // tipo del elemento en C ("int", "double", ...)
    private final AccesoMemoria tamano;     // expresion con el tamaño

    public NewArreglo(AccesoMemoria destino, String tipoC, AccesoMemoria tamano) {
        this.destino = destino;
        this.tipoC = tipoC;
        this.tamano = tamano;
    }

    public AccesoMemoria getDestino() { return destino; }
    public String getTipoC()          { return tipoC; }
    public AccesoMemoria getTamano()  { return tamano; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = malloc(");
        tamano.aCodigoC(sb);
        sb.append(" * sizeof(").append(tipoC).append("));\n");
    }
}
