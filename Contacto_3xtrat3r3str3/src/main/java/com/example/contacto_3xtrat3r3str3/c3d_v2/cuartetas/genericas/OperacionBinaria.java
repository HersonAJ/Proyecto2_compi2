package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;


/**
 * t0 = a + b
 * También sirve para ==, !=, <, >, <=, >=, &&, ||.
 * Casos especiales: "strcmp" (comparar cadenas) y "concat" (concatenar cadenas).
 */
public class OperacionBinaria extends Cuarteta {

    private final AccesoMemoria destino;
    private final AccesoMemoria izquierda;
    private final String operador;
    private final AccesoMemoria derecha;

    public OperacionBinaria(AccesoMemoria destino, AccesoMemoria izquierda,
                            String operador, AccesoMemoria derecha) {
        this.destino = destino;
        this.izquierda = izquierda;
        this.operador = operador;
        this.derecha = derecha;
    }

    public AccesoMemoria getDestino()   { return destino; }
    public AccesoMemoria getIzquierda() { return izquierda; }
    public String getOperador()         { return operador; }
    public AccesoMemoria getDerecha()   { return derecha; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = ");

        if ("strcmp".equals(operador)) {
            sb.append("strcmp(");
            izquierda.aCodigoC(sb);
            sb.append(", ");
            derecha.aCodigoC(sb);
            sb.append(")");
        } else if ("concat".equals(operador)) {
            sb.append("concat(");
            izquierda.aCodigoC(sb);
            sb.append(", ");
            derecha.aCodigoC(sb);
            sb.append(")");
        } else {
            izquierda.aCodigoC(sb);
            sb.append(' ').append(operador).append(' ');
            derecha.aCodigoC(sb);
        }

        sb.append(";\n");
    }
}