package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Cuarteta: p3 = p1
 *
 * Copia completa de una estructura a otra.
 * Semánticamente es una copia campo a campo; en C es un simple '='.
 * Ejemplo en .y: p3 = p1
 * C generado:    p3 = p1;
 */
public class AsignacionEstructura extends Cuarteta {

    private final AccesoMemoria destino;
    private final AccesoMemoria fuente;

    public AsignacionEstructura(AccesoMemoria destino, AccesoMemoria fuente) {
        this.destino = destino;
        this.fuente = fuente;
    }

    public AccesoMemoria getDestino() { return destino; }
    public AccesoMemoria getFuente()  { return fuente; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = ");
        fuente.aCodigoC(sb);
        sb.append(";\n");
    }
}