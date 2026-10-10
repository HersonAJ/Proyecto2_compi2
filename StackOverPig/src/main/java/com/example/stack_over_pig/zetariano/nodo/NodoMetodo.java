package com.example.stack_over_pig.zetariano.nodo;

import java.util.List;

public record NodoMetodo(int linea, int columna, boolean esOverride, Visibilidad visibilidad,
                         String nombre, List<NodoParametroZ> parametros,
                         String tipoRetorno, List<NodoSentencia> cuerpo) implements NodoAST {
}