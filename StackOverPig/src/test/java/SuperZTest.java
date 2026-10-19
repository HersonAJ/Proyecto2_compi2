import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paso 6b: super(...) y super.m() en Zetariano.
 *
 * Cada prueba analiza una clase hija con su padre (y a veces el abuelo) como externas.
 * El paquete es "" salvo que la prueba diga otra cosa.
 */
class SuperZTest {

    private TablaSimbolosZ.DefinicionClaseExterna firmas(String codigo) {
        return ServicioCompilacionZ.recolectarFirmas(codigo).orElseThrow();
    }

    private TablaSimbolosZ.DefinicionClaseExterna firmas(String codigo, String paquete) {
        return ServicioCompilacionZ.recolectarFirmas(codigo, paquete).orElseThrow();
    }

    private ResultadoCompilacionZ analizar(String codigo, TablaSimbolosZ.DefinicionClaseExterna... externas) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : externas) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa);
    }

    private ResultadoCompilacionZ analizar(String codigo, String paquete,
                                           TablaSimbolosZ.DefinicionClaseExterna... externas) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : externas) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa, paquete);
    }

    private void assertErrores(int esperados, ResultadoCompilacionZ r) {
        assertEquals(esperados, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    // ---------- CONSTRUCTOR ----------

    @Test
    void superImplicitoConPadreQueTieneConstructorSinParams() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { }
                }
                """, firmas("public class Animal { public Animal() { } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superImplicitoConPadreSinConstructorSinParamsEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { }
                }
                """, firmas("public class Animal { public Animal(int x) { } }"));
        assertErrores(1, r);
    }

    @Test
    void superExplicitoCorrecto() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { super(5); }
                }
                """, firmas("public class Animal { public Animal(int x) { } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superNoEsLaPrimeraInstruccionEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() {
                        int x = 1;
                        super();
                    }
                }
                """, firmas("public class Animal { public Animal() { } }"));
        assertErrores(1, r);
    }

    @Test
    void superEnUnMetodoEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() { super(); }
                }
                """, firmas("public class Animal { public Animal() { } }"));
        assertErrores(1, r);
    }

    @Test
    void superAConstructorPrivadoDelPadreEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { super(); }
                }
                """, firmas("public class Animal { private Animal() { } }"));
        assertErrores(1, r);
    }

    @Test
    void superAConstructorProtegidoDelPadreEsValido() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { super(); }
                }
                """, firmas("public class Animal { protected Animal() { } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superAConstructorDePaqueteEnElMismoPaqueteEsValido() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { super(); }
                }
                """, "zoo",
                firmas("public class Animal { Animal() { } }", "zoo"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superAConstructorDePaqueteEnOtroPaqueteEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { super(); }
                }
                """, "casa",
                firmas("public class Animal { Animal() { } }", "zoo"));
        assertErrores(1, r);
    }

    @Test
    void claseSinConstructoresHeredaElImplicitoDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal { }
                """, firmas("public class Animal { public Animal() { } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void claseSinConstructoresConPadreSinConstructorSinParamsEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal { }
                """, firmas("public class Animal { public Animal(int x) { } }"));
        assertErrores(1, r);
    }

    // ---------- METODO ----------

    @Test
    void superMetodoSobreMetodoDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() { super.comer(); }
                }
                """, firmas("public class Animal { public void comer() { } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superMetodoInexistenteEnElPadreEsError() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() { super.volar(); }
                }
                """, firmas("public class Animal { }"));
        assertErrores(1, r);
    }

    @Test
    void superMetodoDelAbuelo() {
        ResultadoCompilacionZ r = analizar("""
                public class Cachorro extends Perro {
                    public void f() { super.comer(); }
                }
                """,
                firmas("public class Animal { public void comer() { } }"),
                firmas("public class Perro extends Animal { }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superMetodoConSobrecarga() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void g() {
                        super.f(1);
                        super.f("hola");
                    }
                }
                """, firmas("""
                public class Animal {
                    public void f(int x) { }
                    public void f(String s) { }
                }
                """));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void superMetodoComoExpresion() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public int f() { return super.get() + 1; }
                }
                """, firmas("public class Animal { public int get() { return 1; } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }
}