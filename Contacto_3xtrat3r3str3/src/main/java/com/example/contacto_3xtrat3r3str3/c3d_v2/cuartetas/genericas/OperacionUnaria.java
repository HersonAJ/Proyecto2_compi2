package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;

/**
 * t0 = -a      (prefijo)
 * t0 = !a      (prefijo)
 */
public class OperacionUnaria extends Cuarteta {

    private final AccesoMemoria destino;
    private final String operador;
    private final AccesoMemoria operando;

    public OperacionUnaria(AccesoMemoria destino, String operador, AccesoMemoria operando) {
        this.destino = destino;
        this.operador = operador;
        this.operando = operando;
    }

    public AccesoMemoria getDestino()  { return destino; }
    public String getOperador()        { return operador; }
    public AccesoMemoria getOperando() { return operando; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = ").append(operador);
        operando.aCodigoC(sb);
        sb.append(";\n");
    }
}