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
 * Reglas de visibilidad del proyecto 2:
 *   public     -> cualquier clase.
 *   PAQUETE    -> cualquier clase (no hay paquetes).
 *   protected  -> la clase declarante y sus subclases.
 *   private    -> solo la clase declarante.
 */
class SuperZTest {

    private TablaSimbolosZ.DefinicionClaseExterna firmas(String codigo) {
        return ServicioCompilacionZ.recolectarFirmas(codigo).orElseThrow();
    }

    private ResultadoCompilacionZ analizar(String codigo, TablaSimbolosZ.DefinicionClaseExterna... externas) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : externas) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa);
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
    void superAConstructorSinModificadorEsValidoDesdeCualquierClase() {
        // Sin paquetes, un constructor sin modificador (PAQUETE) es accesible desde cualquier subclase.
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public Perro() { super(); }
                }
                """, firmas("public class Animal { Animal() { } }"));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
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