package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;

/**
 * Operando: 10, 3.14, 'a', "hola", 1/0
 *
 * Literal de cualquier tipo primitivo de .y.
 * Ejemplo en .y: 25
 * C generado:    25
 */
public class Literal extends AccesoMemoria {

    private final Object valor;
    private final String tipo; // "entero", "flotante", "caracter", "cadena", "bool"

    public Literal(Object valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public Object getValor() {
        return valor;
    }

    public String getTipo() {
        return tipo;
    }

    @Override
    public void aCodigoC(StringBuilder sb) {
        switch (tipo) {
            case "cadena" -> sb.append('"').append(valor).append('"');
            case "caracter" -> sb.append('\'').append(valor).append('\'');
            case "bool" -> sb.append(((Boolean) valor) ? 1 : 0);
            default -> sb.append(valor);
        }
    }
}
