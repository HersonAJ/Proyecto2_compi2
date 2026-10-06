import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.*;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.ValidadorSemanticoZ;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorSemanticoZAlcanceProfundoTest {

    @Test
    void detectaLlamadaAMetodoInexistente() {
        // public class Persona {
        //     public void probar() {
        //         metodoQueNoExiste();
        //     }
        // }

        NodoExpr llamada = new NodoExpr.LlamadaFuncion(3, 8, "metodoQueNoExiste", List.of());
        NodoSentencia sentencia = new NodoSentencia.ExpresionComoSentencia(3, 8, llamada);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null, List.of(sentencia));
        NodoClase persona = new NodoClase(1, 0, "Persona", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e ->
                e.categoria().equals("Metodo no declarado") && e.linea() == 3));
    }

    @Test
    void detectaLlamadaAMetodoSobreObjetoInexistenteEnConstructor() {
        // public class Persona {
        //     public Persona() {
        //         Persona(1, 2, 3);   // demasiados argumentos, sin sobrecarga que coincida
        //     }
        // }

        NodoExpr llamada = new NodoExpr.LlamadaFuncion(3, 8, "Persona", List.of(
                new NodoExpr.LiteralEntero(3, 16, 1),
                new NodoExpr.LiteralEntero(3, 19, 2),
                new NodoExpr.LiteralEntero(3, 22, 3)));
        NodoSentencia sentencia = new NodoSentencia.ExpresionComoSentencia(3, 8, llamada);

        NodoConstructor constructor = new NodoConstructor(2, 4, "Persona", List.of(), List.of(sentencia));
        NodoClase persona = new NodoClase(1, 0, "Persona", List.of(), List.of(constructor), List.of());
        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        //'Persona' no esta registrada como metodo (es un constructor), asi que tabla.getMetodos("Persona") viene vacia
        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e -> e.categoria().equals("Metodo no declarado")));
    }

    @Test
    void detectaAccesoAAtributoSobreValorQueNoEsObjeto() {
        // public class Persona {
        //     public void probar() {
        //         int numero = 5;
        //         int x = numero.atributo;
        //     }
        // }

        NodoSentencia declaraNumero = new NodoSentencia.DeclaracionVariable(
                3, 8, "int", "numero", 0, new NodoExpr.LiteralEntero(3, 21, 5));

        NodoExpr acceso = new NodoExpr.AccesoAtributo(4, 17,
                new NodoExpr.Identificador(4, 17, "numero"), "atributo");
        NodoSentencia declaraX = new NodoSentencia.DeclaracionVariable(4, 8, "int", "x", 0, acceso);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null,
                List.of(declaraNumero, declaraX));
        NodoClase persona = new NodoClase(1, 0, "Persona", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e ->
                e.categoria().equals("Acceso invalido") && e.linea() == 4));
    }

    @Test
    void detectaLlamadaAMetodoSobreValorQueNoEsObjeto() {
        // public class Persona {
        //     public void probar() {
        //         int numero = 5;
        //         numero.saludar();
        //     }
        // }

        NodoSentencia declaraNumero = new NodoSentencia.DeclaracionVariable(
                3, 8, "int", "numero", 0, new NodoExpr.LiteralEntero(3, 21, 5));

        NodoExpr llamada = new NodoExpr.LlamadaMetodo(4, 8,
                new NodoExpr.Identificador(4, 8, "numero"), "saludar", List.of());
        NodoSentencia sentencia = new NodoSentencia.ExpresionComoSentencia(4, 8, llamada);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null,
                List.of(declaraNumero, sentencia));
        NodoClase persona = new NodoClase(1, 0, "Persona", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e ->
                e.categoria().equals("Acceso invalido") && e.linea() == 4));
    }

    @Test
    void detectaIndiceFueraDeRangoSoloConTamanoYIndiceLiterales() {
        // public class Persona {
        //     public void probar() {
        //         int[] numeros = {10, 20, 30};   // tamano conocido: 3
        //         int a = numeros[0];             // valido
        //         int b = numeros[5];             // fuera de rango, debe fallar
        //     }
        // }

        NodoExpr lista = new NodoExpr.ListaLiteral(3, 24, List.of(
                new NodoExpr.LiteralEntero(3, 25, 10),
                new NodoExpr.LiteralEntero(3, 29, 20),
                new NodoExpr.LiteralEntero(3, 33, 30)));
        NodoSentencia declaraNumeros = new NodoSentencia.DeclaracionVariable(3, 8, "int", "numeros", 1, lista);

        NodoExpr accesoValido = new NodoExpr.AccesoArray(4, 16,
                new NodoExpr.Identificador(4, 16, "numeros"), new NodoExpr.LiteralEntero(4, 24, 0));
        NodoSentencia declaraA = new NodoSentencia.DeclaracionVariable(4, 8, "int", "a", 0, accesoValido);

        NodoExpr accesoInvalido = new NodoExpr.AccesoArray(5, 16,
                new NodoExpr.Identificador(5, 16, "numeros"), new NodoExpr.LiteralEntero(5, 24, 5));
        NodoSentencia declaraB = new NodoSentencia.DeclaracionVariable(5, 8, "int", "b", 0, accesoInvalido);

        NodoMetodo probar = new NodoMetodo(2, 4, "probar", List.of(), null,
                List.of(declaraNumeros, declaraA, declaraB));
        NodoClase persona = new NodoClase(1, 0, "Persona", List.of(), List.of(), List.of(probar));
        NodoPrograma programa = new NodoPrograma(1, 0, persona);

        List<ErrorSemantico> errores = new ValidadorSemanticoZ().analizar(programa);

        errores.forEach(System.out::println);
        assertEquals(1, errores.size(), "Se esperaba exactamente 1 error: " + errores);
        assertTrue(errores.stream().anyMatch(e ->
                e.categoria().equals("Indice fuera de rango") && e.linea() == 5));
    }
}