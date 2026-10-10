package com.example.stack_over_pig.c3d_v2.cuartetas.genericas;

// en com.example.stack_over_pig.c3d_v2.cuartetas.genericas
public class AccesoDireccion extends AccesoMemoria {
    private final AccesoMemoria base;

    public AccesoDireccion(AccesoMemoria base) {
        this.base = base;
    }

    @Override
    public String getTipo() {
        // El tipo es el mismo pero como puntero.
        return base.getTipo() + "*";
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append('&');
        base.aCodigoC(sb);
    }
}