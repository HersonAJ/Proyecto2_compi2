package com.example.contacto_3xtrat3r3str3.y.ast;

/**
 * Representa un parámetro de una función.
 *
 * Casos posibles:
 *   entero miEntero
 *   [] entero miArray           (arreglo, por referencia)
 *   {} MiEstructura miStruct    (estructura, por referencia)
 */
public sealed interface NodoParametro extends NodoAST permits NodoParametro.Parametro {

    TipoNodoEstructura tipoNodo();

    record Parametro(int linea, int columna,
                     String tipoPrimitivo,
                     String tipoEstructura,
                     String nombre,
                     boolean esArreglo,
                     boolean esEstructura) implements NodoParametro {
        @Override
        public TipoNodoEstructura tipoNodo() {
            return TipoNodoEstructura.PARAMETRO;
        }
    }
}