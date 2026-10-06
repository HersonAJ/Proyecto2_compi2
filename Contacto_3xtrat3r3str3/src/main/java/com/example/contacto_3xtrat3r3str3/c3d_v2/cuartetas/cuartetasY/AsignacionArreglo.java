package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Cuarteta: a[i] = y
 *
 * Asigna un valor a la posición 'i' de un arreglo.
 * Ejemplo en .y: numeros[2] = 30
 * C generado:    numeros[2] = 30;
 */
public class AsignacionArreglo extends Cuarteta {

    private final AccesoMemoria base;
    private final AccesoMemoria indice;
    private final AccesoMemoria valor;

    public AsignacionArreglo(AccesoMemoria base, AccesoMemoria indice, AccesoMemoria valor) {
        this.base = base;
        this.indice = indice;
        this.valor = valor;
    }

    public AccesoMemoria getBase()   { return base; }
    public AccesoMemoria getIndice() { return indice; }
    public AccesoMemoria getValor()  { return valor; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        base.aCodigoC(sb);
        sb.append('[');
        indice.aCodigoC(sb);
        sb.append("] = ");
        valor.aCodigoC(sb);
        sb.append(";\n");
    }
}