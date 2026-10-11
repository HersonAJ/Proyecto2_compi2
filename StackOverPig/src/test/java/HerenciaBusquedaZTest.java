import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Paso 2: atributos y metodos se buscan tambien en las clases padre. Las clases padre se pasan como externas. */
class HerenciaBusquedaZTest {

    private static final String ANIMAL = """
            public class Animal {
                String nombre;
                public void comer() { println(nombre + " está comiendo."); }
                public void f(double x) { }
            }
            """;

    private TablaSimbolosZ.DefinicionClaseExterna firmas(String codigo) {
        return ServicioCompilacionZ.recolectarFirmas(codigo).orElseThrow();
    }

    private ResultadoCompilacionZ analizar(String codigo, TablaSimbolosZ.DefinicionClaseExterna... externas) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : externas) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa);
    }

    @Test
    void hijoUsaAtributosYMetodosDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void ladrar() {
                        println(nombre + " está ladrando");
                        comer();
                        this.comer();
                        println(this.nombre);
                    }
                }
                """, firmas(ANIMAL));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void unNombreInexistenteSigueFallando() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void ladrar() { println(zzz); }
                }
                """, firmas(ANIMAL));
        assertFalse(r.isExitoso());
        assertFalse(r.getErroresSemanticos().isEmpty());
    }

    @Test
    void herenciaDeVariosNiveles() {
        TablaSimbolosZ.DefinicionClaseExterna perro = firmas("""
                public class Perro extends Animal {
                    public void ladrar() { }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class Cachorro extends Perro {
                    public void jugar() { println(nombre); comer(); ladrar(); }
                }
                """, firmas(ANIMAL), perro);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void metodoSobrescritoNoEsAmbiguo() {
        // f(1) necesita promocion int->double: si el f del padre no se descartara, habria dos candidatas
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    @Override
                    public void f(double x) { }
                    public void g() { f(1); }
                }
                """, firmas(ANIMAL));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void esSubtipoDesdeLaTabla() {
        ResultadoCompilacionZ r = analizar("public class Perro extends Animal { }", firmas(ANIMAL));
        TablaSimbolosZ tabla = r.getTablaSimbolos();
        assertTrue(tabla.esSubtipo("Perro", "Animal"));
        assertTrue(tabla.esSubtipo("Perro", "Perro"));
        assertFalse(tabla.esSubtipo("Animal", "Perro"));
    }

    @Test
    void herenciaCircularNoCuelgaElAnalisis() {
        TablaSimbolosZ.DefinicionClaseExterna a = firmas("public class A extends B { }");
        TablaSimbolosZ.DefinicionClaseExterna b = firmas("public class B extends A { }");
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            ResultadoCompilacionZ r = analizar("""
                    public class C extends A {
                        public void f() { println(zzz); }
                    }
                    """, a, b);
            assertFalse(r.isExitoso());
        });
    }
}