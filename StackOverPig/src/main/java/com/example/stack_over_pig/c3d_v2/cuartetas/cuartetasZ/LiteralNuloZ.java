package com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ;

import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.AccesoMemoria;

/**
 * Literal 'null' del lenguaje Z.
 * En C se escribe como NULL.
 */
public class LiteralNuloZ extends AccesoMemoria {

    public LiteralNuloZ() {
    }

    @Override
    public String getTipo() {
        return "null";
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("NULL");
    }
}