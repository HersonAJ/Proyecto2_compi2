package com.example.stack_over_pig.riscv;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rutinas de entrada/salida escritas en ensamblador RISC-V (RV64).
 * <p>
 * Todo pasa por el ecall de Linux
 * (a7 = número de llamada; a0, a1, a2 = argumentos):
 * write = 64   read = 63   exit = 93
 * Ese es el ABI que usan qemu-riscv64 y spike con pk.
 * <p>
 * Convención de las rutinas (igual que las funciones del curso):
 * argumento en a0 (y a1), resultado en a0, se vuelve con ret.
 * Solo tocan t0-t6 y a0-a7; si usan s1-s4 los respaldan en su propio marco.
 * <p>
 * IMPORTANTE para quien las llama: ningún registro t* sobrevive a un jal.
 * Los valores que se necesiten después de la llamada deben guardarse en la pila.
 */
final class RutinasConsola {

    private RutinasConsola() {
    }

    static final String IMPRIMIR_CADENA = "__imprimir_cadena";
    static final String IMPRIMIR_ENTERO = "__imprimir_entero";
    static final String IMPRIMIR_CARACTER = "__imprimir_caracter";
    static final String LEER_CARACTER = "__leer_caracter";
    static final String LEER_ENTERO = "__leer_entero";
    static final String LEER_CADENA = "__leer_cadena";
    static final String CONCAT = "__concat";
    static final String Z_TO_STRING_INT = "__z_to_string_int";

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
                    """,

            CONCAT, """
                    # a0 = string izq, a1 = string der. Devuelve en a0 un string nuevo (en el heap) con izq + der.
                    # Respalda ra, s1-s4. La reserva del heap se redondea a múltiplo de 8 para mantener alineados
                    # los objetos que se reserven después.
                    __concat:
                        addi sp, sp, -48
                        sd   ra, 40(sp)
                        sd   s1, 32(sp)
                        sd   s2, 24(sp)
                        sd   s3, 16(sp)
                        sd   s4, 8(sp)
                        mv   s1, a0
                        mv   s2, a1
                        # longitud izq
                        mv   a1, s1
                        li   a2, 0
                    __cc_l1:
                        add  t0, a1, a2
                        lbu  t1, 0(t0)
                        beq  t1, zero, __cc_l1_fin
                        addi a2, a2, 1
                        j    __cc_l1
                    __cc_l1_fin:
                        mv   t2, a2        # len1
                        # longitud der
                        mv   a1, s2
                        li   a2, 0
                    __cc_l2:
                        add  t0, a1, a2
                        lbu  t1, 0(t0)
                        beq  t1, zero, __cc_l2_fin
                        addi a2, a2, 1
                        j    __cc_l2
                    __cc_l2_fin:
                        add  a0, t2, a2
                        addi a0, a0, 1     # + terminador
                        addi a0, a0, 7
                        andi a0, a0, -8    # redondear a múltiplo de 8
                        mv   s3, a0
                        # reservar en el heap (bump)
                        la   t0, heap_ptr
                        ld   t1, 0(t0)
                        add  t2, t1, s3
                        sd   t2, 0(t0)
                        la   t3, heap
                        add  s4, t3, t1     # s4 = destino
                        # copiar izq a s4
                        mv   a1, s1
                        mv   a0, s4
                        li   t0, 0
                    __cc_c1:
                        add  t1, a1, t0
                        lbu  t2, 0(t1)
                        beq  t2, zero, __cc_c1_fin
                        add  t1, a0, t0
                        sb   t2, 0(t1)
                        addi t0, t0, 1
                        j    __cc_c1
                    __cc_c1_fin:
                        # copiar der después
                        mv   t3, t0        # pos
                        mv   a2, s2
                        li   t0, 0
                    __cc_c2:
                        add  t1, a2, t0
                        lbu  t2, 0(t1)
                        beq  t2, zero, __cc_c2_fin
                        add  t1, a0, t3
                        sb   t2, 0(t1)
                        addi t3, t3, 1
                        addi t0, t0, 1
                        j    __cc_c2
                    __cc_c2_fin:
                        add  t1, a0, t3
                        sb   zero, 0(t1)
                        mv   a0, s4
                        ld   ra, 40(sp)
                        ld   s1, 32(sp)
                        ld   s2, 24(sp)
                        ld   s3, 16(sp)
                        ld   s4, 8(sp)
                        addi sp, sp, 48
                        ret
                    """,

            Z_TO_STRING_INT, """
                    # a1 = entero con signo. Devuelve en a0 un string nuevo (en el heap) con su representación decimal.
                    # Los dígitos se arman al revés en un buffer de la pila, terminado en 0, y luego se copian al heap.
                    __z_to_string_int:
                        addi sp, sp, -64
                        sd   ra, 56(sp)
                        addi t1, sp, 48
                        mv   t2, t1
                        sb   zero, 0(t1)    # terminador de cadena
                        li   t3, 0
                        mv   t0, a1
                        bge  t0, zero, __ztsi_positivo
                        sub  t0, zero, t0
                        li   t3, 1
                    __ztsi_positivo:
                        li   t4, 10
                    __ztsi_ciclo:
                        remu t5, t0, t4
                        divu t0, t0, t4
                        addi t5, t5, 48
                        addi t2, t2, -1
                        sb   t5, 0(t2)
                        bne  t0, zero, __ztsi_ciclo
                        beq  t3, zero, __ztsi_fin
                        addi t2, t2, -1
                        li   t5, 45
                        sb   t5, 0(t2)
                    __ztsi_fin:
                        # reservar en el heap: longitud + terminador, redondeado a múltiplo de 8
                        sub  t0, t1, t2
                        addi t0, t0, 1
                        addi t0, t0, 7
                        andi t0, t0, -8
                        la   t5, heap_ptr
                        ld   t4, 0(t5)
                        add  t3, t4, t0
                        sd   t3, 0(t5)
                        la   t6, heap
                        add  a0, t6, t4     # a0 = destino
                        mv   t1, a0
                        mv   t3, t2
                    __ztsi_copiar:
                        lbu  t4, 0(t3)
                        sb   t4, 0(t1)
                        beq  t4, zero, __ztsi_copiado
                        addi t1, t1, 1
                        addi t3, t3, 1
                        j    __ztsi_copiar
                    __ztsi_copiado:
                        ld   ra, 56(sp)
                        addi sp, sp, 64
                        ret
                    """
    );

    /**
     * Devuelve el código de las rutinas pedidas (más las que dependen de ellas).
     */
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