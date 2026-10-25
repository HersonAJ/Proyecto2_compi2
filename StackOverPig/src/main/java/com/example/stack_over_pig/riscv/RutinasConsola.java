package com.example.stack_over_pig.riscv;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rutinas de entrada/salida escritas en ensamblador RISC-V (RV64).
 *
 * No se usa ninguna librería de C: todo pasa por el ecall de Linux
 * (a7 = número de llamada; a0, a1, a2 = argumentos):
 *   write = 64   read = 63   exit = 93
 * Ese es el ABI que usan qemu-riscv64 y spike con pk.
 *
 * Convención de las rutinas (igual que las funciones del curso):
 *   argumento en a0 (y a1), resultado en a0, se vuelve con ret.
 *   Solo tocan t0-t6 y a0-a7; si usan s1/s2 los respaldan en su propio marco.
 */
final class RutinasConsola {

    private RutinasConsola() {}

    static final String IMPRIMIR_CADENA = "__imprimir_cadena";
    static final String IMPRIMIR_ENTERO = "__imprimir_entero";
    static final String IMPRIMIR_CARACTER = "__imprimir_caracter";
    static final String LEER_CARACTER = "__leer_caracter";
    static final String LEER_ENTERO = "__leer_entero";
    static final String LEER_CADENA = "__leer_cadena";

    private static final Map<String, String> CODIGO = Map.of(

            IMPRIMIR_CADENA, """
            # a0 = dirección de una cadena terminada en 0. Mide la longitud y hace write(1, cadena, longitud)
            __imprimir_cadena:
                mv   a1, a0
                li   a2, 0
            __ic_medir:
                add  t0, a1, a2
                lbu  t1, 0(t0)
                beq  t1, zero, __ic_escribir
                addi a2, a2, 1
                j    __ic_medir
            __ic_escribir:
                li   a0, 1
                li   a7, 64
                ecall
                ret
            """,

            IMPRIMIR_CARACTER, """
            # a0 = código del carácter. Lo deja en la pila y hace write(1, &caracter, 1)
            __imprimir_caracter:
                addi sp, sp, -16
                sb   a0, 0(sp)
                li   a0, 1
                mv   a1, sp
                li   a2, 1
                li   a7, 64
                ecall
                addi sp, sp, 16
                ret
            """,

            IMPRIMIR_ENTERO, """
            # a0 = entero con signo (64 bits). Convierte a texto desde el final de un buffer de la pila y hace write
            __imprimir_entero:
                addi sp, sp, -48
                addi t1, sp, 40
                mv   t2, t1
                li   t3, 0
                bge  a0, zero, __ie_positivo
                sub  a0, zero, a0
                li   t3, 1
            __ie_positivo:
                li   t4, 10
            __ie_ciclo:
                remu t5, a0, t4
                divu a0, a0, t4
                addi t5, t5, 48
                addi t2, t2, -1
                sb   t5, 0(t2)
                bne  a0, zero, __ie_ciclo
                beq  t3, zero, __ie_escribir
                addi t2, t2, -1
                li   t5, 45
                sb   t5, 0(t2)
            __ie_escribir:
                li   a0, 1
                mv   a1, t2
                sub  a2, t1, t2
                li   a7, 64
                ecall
                addi sp, sp, 48
                ret
            """,

            LEER_CARACTER, """
            # Devuelve en a0 el siguiente byte de la entrada estándar (-1 si no hay más)
            __leer_caracter:
                addi sp, sp, -16
                li   a0, 0
                mv   a1, sp
                li   a2, 1
                li   a7, 63
                ecall
                li   t0, 1
                bne  a0, t0, __lc_fin_archivo
                lbu  a0, 0(sp)
                addi sp, sp, 16
                ret
            __lc_fin_archivo:
                li   a0, -1
                addi sp, sp, 16
                ret
            """,

            LEER_ENTERO, """
            # Lee un entero decimal (con signo opcional). Salta espacios y saltos de línea al inicio
            __leer_entero:
                addi sp, sp, -32
                sd   ra, 24(sp)
                sd   s1, 16(sp)
                sd   s2, 8(sp)
                li   s1, 0
                li   s2, 1
            __le_saltar:
                jal  ra, __leer_caracter
                li   t0, 32
                beq  a0, t0, __le_saltar
                li   t0, 10
                beq  a0, t0, __le_saltar
                li   t0, 13
                beq  a0, t0, __le_saltar
                li   t0, 9
                beq  a0, t0, __le_saltar
                li   t0, 45
                bne  a0, t0, __le_digitos
                li   s2, -1
                jal  ra, __leer_caracter
            __le_digitos:
                li   t0, 48
                blt  a0, t0, __le_fin
                li   t0, 57
                blt  t0, a0, __le_fin
                addi a0, a0, -48
                li   t1, 10
                mul  s1, s1, t1
                add  s1, s1, a0
                jal  ra, __leer_caracter
                j    __le_digitos
            __le_fin:
                mul  a0, s1, s2
                ld   ra, 24(sp)
                ld   s1, 16(sp)
                ld   s2, 8(sp)
                addi sp, sp, 32
                ret
            """,

            LEER_CADENA, """
            # a0 = buffer destino, a1 = capacidad en bytes. Lee una línea y la deja terminada en 0
            __leer_cadena:
                addi sp, sp, -32
                sd   ra, 24(sp)
                sd   s1, 16(sp)
                sd   s2, 8(sp)
                mv   s1, a0
                addi s2, a1, -1
            __lcad_ciclo:
                jal  ra, __leer_caracter
                li   t0, 10
                beq  a0, t0, __lcad_fin
                blt  a0, zero, __lcad_fin
                li   t0, 13
                beq  a0, t0, __lcad_ciclo
                bge  zero, s2, __lcad_ciclo
                sb   a0, 0(s1)
                addi s1, s1, 1
                addi s2, s2, -1
                j    __lcad_ciclo
            __lcad_fin:
                sb   zero, 0(s1)
                ld   ra, 24(sp)
                ld   s1, 16(sp)
                ld   s2, 8(sp)
                addi sp, sp, 32
                ret
            """
    );

    /** Devuelve el código de las rutinas pedidas (más las que dependen de ellas). */
    static String generar(Set<String> usadas) {
        Set<String> todas = new LinkedHashSet<>(usadas);
        if (todas.contains(LEER_ENTERO) || todas.contains(LEER_CADENA)) {
            todas.add(LEER_CARACTER);
        }
        StringBuilder sb = new StringBuilder();
        for (String nombre : todas) {
            sb.append(CODIGO.get(nombre)).append("\n");
        }
        return sb.toString();
    }
}