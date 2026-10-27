package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.Cuarteta;

import java.util.List;
import java.util.Map;

/**
 * Representa una función RISC-V a emitir además de main (constructor o método de Zetariano).
 *
 * - nombre: nombre único en el .s (ej. "Persona_constructor_textum_numerus").
 * - tipoClase: clase a la que pertenece (ej. "Persona"). Necesario para resolver this.campo.
 * - parametros: parámetros formales (sin this).
 * - tieneThis: true si a0 es this.
 * - tipoRetorno: tipo C de retorno ("int", "void", null = void).
 * - tiposVariables: nombre -> tipo (primitivo o clase). Para resolver obj.campo.
 * - cuartetas: cuerpo ya traducido a C3D.
 */
public record FuncionRiscV(
        String nombre,
        String tipoClase,
        List<ParametroRiscV> parametros,
        boolean tieneThis,
        String tipoRetorno,
        Map<String, String> tiposVariables,
        List<Cuarteta> cuartetas) {

    public record ParametroRiscV(String nombre, String tipoPig, boolean esPorReferencia) {
        public ParametroRiscV(String nombre, String tipoPig) {
            this(nombre, tipoPig, false);
        }
    }
}