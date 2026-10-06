package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;


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