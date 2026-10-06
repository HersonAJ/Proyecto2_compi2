package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;

/**
 * Operando: p->campo  o  p.campo
 *
 * Acceso a un campo de estructura u objeto.
 * En .z siempre se usa '->' porque los objetos son punteros.
 * Ejemplo en .z: p1.edad
 * C generado:    p1->edad
 */
public class AccesoAtributo1 extends AccesoMemoria {

    private final AccesoMemoria base;
    private final String campo;
    private final boolean porPuntero;
    private final String tipoCampo; // tipo del campo

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
        base.aCodigoC(sb);
        sb.append(porPuntero ? "->" : ".");
        sb.append(campo);
    }
}