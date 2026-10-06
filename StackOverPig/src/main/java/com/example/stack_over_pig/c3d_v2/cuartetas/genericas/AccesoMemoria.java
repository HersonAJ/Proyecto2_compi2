package com.example.stack_over_pig.c3d_v2.cuartetas.genericas;

/**
 * Clase base de todos los operandos.
 * Puede ser: literal, temporal, variable, acceso a arreglo, acceso a atributo o etiqueta.
 * Cada subclase sabe escribirse en C y declarar su tipo.
 */
public abstract class AccesoMemoria implements TransformableACodigo {

    /**
     * Tipo del lenguaje fuente que representa este operando.
     * Ej: "entero", "int", "String", "Persona", "etiqueta".
     */
    public abstract String getTipo();
}