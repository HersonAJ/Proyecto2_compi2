package com.example.stack_over_pig.c3d_v2.cuartetas.genericas;


/**
 * goto Lx;
 */
public class Salto extends Cuarteta {

    private final int etiqueta;

    public Salto(int etiqueta) {
        this.etiqueta = etiqueta;
    }

    public int getEtiqueta() {
        return etiqueta;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    goto L").append(etiqueta).append(";\n");
    }
}
