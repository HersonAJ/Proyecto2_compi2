import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paso 5: un objeto de una clase hija se puede usar donde se espera su clase padre.
 * Las clases de la jerarquia no declaran constructores a proposito: deben poder instanciarse
 * con el constructor por defecto implicito.
 */
class SubtipadoZTest {

    private TablaSimbolosZ.DefinicionClaseExterna[] jerarquia() {
        return new TablaSimbolosZ.DefinicionClaseExterna[] {
                firmas("public class Animal { }"),
                firmas("public class Perro extends Animal { public void ladrar() { } }"),
                firmas("public class Gato extends Animal { }"),
                firmas("public class Cachorro extends Perro { }")
        };
    }

    private TablaSimbolosZ.DefinicionClaseExterna firmas(String codigo) {
        return ServicioCompilacionZ.recolectarFirmas(codigo).orElseThrow();
    }

    private ResultadoCompilacionZ analizar(String codigo) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : jerarquia()) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa);
    }

    private void assertErrores(int esperados, ResultadoCompilacionZ r) {
        assertEquals(esperados, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    @Test
    void unaSubclaseSeAsignaAUnaVariableDeSuPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Zoo {
                    public void f() {
                        Animal a = new Perro();
                        Animal b = new Cachorro();
                        a = b;
                    }
                }
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void elPadreNoSeAsignaAUnHijoNiHermanosEntreSi() {
        assertErrores(1, analizar("public class Zoo { public void f() { Perro p = new Animal(); } }"));
        assertErrores(1, analizar("public class Zoo { public void f() { Perro p = new Gato(); } }"));
    }

    @Test
    void argumentosYRetornoAceptanSubtipos() {
        ResultadoCompilacionZ r = analizar("""
                public class Veterinario {
                    public void atender(Animal a) { }
                    public Animal elegir() { return new Perro(); }
                    public void f() {
                        atender(new Perro());
                        atender(new Cachorro());
                        Animal a = elegir();
                    }
                }
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void entreSobrecargasAplicablesGanaLaMasEspecifica() {
        ResultadoCompilacionZ r = analizar("""
                public class Clasificador {
                    public void clasificar(Animal a) { }
                    public void clasificar(Perro p) { }
                    public void f() {
                        clasificar(new Cachorro());
                        clasificar(new Gato());
                        clasificar(null);
                    }
                }
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void siNingunaEsMasEspecificaSigueSiendoAmbigua() {
        ResultadoCompilacionZ r = analizar("""
                public class Raro {
                    public void g(Animal a, Perro p) { }
                    public void g(Perro p, Animal a) { }
                    public void f() { g(new Perro(), new Perro()); }
                }
                """);
        assertErrores(1, r);
    }

    @Test
    void igualdadSoloEntreClasesDeLaMismaJerarquia() {
        ResultadoCompilacionZ r = analizar("""
                public class Cmp {
                    public void f() {
                        Animal a = new Perro();
                        Perro p = new Perro();
                        Gato g = new Gato();
                        boolean x = a == p;
                        boolean y = p == g;
                    }
                }
                """);
        assertErrores(1, r);   // solo p == g
    }

    @Test
    void elTipoDeclaradoManda() {
        ResultadoCompilacionZ r = analizar("""
                public class Zoo {
                    public void f() {
                        Perro p = new Perro();
                        p.ladrar();
                        Animal a = new Perro();
                        a.ladrar();
                    }
                }
                """);
        assertErrores(1, r);   // solo a.ladrar(): Animal no tiene ladrar()
    }

    @Test
    void ternarioConHermanosTomaElAncestroComun() {
        assertTrue(analizar("""
                public class Zoo {
                    public void f() { Animal a = true ? new Perro() : new Gato(); }
                }
                """).isExitoso());
        assertErrores(1, analizar("""
                public class Zoo {
                    public void f() { Perro p = true ? new Perro() : new Gato(); }
                }
                """));
    }

    @Test
    void constructorPorDefectoSoloSiLaClaseNoDeclaraNinguno() {
        assertErrores(1, analizar("""
                public class Punto {
                    public Punto(int x) { }
                    public void f() { Punto p = new Punto(); }
                }
                """));
        assertTrue(analizar("""
                public class Punto {
                    public Punto(int x) { }
                    public void f() { Punto p = new Punto(1); }
                }
                """).isExitoso());
    }
}