package com.example.contacto_3xtrat3r3str3.piglatin.nodo;

// Representa una importación: import carpeta.Objeto1.z import carpeta.Funciones.y
public record NodoImportacion(int linea, int columna, String ruta) implements NodoAST {
}