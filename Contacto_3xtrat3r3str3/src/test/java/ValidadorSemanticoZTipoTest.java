import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.*;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.ValidadorSemanticoZ;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorSemanticoZTiposTest {

    @Test
    void resuelveSobrecargaYPromocionSinFalsosPositivos() {
        // public class Persona {
        //     int edad;
        //
        //     public Persona(int edadParametro) {
        //         edad = edadParametro;
        //     }
        //
        //     public int calcularAnioNacimiento(int anioActual) {
        //         return anioActual - edad;
        //     }
        //
        //     public double promedioConBono(double bono) {
        //         return edad + bono;     // int + double -> double, valido por promocion
        //     }
        //
        //     public void probar() {
        //         Persona p1 = new Persona(25);
        //         int anio = p1.calcularAnioNacimiento(2026);
        //         double resultado = p1.promedioConBono(1);   // 1 (int) se promueve a double
        //     }
        // }

        NodoAtributoZ atrEdad = new NodoAtributoZ(2, 4, "int", "edad");

        NodoParametroZ paramEdad = new NodoParametroZ(4, 20, "int", "edadParametro");
        NodoSentencia asignaEdad = new NodoSentencia.Asignacion(5, 8, "=",
                new NodoExpr.Identificador(5, 8, "edad"),
                new NodoExpr.Identificador(5, 15, "edadParametro"));
        NodoConstructor constructor = new NodoConstructor(4, 4, "Persona",
                List.of(paramEdad), List.of(asignaEdad));

        NodoParametroZ paramAnioActual = new NodoParametroZ(8, 40, "int", "anioActual");
        NodoExpr resta = new NodoExpr.Binaria(9, 15, "-",
                new NodoExpr.Identificador(9, 15, "anioActual"),
                new NodoExpr.Identificador(9, 28, "edad"));
        NodoMetodo calcularAnio = new NodoMetodo(8, 4, "calcularAnioNacimiento",
                List.of(paramAnioActual), "int", List.of(new NodoSentencia.Retorno(9, 8, resta)));

        NodoParametroZ paramBono = new NodoParametroZ(12, 30, "double", "bono");
        NodoExpr suma = new NodoExpr.Binaria(13, 15, "+",
                new NodoExpr.Identificador(13, 15, "edad"),
                new NodoExpr.Identificador(13, 22, "bono"));
        NodoMetodo promedioConBono = new NodoMetodo(12, 4, "promedioConBono",
                List.of(paramBono), "double", List.of(new NodoSentencia.Retorno(13, 8, suma)));

        NodoSentencia declaraP1 = new NodoSentencia.DeclaracionVariable(17, 8, "Persona", "p1", 0,
                new NodoExpr.InstanciaObjeto(17, 22, "Persona", List.of(new NodoExpr.LiteralEntero(17, 30, 25))));

        NodoSentencia declaraAnio = new NodoSentencia.DeclaracionVariable(18, 8, "int", "anio", 0,
                new NodoExpr.LlamadaMetodo(18, 20, new NodoExpr.Identificador(18, 20, "p1"),
                        "calcularAnioNacimiento", List.of(new NodoExpr.LiteralEntero(18, 47, 2026))));

        NodoSentencia declaraResultado = new NodoSentencia.DeclaracionVariable(19, 8, "double", "resultado", 0,
                new NodoExpr.LlamadaMetodo(19, 25, new NodoExpr.Identificador(19, 25, "p1"),
                        "promedioConBono", List.of(new NodoExpr.LiteralEntero(19, 45, 1))));

        NodoMetodo probar = new NodoMetodo(16, 4, "probar", List.of(), null,
                List.of(declaraP1, declaraAnio, declaraResultado));

        NodoClase persona = new NodoClase(1, 0, "Persona",
                List.of(atrEdad), List.of(constructor), List.of(calcularAnio, promedioConBono, probar));

        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        ValidadorSemanticoZ validador = new ValidadorSemanticoZ();
        List<ErrorSemantico> errores = validador.analizar(programa);

        errores.forEach(System.out::println);
        assertTrue(errores.isEmpty(), "No se esperaban errores, se obtuvo: " + errores);
    }

    @Test
    void detectaAsignacionDeTipoIncompatible() {
        // public class Persona {
        //     public void probar() {
        //         int edad = "veinticinco";   // String a int, debe fallar
        //     }
        // }

        NodoSentencia declaracionInvalida = new NodoSentencia.DeclaracionVariable(3, 8, "int", "edad", 0,
                new NodoExpr.LiteralCadena(3, 19, "veinticinco"));

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null, List.of(declaracionInvalida));
        NodoClase persona = new NodoClase(1, 0, "Persona", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        ValidadorSemanticoZ validador = new ValidadorSemanticoZ();
        List<ErrorSemantico> errores = validador.analizar(programa);

        errores.forEach(System.out::println);

        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e -> e.categoria().equals("Tipo incompatible") && e.linea() == 3));
    }
}