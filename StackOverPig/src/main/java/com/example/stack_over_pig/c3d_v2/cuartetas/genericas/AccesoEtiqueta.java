package com.example.stack_over_pig.c3d_v2.cuartetas.genericas;


/**
 * Operando: L0, L1, L2, ...
 *
 * Representa una etiqueta usada en cuartetas de control (salto, condicional).
 * Ejemplo en C: if (x == 0) goto L1;
 * C generado:   L1
 */
public class AccesoEtiqueta extends AccesoMemoria {

    private final int numero;

    public AccesoEtiqueta(int numero) {
        this.numero = numero;
    }

    public int getNumero() {
        return numero;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append('L').append(numero);
    }

    @Override
    public String getTipo() { return "etiqueta"; }
}