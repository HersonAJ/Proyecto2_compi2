package com.example.stack_over_pig.zetariano.nodo;

public record NodoAtributoZ(int linea, int columna, Visibilidad visibilidad,
                            String tipo, String nombre, int dimensiones) implements NodoAST {
}