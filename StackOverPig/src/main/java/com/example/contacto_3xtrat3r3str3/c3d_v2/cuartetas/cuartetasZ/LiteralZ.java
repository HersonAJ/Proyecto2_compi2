package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;

/**
 * Operando: 10, 3.14, 'a', "hola", true, false
 *
 * Literal de .z. Se escribe tal cual en C según su tipo.
 * Ejemplo en .z: 25
 * C generado:    25
 */
public class LiteralZ extends AccesoMemoria {

    private final Object valor;
    private final String tipo;   // "int", "double", "char", "boolean", "String"

    public LiteralZ(Object valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public Object getValor() { return valor; }

    @Override
    public String getTipo() { return tipo; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        switch (tipo) {
            case "String"  -> sb.append('"').append(valor).append('"');
            case "char"    -> sb.append('\'').append(valor).append('\'');
            case "boolean" -> sb.append(((Boolean) valor) ? 1 : 0);
            default        -> sb.append(valor);   // int, double
        }
    }
}