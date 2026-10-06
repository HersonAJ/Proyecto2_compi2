package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;

/**
 * Operando: a[i]  o  m[i][j]
 *
 * Acceso a un elemento de arreglo. En .y y .z cada nivel de acceso genera
 * un AccesoArreglo anidado, así 'm[i][j]' queda como m[i][j] en C.
 * Ejemplo en .y: numeros[2]
 * C generado:    numeros[2]
 */
public class AccesoArreglo extends AccesoMemoria {

    private final AccesoMemoria base;
    private final AccesoMemoria indice;
    private final String tipoElemento; // tipo del elemento, no del arreglo

    public AccesoArreglo(AccesoMemoria base, AccesoMemoria indice, String tipoElemento) {
        this.base = base;
        this.indice = indice;
        this.tipoElemento = tipoElemento;
    }

    public AccesoMemoria getBase()   { return base; }
    public AccesoMemoria getIndice() { return indice; }

    @Override
    public String getTipo() { return tipoElemento; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        base.aCodigoC(sb);
        sb.append('[');
        indice.aCodigoC(sb);
        sb.append(']');
    }
}