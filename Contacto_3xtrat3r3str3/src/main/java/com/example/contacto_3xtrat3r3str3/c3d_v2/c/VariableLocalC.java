package com.example.contacto_3xtrat3r3str3.c3d_v2.c;

/**
 * Una variable local de una función en C.
 *   - Si es primitiva:  tipoC = "int",       nombre = "resultado"
 *   - Si es arreglo:    tipoC = "int[10]",   nombre = "numeros"
 *   - Si es matriz:     tipoC = "int[3][3]", nombre = "matriz"
 *   - Si es struct:     tipoC = "Persona",   nombre = "alumno1"
 *
 * Se escribe como:  <tipoC> <nombre>;
 */
public class VariableLocalC {

    private final String tipoC;
    private final String nombre;
    private final String inicializadorC;

    public VariableLocalC(String tipoC, String nombre) {
        this(tipoC, nombre, null);
    }

    public VariableLocalC(String tipoC, String nombre, String inicializadorC) {
        this.tipoC = tipoC;
        this.nombre = nombre;
        this.inicializadorC = inicializadorC;
    }

    public String getTipoC()  { return tipoC; }
    public String getNombre() { return nombre; }
    public String getInicializadorC() { return inicializadorC; }

    public void aCodigoC(StringBuilder sb) {
        sb.append(tipoC).append(' ').append(nombre);
        if (inicializadorC != null) {
            sb.append(" = ").append(inicializadorC);
        }
        sb.append(";\n");
    }
}