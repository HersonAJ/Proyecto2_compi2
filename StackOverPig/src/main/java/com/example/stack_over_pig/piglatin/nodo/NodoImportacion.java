package com.example.stack_over_pig.piglatin.nodo;

// Representa una importación: import carpeta.Objeto1.z import carpeta.Funciones.y
public record NodoImportacion(int linea, int columna, String ruta) implements NodoAST {
}