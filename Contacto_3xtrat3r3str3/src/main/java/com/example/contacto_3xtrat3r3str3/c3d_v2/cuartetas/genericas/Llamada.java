package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;


import java.util.List;

/**
 * Cuarteta: t0 = suma(a, b)      (con retorno)
 *           imprimir(x)          (sin retorno)
 *
 * Llamada a función. El destino puede ser null si la función es void.
 * Ejemplo en .y: suma(10, 20)
 * C generado:    t0 = suma(10, 20);
 */
public class Llamada extends Cuarteta {

    private final AccesoMemoria destino;   // puede ser null
    private final String nombreFuncion;
    private final List<AccesoMemoria> argumentos;

    public Llamada(AccesoMemoria destino, String nombreFuncion, List<AccesoMemoria> argumentos) {
        this.destino = destino;
        this.nombreFuncion = nombreFuncion;
        this.argumentos = argumentos;
    }

    public AccesoMemoria getDestino()           { return destino; }
    public String getNombreFuncion()            { return nombreFuncion; }
    public List<AccesoMemoria> getArgumentos()  { return argumentos; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    ");
        if (destino != null) {
            destino.aCodigoC(sb);
            sb.append(" = ");
        }
        sb.append(nombreFuncion).append('(');
        for (int i = 0; i < argumentos.size(); i++) {
            if (i > 0) sb.append(", ");
            argumentos.get(i).aCodigoC(sb);
        }
        sb.append(");\n");
    }
}