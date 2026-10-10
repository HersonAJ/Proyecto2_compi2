package com.example.stack_over_pig.zetariano.nodo;

import java.util.List;

public record NodoClase(int linea, int columna, Visibilidad visibilidad, String nombre,
                        String superclase, List<NodoAtributoZ> atributos,
                        List<NodoConstructor> constructores,
                        List<NodoMetodo> metodos) implements NodoAST {
}