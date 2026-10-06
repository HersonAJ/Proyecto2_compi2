package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY;


import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Cuarteta: goto Lx
 *
 * Salto incondicional al final del ciclo.
 * Se emite cuando el lenguaje fuente usa 'romper' (en .y) o 'break'.
 * La etiqueta concreta la decide el AST a partir de la pila de ciclos.
 * Ejemplo en .y: romper
 * C generado:    goto L1;
 */
public class Romper1 extends Cuarteta {

    private final int etiqueta;

    public Romper1(int etiqueta) {
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