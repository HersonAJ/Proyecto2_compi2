package com.example.contacto_3xtrat3r3str3.c3d_v2.c;

import java.util.List;

/**
 * Representa un struct de C listo para escribir:
 *   struct Nombre {
 *       tipo campo;
 *       ...
 *   };
 */
public class EstructuraC {

    private final String nombre;
    private final List<ParametroC> campos;   // reutilizamos ParametroC (tipoC + nombre)

    public EstructuraC(String nombre, List<ParametroC> campos) {
        this.nombre = nombre;
        this.campos = campos;
    }

    public String getNombre() { return nombre; }
    public List<ParametroC> getCampos() { return campos; }

    public void aCodigoC(StringBuilder sb) {
        sb.append("struct ").append(nombre).append(" {\n");
        for (ParametroC campo : campos) {
            sb.append("    ").append(campo.getTipoC())
                    .append(' ').append(campo.getNombre()).append(";\n");
        }
        sb.append("};\n\n");
    }
}