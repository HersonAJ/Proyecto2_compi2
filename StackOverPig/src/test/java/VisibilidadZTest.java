import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de visibilidad del proyecto 2:
 *   public     -> cualquier clase.
 *   PAQUETE    -> como private: solo la clase declarante.
 *   protected  -> la clase declarante y sus subclases.
 *   private    -> solo la clase declarante.
 */
class VisibilidadZTest {

    private static final String ANIMAL = """
            public class Animal {
                private String secreto;
                protected String raza;
                String nombre;
                public String publico;
                public Animal() { }
                private void ocultar() { }
                protected void proteger() { }
                void interno() { }
                public void abierto() { }
            }
            """;

    private TablaSimbolosZ.DefinicionClaseExterna animal() {
        return ServicioCompilacionZ.recolectarFirmas(ANIMAL).orElseThrow();
    }

    private ResultadoCompilacionZ analizar(String codigo, TablaSimbolosZ.DefinicionClaseExterna... externas) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : externas) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa);
    }

    private void assertErrores(int esperados, ResultadoCompilacionZ r) {
        assertEquals(esperados, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    // ---------- HIJA ----------

    @Test
    void hijaVePublicYProtegidoPeroNoDefaultNiPrivado() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(raza); println(publico);
                        proteger(); abierto();
                    }
                }
                """, animal());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void hijaNoVeDefaultNiPrivado() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(nombre);      // default: error
                        println(secreto);     // private: error
                        interno();            // default: error
                        ocultar();            // private: error
                    }
                }
                """, animal());
        assertErrores(4, r);
        assertTrue(r.getErroresSemanticos().toString().contains("Acceso no permitido"));
    }

    @Test
    void nietoVePublicYProtegido() {
        ResultadoCompilacionZ r = analizar("""
                public class Cachorro extends Perro {
                    public void f() {
                        println(raza); println(publico);
                        proteger(); abierto();
                    }
                }
                """,
                animal(),
                ServicioCompilacionZ.recolectarFirmas("public class Perro extends Animal { }").orElseThrow());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // ---------- CLASE NO RELACIONADA ----------

    @Test
    void claseNoRelacionadaSoloVePublic() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        println(a.publico);
                        println(a.nombre);   // default: error
                        println(a.raza);     // protected: error
                        println(a.secreto);  // private: error
                    }
                }
                """, animal());
        assertErrores(3, r);
    }

    @Test
    void claseNoRelacionadaNoUsaMetodosNoPublicos() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        a.abierto();
                        a.interno();    // default: error
                        a.proteger();   // protected: error
                        a.ocultar();    // private: error
                    }
                }
                """, animal());
        assertErrores(3, r);
    }

    // ---------- PRIVADO DENTRO DE LA MISMA CLASE ----------

    @Test
    void privadoSePuedeUsarDentroDeLaMismaClaseEntreInstancias() {
        ResultadoCompilacionZ r = analizar("""
                public class Cuenta {
                    private int saldo;
                    public Cuenta() { }
                    public boolean mayor(Cuenta otra) { return saldo > otra.saldo; }
                }
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // ---------- DEFAULT ES COMO PRIVATE ----------

    @Test
    void defaultEsComoPrivadoDesdeOtraClase() {
        ResultadoCompilacionZ r = analizar("""
                public class Otra {
                    public void f() {
                        Animal a = new Animal();
                        println(a.nombre);
                    }
                }
                """, animal());
        assertErrores(1, r);
    }

    @Test
    void defaultSePuedeUsarDentroDeLaMismaClase() {
        ResultadoCompilacionZ r = analizar("""
                public class Animal {
                    String nombre;
                    public Animal() { }
                    public void f() { println(nombre); }
                }
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // ---------- PROTEGIDO EN HIJA ----------

    @Test
    void hijaAccedeAAtributoProtegidoDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        raza = "Labrador";
                        println(raza);
                    }
                }
                """, animal());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void hijaAccedeAMetodoProtegidoDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        proteger();
                    }
                }
                """, animal());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // ---------- VARIABLE LOCAL ----------

    @Test
    void variableLocalNoSeBloqueaPorElAtributoPrivadoDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        String secreto = "x";
                        println(secreto);
                    }
                }
                """, animal());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }
}