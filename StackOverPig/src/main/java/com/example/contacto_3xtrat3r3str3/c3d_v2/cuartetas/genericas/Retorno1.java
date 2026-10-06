package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;


/**
 * Cuarteta: return valor       (con valor)
 *           return             (void)
 *
 * Retorno de una función o metodo. El valor puede ser null si no retorna nada.
 * Ejemplo en .y: retornar a + b
 * C generado:    return t0;
 */
public class Retorno1 extends Cuarteta {

    private final AccesoMemoria valor; // puede ser null

    public Retorno1(AccesoMemoria valor) {
        this.valor = valor;
    }

    public AccesoMemoria getValor() {
        return valor;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    return");
        if (valor != null) {
            sb.append(' ');
            valor.aCodigoC(sb);
        }
        sb.append(";\n");
    }
}