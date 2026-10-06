package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;


/**
 * Cuarteta: t0 = (tipoC) a
 *
 * Convierte explícitamente un valor a otro tipo (promoción de tipos).
 * Ejemplo en .y: entero + flotante  →  conversión del entero a flotante
 * C generado:    t0 = (float) t1;
 */
public class ConversionTipo extends Cuarteta {

    private final AccesoMemoria destino;
    private final String tipoC;
    private final AccesoMemoria fuente;

    public ConversionTipo(AccesoMemoria destino, String tipoC, AccesoMemoria fuente) {
        this.destino = destino;
        this.tipoC = tipoC;
        this.fuente = fuente;
    }

    public AccesoMemoria getDestino() { return destino; }
    public String getTipoC()          { return tipoC; }
    public AccesoMemoria getFuente()  { return fuente; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = (").append(tipoC).append(") ");
        fuente.aCodigoC(sb);
        sb.append(";\n");
    }
}