package com.example.contacto_3xtrat3r3str3.y.ast;

import java.util.List;

/**
 * Representa la definición de una estructura:
 *
 *   estructura Persona:
 *       entero edad
 *       cadena nombre
 */
public sealed interface NodoEstructura extends NodoAST permits NodoEstructura.Estructura {

    TipoNodoEstructura tipoNodo();

    record Estructura(int linea, int columna,
                      String nombre,
                      List<NodoAtributo> atributos) implements NodoEstructura {
        @Override
        public TipoNodoEstructura tipoNodo() {
            return TipoNodoEstructura.ESTRUCTURA;
        }
    }
}