package com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ;

import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.AccesoMemoria;

/**
 * Operando: acceso a un campo de un objeto (p.campo).
 * Los objetos son punteros al heap; el emisor RISC-V suma el offset del campo al puntero.
 */
public class AccesoAtributo1 extends AccesoMemoria {

    private final AccesoMemoria base;
    private final String campo;
    private final boolean porPuntero;
    private final String tipoCampo;

    public AccesoAtributo1(AccesoMemoria base, String campo, boolean porPuntero, String tipoCampo) {
        this.base = base;
        this.campo = campo;
        this.porPuntero = porPuntero;
        this.tipoCampo = tipoCampo;
    }

    public AccesoMemoria getBase() { return base; }
    public String getCampo()       { return campo; }
    public boolean isPorPuntero()  { return porPuntero; }

    @Override
    public String getTipo() { return tipoCampo; }

    @Override
    public void aCodigoC(StringBuilder sb) {
      /*  base.aCodigoC(sb);
        sb.append(porPuntero ? "->" : ".");
        sb.append(campo);*/
    }
}