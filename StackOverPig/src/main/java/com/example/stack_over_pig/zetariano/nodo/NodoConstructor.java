package com.example.stack_over_pig.zetariano.nodo;

import java.util.List;

public record NodoConstructor(int linea, int columna, Visibilidad visibilidad, String nombre,
                              List<NodoParametroZ> parametros,
                              List<NodoSentencia> cuerpo) implements NodoAST {
}