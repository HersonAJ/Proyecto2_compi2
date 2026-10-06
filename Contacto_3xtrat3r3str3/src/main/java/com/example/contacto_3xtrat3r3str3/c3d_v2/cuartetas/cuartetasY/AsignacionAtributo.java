package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;


import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Cuarteta: p.campo = y        (struct por valor)
 *            p->campo = y       (struct por puntero)
 *
 * Asigna un valor a un campo de una estructura u objeto.
 * Ejemplo en .y: alumno1.nombre = "alguien"
 * C generado:    alumno1.nombre = "alguien";
 */
public class AsignacionAtributo extends Cuarteta {

    private final AccesoMemoria base;
    private final String campo;
    private final boolean porPuntero;
    private final AccesoMemoria valor;

    public AsignacionAtributo(AccesoMemoria base, String campo, boolean porPuntero, AccesoMemoria valor) {
        this.base = base;
        this.campo = campo;
        this.porPuntero = porPuntero;
        this.valor = valor;
    }

    public AccesoMemoria getBase()   { return base; }
    public String getCampo()         { return campo; }
    public boolean isPorPuntero()    { return porPuntero; }
    public AccesoMemoria getValor()  { return valor; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        base.aCodigoC(sb);
        sb.append(porPuntero ? "->" : ".");
        sb.append(campo).append(" = ");
        valor.aCodigoC(sb);
        sb.append(";\n");
    }
}