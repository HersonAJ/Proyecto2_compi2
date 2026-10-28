package com.example.stack_over_pig.riscv;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rutinas de entrada/salida escritas en ensamblador RISC-V (RV64).
 * write = 64   read = 63   exit = 93
 * Convención: argumento en a0/a1, resultado en a0, ret.
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
    static final String Z_TO_STRING_CHAR = "__z_to_string_char";
    static final String Z_TO_STRING_BOOL = "__z_to_string_bool";
    static final String IMPRIMIR_DECIMAL = "__imprimir_decimal";
    static final String Z_TO_STRING_DOUBLE = "__z_to_string_double";
    static final String LEER_DECIMAL = "__leer_decimal";

    private static final Map<String, String> CODIGO = Map.ofEntries(

            Map.entry(IMPRIMIR_CADENA, """
                    # a0 = dirección de una cadena terminada en 0.
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
                    """),

            Map.entry(IMPRIMIR_CARACTER, """
                    # a0 = código del carácter.
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
                    """),

            Map.entry(IMPRIMIR_ENTERO, """
                    # a0 = entero con signo (64 bits).
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
                    """),

            Map.entry(LEER_CARACTER, """
                    # Devuelve en a0 el siguiente byte (-1 si EOF).
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
                    """),

            Map.entry(LEER_ENTERO, """
                    # Lee entero decimal con signo opcional.
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
                    """),

            Map.entry(LEER_CADENA, """
                    # a0 = buffer destino, a1 = capacidad. Lee una línea terminada en 0.
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
                    """),

            Map.entry(CONCAT, """
                    # a0 = izq, a1 = der. Devuelve a0 = izq + der (en el heap).
                    __concat:
                        addi sp, sp, -48
                        sd   ra, 40(sp)
                        sd   s1, 32(sp)
                        sd   s2, 24(sp)
                        sd   s3, 16(sp)
                        sd   s4, 8(sp)
                        mv   s1, a0
                        mv   s2, a1
                        mv   a1, s1
                        li   a2, 0
                    __cc_l1:
                        add  t0, a1, a2
                        lbu  t1, 0(t0)
                        beq  t1, zero, __cc_l1_fin
                        addi a2, a2, 1
                        j    __cc_l1
                    __cc_l1_fin:
                        mv   t2, a2
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
                        addi a0, a0, 1
                        addi a0, a0, 7
                        andi a0, a0, -8
                        mv   s3, a0
                        la   t0, heap_ptr
                        ld   t1, 0(t0)
                        add  t2, t1, s3
                        sd   t2, 0(t0)
                        la   t3, heap
                        add  s4, t3, t1
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
                        mv   t3, t0
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
                    """),

            Map.entry(Z_TO_STRING_INT, """
                    # a1 = entero con signo. Devuelve a0 = string decimal.
                    __z_to_string_int:
                        addi sp, sp, -64
                        sd   ra, 56(sp)
                        addi t1, sp, 48
                        mv   t2, t1
                        sb   zero, 0(t1)
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
                        sub  t0, t1, t2
                        addi t0, t0, 1
                        addi t0, t0, 7
                        andi t0, t0, -8
                        la   t5, heap_ptr
                        ld   t4, 0(t5)
                        add  t3, t4, t0
                        sd   t3, 0(t5)
                        la   t6, heap
                        add  a0, t6, t4
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
                    """),

            Map.entry(Z_TO_STRING_CHAR, """
                    # a1 = código de carácter. Devuelve a0 = string de 1 carácter.
                    __z_to_string_char:
                        la   t5, heap_ptr
                        ld   t4, 0(t5)
                        addi t3, t4, 8
                        sd   t3, 0(t5)
                        la   t6, heap
                        add  a0, t6, t4
                        sb   a1, 0(a0)
                        sb   zero, 1(a0)
                        ret
                    """),

            Map.entry(Z_TO_STRING_BOOL, """
                    # a1 = 0 o 1. Devuelve a0 = "true" o "false".
                    __z_to_string_bool:
                        la   t5, heap_ptr
                        ld   t4, 0(t5)
                        addi t3, t4, 8
                        sd   t3, 0(t5)
                        la   t6, heap
                        add  a0, t6, t4
                        beq  a1, zero, __ztsb_falso
                        li   t0, 116
                        sb   t0, 0(a0)
                        li   t0, 114
                        sb   t0, 1(a0)
                        li   t0, 117
                        sb   t0, 2(a0)
                        li   t0, 101
                        sb   t0, 3(a0)
                        sb   zero, 4(a0)
                        ret
                    __ztsb_falso:
                        li   t0, 102
                        sb   t0, 0(a0)
                        li   t0, 97
                        sb   t0, 1(a0)
                        li   t0, 108
                        sb   t0, 2(a0)
                        li   t0, 115
                        sb   t0, 3(a0)
                        li   t0, 101
                        sb   t0, 4(a0)
                        sb   zero, 5(a0)
                        ret
                    """),

            Map.entry(IMPRIMIR_DECIMAL, """
                    # a0 = entero escalado por 10^6. Imprime <entero>.<6 decimales>.
                    __imprimir_decimal:
                        addi sp, sp, -96
                        sd   ra, 88(sp)
                        sd   s1, 80(sp)
                        sd   s2, 72(sp)
                        sd   s3, 64(sp)
                        sd   s4, 56(sp)
                        mv   s1, a0
                        bge  s1, zero, __id_positivo
                        li   t0, 45
                        sb   t0, 0(sp)
                        li   a0, 1
                        mv   a1, sp
                        li   a2, 1
                        li   a7, 64
                        ecall
                        sub  s1, zero, s1
                    __id_positivo:
                        li   t1, 1000000
                        remu s2, s1, t1
                        divu s1, s1, t1
                        mv   a0, s1
                        jal  ra, __imprimir_entero
                        li   t0, 46
                        sb   t0, 0(sp)
                        li   a0, 1
                        mv   a1, sp
                        li   a2, 1
                        li   a7, 64
                        ecall
                        addi t1, sp, 40
                        mv   t2, t1
                        li   t3, 6
                        mv   t4, s2
                        li   t5, 10
                    __id_ciclo:
                        remu t6, t4, t5
                        divu t4, t4, t5
                        addi t6, t6, 48
                        addi t2, t2, -1
                        sb   t6, 0(t2)
                        addi t3, t3, -1
                        bne  t3, zero, __id_ciclo
                        li   a0, 1
                        mv   a1, t2
                        li   a2, 6
                        li   a7, 64
                        ecall
                        ld   ra, 88(sp)
                        ld   s1, 80(sp)
                        ld   s2, 72(sp)
                        ld   s3, 64(sp)
                        ld   s4, 56(sp)
                        addi sp, sp, 96
                        ret
                    """),

            Map.entry(Z_TO_STRING_DOUBLE, """
                    # a1 = entero escalado por 10^6. Devuelve a0 = "<entero>.<6 decimales>".
                    __z_to_string_double:
                        addi sp, sp, -128
                        sd   ra, 120(sp)
                        sd   s1, 112(sp)
                        sd   s2, 104(sp)
                        sd   s3, 96(sp)
                        sd   s4, 88(sp)
                        sd   s5, 80(sp)
                        la   t0, heap_ptr
                        ld   t1, 0(t0)
                        addi t2, t1, 32
                        sd   t2, 0(t0)
                        la   t3, heap
                        add  s4, t3, t1
                        mv   s1, a1
                        mv   s5, s4
                        bge  s1, zero, __ztsd_pos
                        li   t0, 45
                        sb   t0, 0(s5)
                        addi s5, s5, 1
                        sub  s1, zero, s1
                    __ztsd_pos:
                        li   t1, 1000000
                        remu s2, s1, t1
                        divu s3, s1, t1
                        addi t1, sp, 64
                        mv   t2, t1
                        mv   t4, s3
                        li   t5, 10
                        beq  t4, zero, __ztsd_cero
                    __ztsd_ent_ciclo:
                        remu t6, t4, t5
                        divu t4, t4, t5
                        addi t6, t6, 48
                        addi t2, t2, -1
                        sb   t6, 0(t2)
                        bne  t4, zero, __ztsd_ent_ciclo
                        j    __ztsd_ent_copia
                    __ztsd_cero:
                        addi t2, t2, -1
                        li   t6, 48
                        sb   t6, 0(t2)
                    __ztsd_ent_copia:
                        lbu  t6, 0(t2)
                        beq  t6, zero, __ztsd_ent_fin
                        sb   t6, 0(s5)
                        addi t2, t2, 1
                        addi s5, s5, 1
                        j    __ztsd_ent_copia
                    __ztsd_ent_fin:
                        li   t0, 46
                        sb   t0, 0(s5)
                        addi s5, s5, 1
                        addi t1, sp, 76
                        mv   t2, t1
                        li   t3, 6
                        mv   t4, s2
                        li   t5, 10
                    __ztsd_frac_ciclo:
                        remu t6, t4, t5
                        divu t4, t4, t5
                        addi t6, t6, 48
                        addi t2, t2, -1
                        sb   t6, 0(t2)
                        addi t3, t3, -1
                        bne  t3, zero, __ztsd_frac_ciclo
                        li   t3, 6
                    __ztsd_frac_copia:
                        lbu  t6, 0(t2)
                        sb   t6, 0(s5)
                        addi t2, t2, 1
                        addi s5, s5, 1
                        addi t3, t3, -1
                        bne  t3, zero, __ztsd_frac_copia
                        sb   zero, 0(s5)
                        mv   a0, s4
                        ld   ra, 120(sp)
                        ld   s1, 112(sp)
                        ld   s2, 104(sp)
                        ld   s3, 96(sp)
                        ld   s4, 88(sp)
                        ld   s5, 80(sp)
                        addi sp, sp, 128
                        ret
                    """),

            Map.entry(LEER_DECIMAL, """
                    # Devuelve a0 = entero escalado por 10^6, leído como decimal con signo y punto opcional.
                    __leer_decimal:
                        addi sp, sp, -48
                        sd   ra, 40(sp)
                        sd   s1, 32(sp)
                        sd   s2, 24(sp)
                        sd   s3, 16(sp)
                        li   s1, 0
                        li   s2, 0
                        li   s3, 0
                    __ld_saltar:
                        jal  ra, __leer_caracter
                        li   t0, 32
                        beq  a0, t0, __ld_saltar
                        li   t0, 10
                        beq  a0, t0, __ld_saltar
                        li   t0, 13
                        beq  a0, t0, __ld_saltar
                        li   t0, 9
                        beq  a0, t0, __ld_saltar
                        li   t0, 45
                        bne  a0, t0, __ld_ent
                        li   s3, 1
                        jal  ra, __leer_caracter
                    __ld_ent:
                        li   t0, 48
                        blt  a0, t0, __ld_punto
                        li   t0, 57
                        blt  t0, a0, __ld_punto
                        addi a0, a0, -48
                        li   t1, 10
                        mul  s1, s1, t1
                        add  s1, s1, a0
                        jal  ra, __leer_caracter
                        j    __ld_ent
                    __ld_punto:
                        li   t0, 46
                        bne  a0, t0, __ld_fin
                        jal  ra, __leer_caracter
                    __ld_frac:
                        li   t0, 48
                        blt  a0, t0, __ld_fin
                        li   t0, 57
                        blt  t0, a0, __ld_fin
                        addi a0, a0, -48
                        li   t1, 10
                        mul  s2, s2, t1
                        add  s2, s2, a0
                        jal  ra, __leer_caracter
                        j    __ld_frac
                    __ld_fin:
                        li   t0, 6
                        mv   t1, s2
                        li   t2, 0
                    __ld_contar:
                        beq  t1, zero, __ld_escala
                        li   t3, 10
                        divu t1, t1, t3
                        addi t2, t2, 1
                        j    __ld_contar
                    __ld_escala:
                        li   t3, 6
                        sub  t3, t3, t2
                        beq  t3, zero, __ld_armar
                        li   t4, 10
                    __ld_mult:
                        mul  s2, s2, t4
                        addi t3, t3, -1
                        bne  t3, zero, __ld_mult
                    __ld_armar:
                        li   t1, 1000000
                        mul  s1, s1, t1
                        add  s1, s1, s2
                        beq  s3, zero, __ld_pos
                        sub  s1, zero, s1
                    __ld_pos:
                        mv   a0, s1
                        ld   ra, 40(sp)
                        ld   s1, 32(sp)
                        ld   s2, 24(sp)
                        ld   s3, 16(sp)
                        addi sp, sp, 48
                        ret
                    """)
    );

    static String generar(Set<String> usadas) {
        Set<String> todas = new LinkedHashSet<>(usadas);
        if (todas.contains(LEER_ENTERO) || todas.contains(LEER_CADENA) || todas.contains(LEER_DECIMAL)) {
            todas.add(LEER_CARACTER);
        }
        if (todas.contains(IMPRIMIR_DECIMAL)) {
            todas.add(IMPRIMIR_ENTERO);
        }
        StringBuilder sb = new StringBuilder();
        for (String nombre : todas) {
            sb.append(CODIGO.get(nombre)).append("\n");
        }
        return sb.toString();
    }
}