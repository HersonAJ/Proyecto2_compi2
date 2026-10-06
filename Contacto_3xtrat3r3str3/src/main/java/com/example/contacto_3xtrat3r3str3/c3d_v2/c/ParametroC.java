package com.example.contacto_3xtrat3r3str3.c3d_v2.c;


/**
 * Un parámetro de una función en C.
 *   tipoC: "int", "float", "char*", "Persona*", etc.
 *   porReferencia: si es true, en C se escribe como puntero.
 *                  La decisión de "int*" vs "int" la toma el AST al
 *                  construir este objeto; aquí ya viene el tipoC final.
 */
public class ParametroC {

    private final String tipoC;
    private final String nombre;

    public ParametroC(String tipoC, String nombre) {
        this.tipoC = tipoC;
        this.nombre = nombre;
    }

    public String getTipoC()  { return tipoC; }
    public String getNombre() { return nombre; }

    public void aCodigoC(StringBuilder sb) {
        sb.append(tipoC).append(' ').append(nombre);
    }
}