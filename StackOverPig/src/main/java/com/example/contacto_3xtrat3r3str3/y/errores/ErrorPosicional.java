package com.example.contacto_3xtrat3r3str3.y.errores;

public class ErrorPosicional {

    private final int linea;
    private final int columna;
    private final String mensaje;

    public ErrorPosicional(int linea, int columna, String mensaje) {
        this.linea = linea;
        this.columna = columna;
        this.mensaje = mensaje;
    }

    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }

    public String getMensaje() {
        return mensaje;
    }

    @Override
    public String toString() {
        return "Error [linea " + linea + ", columna " + columna + "]: " + mensaje;
    }
}
