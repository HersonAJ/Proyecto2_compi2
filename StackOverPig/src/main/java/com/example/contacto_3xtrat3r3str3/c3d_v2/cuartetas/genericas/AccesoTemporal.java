package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;

/**
 * Operando: t0, t1, t2, ...
 *
 * Representa una variable temporal generada por el compilador.
 * Ejemplo en C: t0 = a + b;
 * C generado:   t0
 */
public class AccesoTemporal extends AccesoMemoria {

    private final int numero;
    private final String tipo;

    public AccesoTemporal(int numero, String tipo) {
        this.numero = numero;
        this.tipo = tipo;
    }

    public int getNumero() { return numero; }

    @Override
    public String getTipo() { return tipo; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append('t').append(numero);
    }
}