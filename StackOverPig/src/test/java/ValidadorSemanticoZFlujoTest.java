import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.*;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.ValidadorSemanticoZ;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorSemanticoZFlujoTest {

    @Test
    void permiteFallthroughEnSwitchSinBreak() {
        // public class Vacia {
        //     public void probar() {
        //         int opcion = 2;
        //         switch (opcion) {
        //             case 1:
        //                 println(1);
        //             case 2:
        //                 println(2);
        //             default:
        //                 println(3);
        //         }
        //     }
        // }

        NodoSentencia declaraOpcion = new NodoSentencia.DeclaracionVariable(
                3, 8, "int", "opcion", 0, new NodoExpr.LiteralEntero(3, 20, 2));

        NodoSentencia.CasoSwitch caso1 = new NodoSentencia.CasoSwitch(5, 12,
                new NodoExpr.LiteralEntero(5, 17, 1),
                List.of(new NodoSentencia.Imprimir(6, 16, true, new NodoExpr.LiteralEntero(6, 24, 1))));

        NodoSentencia.CasoSwitch caso2 = new NodoSentencia.CasoSwitch(7, 12,
                new NodoExpr.LiteralEntero(7, 17, 2),
                List.of(new NodoSentencia.Imprimir(8, 16, true, new NodoExpr.LiteralEntero(8, 24, 2))));

        NodoSentencia.CasoDefault casoDefault = new NodoSentencia.CasoDefault(9, 12,
                List.of(new NodoSentencia.Imprimir(10, 16, true, new NodoExpr.LiteralEntero(10, 24, 3))));

        NodoSentencia sw = new NodoSentencia.Switch(4, 8,
                new NodoExpr.Identificador(4, 16, "opcion"),
                List.of(caso1, caso2), casoDefault);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null, List.of(declaraOpcion, sw));
        NodoClase vacia = new NodoClase(1, 0, "Vacia", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, vacia);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertTrue(errores.isEmpty(), "El fallthrough sin 'break' no debe reportar error: " + errores);
    }

    @Test
    void permiteForVacioConBreakAdentro() {
        // public class Vacia {
        //     public void probar() {
        //         for (;;) {
        //             break;
        //         }
        //     }
        // }

        NodoSentencia romper = new NodoSentencia.Romper(4, 12);
        NodoSentencia.CicloPara ciclo = new NodoSentencia.CicloPara(
                3, 8, null, null, null, List.of(romper));

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null, List.of(ciclo));
        NodoClase vacia = new NodoClase(1, 0, "Vacia", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, vacia);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertTrue(errores.isEmpty(), "'for(;;)' con break adentro no debe reportar error: " + errores);
    }

    @Test
    void permiteDoWhileConContinueAdentro() {
        // public class Vacia {
        //     public void probar() {
        //         int contador = 0;
        //         do {
        //             contador++;
        //             continue;
        //         } while (contador < 5);
        //     }
        // }

        NodoSentencia declaraContador = new NodoSentencia.DeclaracionVariable(
                3, 8, "int", "contador", 0, new NodoExpr.LiteralEntero(3, 24, 0));

        NodoExpr incremento = new NodoExpr.IncrementoDecremento(
                5, 12, "++", new NodoExpr.Identificador(5, 12, "contador"), false);
        NodoSentencia incrementoSentencia = new NodoSentencia.ExpresionComoSentencia(5, 12, incremento);
        NodoSentencia continuar = new NodoSentencia.Continuar(6, 12);

        NodoExpr condicion = new NodoExpr.Binaria(7, 17, "<",
                new NodoExpr.Identificador(7, 17, "contador"),
                new NodoExpr.LiteralEntero(7, 28, 5));

        NodoSentencia.CicloHacerMientras ciclo = new NodoSentencia.CicloHacerMientras(
                4, 8, List.of(incrementoSentencia, continuar), condicion);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(),
                null, List.of(declaraContador, ciclo));

        NodoClase vacia = new NodoClase(1, 0, "Vacia", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, vacia);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertTrue(errores.isEmpty(), "'do-while' con continue adentro no debe reportar error: " + errores);
    }

    @Test
    void detectaContinueDentroDeSwitchSinCiclo() {
        // public class Vacia {
        //     public void probar() {
        //         int x = 1;
        //         switch (x) {
        //             case 1:
        //                 continue;   // invalido: switch no cuenta como ciclo para 'continue'
        //         }
        //     }
        // }

        NodoSentencia declaraX = new NodoSentencia.DeclaracionVariable(
                3, 8, "int", "x", 0, new NodoExpr.LiteralEntero(3, 16, 1));

        NodoSentencia.CasoSwitch caso1 = new NodoSentencia.CasoSwitch(5, 12,
                new NodoExpr.LiteralEntero(5, 17, 1),
                List.of(new NodoSentencia.Continuar(6, 16)));

        NodoSentencia sw = new NodoSentencia.Switch(4, 8,
                new NodoExpr.Identificador(4, 16, "x"),
                List.of(caso1), null);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null, List.of(declaraX, sw));
        NodoClase vacia = new NodoClase(1, 0, "Vacia", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, vacia);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e -> e.categoria().equals("Flujo invalido") && e.linea() == 6));
    }
}