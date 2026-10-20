import com.example.stack_over_pig.piglatin.service.ResultadoCompilacionPig;
import com.example.stack_over_pig.piglatin.service.ServicioCompilacionPig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fase 1 de RISC-V: comprobar que un .pig minimo genera cuartetas y
 * ensamblador RISC-V con la estructura esperada.
 *
 * No ejecuta Venus; solo verifica el texto generado.
 */
class RiscVFase1Test {

    @TempDir
    Path raiz;

    private ResultadoCompilacionPig compilar(String pig) {
        return new ServicioCompilacionPig().compilarRiscV(pig, raiz);
    }

    // ---------- CASO 1: PROGRAMA VACIO ----------

    @Test
    void programaMinimoGeneraPrologoYEpilogo() {
        ResultadoCompilacionPig r = compilar("""
                MAIOR>
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        assertNotNull(r.getCodigoRiscV());
        String s = r.getCodigoRiscV();

        assertTrue(s.contains(".text"), s);
        assertTrue(s.contains(".globl main"), s);
        assertTrue(s.contains("main:"), s);
        assertTrue(s.contains("addi sp, sp, -"), s);
        assertTrue(s.contains("sw   s0, "), s);
        assertTrue(s.contains("sw   ra, "), s);
        assertTrue(s.contains("lw   ra, -4(s0)"), s);
        assertTrue(s.contains("li   a7, 10"), s);
        assertTrue(s.contains("ecall"), s);
    }

    // ---------- CASO 2: ESCRITURA DE ENTERO ----------

    @Test
    void imprimirEnteroGeneraSyscall1() {
        ResultadoCompilacionPig r = compilar("""
            MAIOR>
            >> 42 ;
            FINIS;
            """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        // El literal 42 debe estar cargado (en cualquier registro del pool)
        assertTrue(s.contains(", 42"), s);
        // Debe moverse a a1 para el syscall
        assertTrue(s.contains("mv   a1, "), s);
        // Syscall 1 = print_int
        assertTrue(s.contains("li   a0, 1"), s);
    }

    // ---------- CASO 3: ESCRITURA DE TEXTO ----------

    @Test
    void imprimirTextoGeneraAsciizYSyscall4() {
        ResultadoCompilacionPig r = compilar("""
                MAIOR>
                >> "Hola" ;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        assertTrue(s.contains(".data"), s);
        assertTrue(s.contains(".asciiz \"Hola\""), s);
        assertTrue(s.contains("li   a0, 4"), s);
    }

    // ---------- CASO 4: VARIABLE Y SUMA ----------

    @Test
    void variableYSumaGeneraAddYSw() {
        ResultadoCompilacionPig r = compilar("""
                VARIABILES>
                esto x : numerus 5;

                MAIOR>
                esto y : numerus 10;
                x = x + y;
                >> x ;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        assertTrue(s.contains("add"), s);
        assertTrue(s.contains("sw"), s);
        assertTrue(s.contains("lw"), s);
    }

    // ---------- CASO 5: CONDICIONAL ----------

    @Test
    void condicionalGeneraBranchYEtiquetas() {
        ResultadoCompilacionPig r = compilar("""
                MAIOR>
                esto x : numerus 5;
                si (x > 0) {
                    >> "positivo" ;
                } finis;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        assertTrue(s.contains("bgt") || s.contains("beq") || s.contains("blt"), s);
        assertTrue(s.contains("L0:") || s.contains("L1:") || s.contains("L2:"), s);
        assertTrue(s.contains("j    L"), s);
    }

    // ---------- CASO 6: CICLO DUM ----------

    @Test
    void cicloDumGeneraSaltoAtras() {
        ResultadoCompilacionPig r = compilar("""
                MAIOR>
                esto i : numerus 0;
                dum (i < 3) {
                    i = i + 1;
                } finis;
                >> i ;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        // Debe haber al menos un salto hacia atrás (j Lx) y una etiqueta
        assertTrue(s.contains("j    L"), s);
        assertTrue(s.contains("beq") || s.contains("bne")
                || s.contains("blt") || s.contains("bge"), s);
    }

    // ---------- CASO 7: ASIGNACION Y OPERACIONES ----------

    @Test
    void operacionMultiplicacionGeneraMul() {
        ResultadoCompilacionPig r = compilar("""
                MAIOR>
                esto a : numerus 6;
                esto b : numerus 7;
                esto c : numerus 0;
                c = a * b;
                >> c ;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        assertTrue(s.contains("mul"), s);
    }

    // ---------- CASO 8: LECTURA ----------

    @Test
    void lecturaGeneraSyscall5() {
        ResultadoCompilacionPig r = compilar("""
                VARIABILES>
                esto n : numerus 0;

                MAIOR>
                n <<
                >> n ;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        // Syscall 5 = read_int
        assertTrue(s.contains("li   a0, 5"), s);
    }

    // ---------- CASO 9: ERROR SEMANTICO NO GENERA RISC-V ----------

    @Test
    void programaConErrorSemanticoNoGeneraRiscV() {
        ResultadoCompilacionPig r = compilar("""
                MAIOR>
                >> zzz ;
                FINIS;
                """);

        assertFalse(r.isExitoso());
        assertFalse(r.getErroresSemanticos().isEmpty());
        assertNull(r.getCodigoRiscV());
    }

    // ---------- CASO 10: PROGRAMA COMPLETO ----------

    @Test
    void programaCompletoGeneraTodoLoEsperado() {
        ResultadoCompilacionPig r = compilar("""
                VARIABILES>
                esto contador : numerus 3;

                MAIOR>
                >> "Iniciando" ;
                dum (contador > 0) {
                    >> contador ;
                    contador = contador - 1;
                } finis;
                si (contador == 0) {
                    >> "Listo" ;
                } finis;
                FINIS;
                """);

        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
        String s = r.getCodigoRiscV();

        assertTrue(s.contains(".data"), s);
        assertTrue(s.contains(".text"), s);
        assertTrue(s.contains("main:"), s);
        assertTrue(s.contains(".asciiz \"Iniciando\""), s);
        assertTrue(s.contains(".asciiz \"Listo\""), s);
        assertTrue(s.contains("sub"), s);
        assertTrue(s.contains("li   a0, 1"), s);   // print_int
        assertTrue(s.contains("li   a0, 4"), s);   // print_string
    }
}