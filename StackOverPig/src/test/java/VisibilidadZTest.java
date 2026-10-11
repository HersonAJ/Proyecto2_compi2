
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Paso 3: las cuatro reglas de acceso. Animal vive en el paquete "zoo". */
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

    private TablaSimbolosZ.DefinicionClaseExterna animalEnZoo() {
        return ServicioCompilacionZ.recolectarFirmas(ANIMAL, "zoo").orElseThrow();
    }

    private ResultadoCompilacionZ analizar(String codigo, String paquete,
                                           TablaSimbolosZ.DefinicionClaseExterna... externas) {
        Map<String, TablaSimbolosZ.DefinicionClaseExterna> mapa = new LinkedHashMap<>();
        for (TablaSimbolosZ.DefinicionClaseExterna e : externas) mapa.put(e.nombre(), e);
        return new ServicioCompilacionZ().analizar(codigo, mapa, paquete);
    }

    @Test
    void hijoEnElMismoPaqueteVeTodoMenosLoPrivado() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(raza); println(nombre); println(publico);
                        proteger(); interno(); abierto();
                    }
                }
                """, "zoo", animalEnZoo());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void hijoEnOtroPaqueteVeProtectedYPublic() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(raza); println(publico);
                        proteger(); abierto();
                    }
                }
                """, "casa", animalEnZoo());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void hijoEnOtroPaqueteNoVeLoDePaquete() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(nombre);
                        interno();
                    }
                }
                """, "casa", animalEnZoo());
        assertEquals(2, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    @Test
    void privadoDelPadreNoSeVeDesdeElHijoYElMensajeNoDiceNoDeclarado() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        println(secreto);
                        ocultar();
                    }
                }
                """, "zoo", animalEnZoo());
        assertEquals(2, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
        assertTrue(r.getErroresSemanticos().toString().contains("Acceso no permitido"),
                r.getErroresSemanticos().toString());
    }

    @Test
    void claseNoRelacionadaEnElMismoPaquete() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        println(a.raza);
                        println(a.nombre);
                        println(a.secreto);
                    }
                }
                """, "zoo", animalEnZoo());
        assertEquals(1, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    @Test
    void claseNoRelacionadaEnOtroPaquete() {
        ResultadoCompilacionZ r = analizar("""
                public class Visitante {
                    public void f() {
                        Animal a = new Animal();
                        println(a.raza);
                        println(a.nombre);
                        println(a.publico);
                    }
                }
                """, "casa", animalEnZoo());
        assertEquals(2, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    @Test
    void privadoSePuedeUsarDentroDeLaMismaClaseEntreInstancias() {
        ResultadoCompilacionZ r = analizar("""
                public class Cuenta {
                    private int saldo;
                    public Cuenta() { }
                    public boolean mayor(Cuenta otra) { return saldo > otra.saldo; }
                }
                """, "");
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void variableLocalNoSeBloqueaPorElAtributoPrivadoDelPadre() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Animal {
                    public void f() {
                        String secreto = "x";
                        println(secreto);
                    }
                }
                """, "zoo", animalEnZoo());
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void constructorPrivadoSoloSePuedeUsarDentroDeSuClase() {
        TablaSimbolosZ.DefinicionClaseExterna singleton = ServicioCompilacionZ.recolectarFirmas("""
                public class Singleton {
                    private Singleton() { }
                }
                """, "util").orElseThrow();

        ResultadoCompilacionZ fuera = analizar("""
                public class Cliente {
                    public void f() { Singleton s = new Singleton(); }
                }
                """, "util", singleton);
        assertEquals(1, fuera.getErroresSemanticos().size(), fuera.getErroresSemanticos().toString());

        ResultadoCompilacionZ dentro = analizar("""
                public class Singleton {
                    private Singleton() { }
                    public Singleton crear() { return new Singleton(); }
                }
                """, "util");
        assertTrue(dentro.isExitoso(), dentro.getErroresSemanticos().toString());
    }
}