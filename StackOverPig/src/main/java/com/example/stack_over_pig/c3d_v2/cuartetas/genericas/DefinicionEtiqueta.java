package com.example.stack_over_pig.c3d_v2.cuartetas.genericas;


/**
 * Lx:
 */
public class DefinicionEtiqueta extends Cuarteta {

    private final int etiqueta;

    public DefinicionEtiqueta(int etiqueta) {
        this.etiqueta = etiqueta;
    }

    public int getEtiqueta() {
        return etiqueta;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("L").append(etiqueta).append(":\n");
    }
}