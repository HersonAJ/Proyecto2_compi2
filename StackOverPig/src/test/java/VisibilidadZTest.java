import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglas de visibilidad con la definicion del proyecto 2:
 *   public     -> cualquier clase.
 *   PAQUETE    -> cualquier clase (no hay paquetes).
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
    void hijaVeTodoMenosLoPrivado() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(raza); println(nombre); println(publico);
                        proteger(); interno(); abierto();
                    }
                }
                """, animal());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void hijaNoVeLoPrivado() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(secreto);
                        ocultar();
                    }
                }
                """, animal());
        assertErrores(2, r);
        assertTrue(r.getErroresSemanticos().toString().contains("Acceso no permitido"));
    }

    @Test
    void nietoVeLoProtegidoYDePaquete() {
        ResultadoCompilacionZ r = analizar("""
                public class Cachorro extends Perro {
                    public void f() {
                        println(raza); println(nombre);
                        proteger(); interno();
                    }
                }
                """,
                animal(),
                ServicioCompilacionZ.recolectarFirmas("public class Perro extends Animal { }").orElseThrow());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // ---------- CLASE NO RELACIONADA ----------

    @Test
    void claseNoRelacionadaVePublicYDePaquetePeroNoProtegidoNiPrivado() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        println(a.publico);
                        println(a.nombre);
                        println(a.raza);
                        println(a.secreto);
                    }
                }
                """, animal());
        // raza (protected) y secreto (private) no son accesibles desde Visitante
        assertErrores(2, r);
    }

    @Test
    void claseNoRelacionadaNoUsaMetodosProtegidosNiPrivados() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        a.abierto();
                        a.interno();
                        a.proteger();
                        a.ocultar();
                    }
                }
                """, animal());
        // proteger y ocultar no son accesibles desde Visitante
        assertErrores(2, r);
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

    // ---------- SIN MODIFICADOR ----------

    @Test
    void sinModificadorEsAccesibleDesdeCualquierClase() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        println(a.nombre);
                        a.interno();
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