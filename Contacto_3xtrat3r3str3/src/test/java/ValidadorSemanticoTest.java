
import com.example.contacto_3xtrat3r3str3.y.ast.*;
import com.example.contacto_3xtrat3r3str3.y.semantica.ValidadorSemantico;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorSemanticoTest {

    @Test
    void detectaIdentificadorNoDeclaradoYRomperFueraDeCiclo() {
        // definir probar():
        //     imprimir(x)      // x nunca fue declarada
        //     romper           // fuera de cualquier ciclo

        NodoSentencia imprimirX = new NodoSentencia.Imprimir(
                2, 4, new NodoExpr.Identificador(2, 13, "x"));

        NodoSentencia romperSuelto = new NodoSentencia.Romper(3, 4);

        NodoFuncion.Funcion funcion = new NodoFuncion.Funcion(
                1, 0, "probar",
                List.of(),
                null,
                List.of(imprimirX, romperSuelto));

        NodoPrograma.Programa programa = new NodoPrograma.Programa(
                1, 0, List.of(), List.of(funcion));

        ValidadorSemantico validador = new ValidadorSemantico();
        List<ErrorSemantico> errores = validador.analizar(programa);

        errores.forEach(System.out::println);

        assertEquals(2, errores.size(), "Se esperaban exactamente 2 errores: " + errores);

        assertTrue(errores.stream().anyMatch(e ->
                e.categoria().equals("Identificador no declarado") && e.linea() == 2));

        assertTrue(errores.stream().anyMatch(e ->
                e.categoria().equals("Corrupcion de flujo") && e.linea() == 3));

    }

    @Test
    void noReportaErroresConCodigoValido() {
        // definir probar(entero a):
        //     entero b = a
        //     imprimir(b)

        NodoSentencia declaracionB = new NodoSentencia.DeclaracionVariable(
                2, 4, "entero", "b", new NodoExpr.Identificador(2, 16, "a"));

        NodoSentencia imprimirB = new NodoSentencia.Imprimir(
                3, 4, new NodoExpr.Identificador(3, 13, "b"));

        NodoParametro.Parametro parametroA = new NodoParametro.Parametro(
                1, 0, "entero", null, "a", false, false);

        NodoFuncion.Funcion funcion = new NodoFuncion.Funcion(
                1, 0, "probar",
                List.of(parametroA),
                null,
                List.of(declaracionB, imprimirB));

        NodoPrograma.Programa programa = new NodoPrograma.Programa(
                1, 0, List.of(), List.of(funcion));

        ValidadorSemantico validador = new ValidadorSemantico();
        List<ErrorSemantico> errores = validador.analizar(programa);

        assertTrue(errores.isEmpty(), "No se esperaban errores, se obtuvo: " + errores);
    }
}