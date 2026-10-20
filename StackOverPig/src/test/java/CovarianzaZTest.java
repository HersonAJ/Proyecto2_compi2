import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regla del proyecto 2: una sobrescritura puede devolver el mismo tipo que el padre
 * o un subtipo de el (retorno covariante). La firma (nombre + parametros) no cambia.
 */
class CovarianzaZTest {

    private static final String ANIMAL = """
            public class Animal {
                public Animal() { }
            }
            """;

    private static final String PERRO = """
            public class Perro extends Animal {
                public Perro() { }
            }
            """;

    private static final String GATO = """
            public class Gato extends Animal {
                public Gato() { }
            }
            """;

    private static final String VETERINARIO = """
            public class Veterinario {
                public Animal adoptar() { return new Animal(); }
                public Animal rescatar() { return new Animal(); }
                public Animal examinar() { return new Animal(); }
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

    private void assertErrores(int esperados, ResultadoCompilacionZ r) {
        assertEquals(esperados, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    // ---------- COVARIANZA VALIDA ----------

    @Test
    void sobrescrituraConMismoTipoDeRetornoEsValida() {
        ResultadoCompilacionZ r = analizar("""
                public class VeterinarioEspecialista extends Veterinario {
                    @Override
                    public Animal adoptar() { return new Animal(); }
                }
                """,
                firmas(ANIMAL), firmas(VETERINARIO));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void sobrescrituraConSubtipoDeRetornoEsValida() {
        ResultadoCompilacionZ r = analizar("""
                public class VeterinarioEspecialista extends Veterinario {
                    @Override
                    public Perro adoptar() { return new Perro(); }
                }
                """,
                firmas(ANIMAL), firmas(PERRO), firmas(VETERINARIO));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void sobrescrituraConSubtipoIndirectoEsValida() {
        // Cachorro -> Perro -> Animal
        TablaSimbolosZ.DefinicionClaseExterna cachorro = firmas("""
                public class Cachorro extends Perro {
                    public Cachorro() { }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class VeterinarioEspecialista extends Veterinario {
                    @Override
                    public Cachorro adoptar() { return new Cachorro(); }
                }
                """,
                firmas(ANIMAL), firmas(PERRO), cachorro, firmas(VETERINARIO));
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // ---------- COVARIANZA INVALIDA ----------

    @Test
    void sobrescrituraConSuperclaseDeRetornoEsError() {
        // El padre devuelve Perro, el hijo devuelve Animal (su padre). No es covarianza valida.
        TablaSimbolosZ.DefinicionClaseExterna veterinarioPerro = firmas("""
                public class VeterinarioPerro {
                    public Perro adoptar() { return new Perro(); }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class VeterinarioEspecialista extends VeterinarioPerro {
                    @Override
                    public Animal adoptar() { return new Animal(); }
                }
                """,
                firmas(ANIMAL), firmas(PERRO), veterinarioPerro);
        assertErrores(1, r);
    }

    @Test
    void sobrescrituraConTipoHermanoEsError() {
        // El padre devuelve Perro, el hijo devuelve Gato. No hay relacion.
        TablaSimbolosZ.DefinicionClaseExterna veterinarioPerro = firmas("""
                public class VeterinarioPerro {
                    public Perro adoptar() { return new Perro(); }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class VeterinarioEspecialista extends VeterinarioPerro {
                    @Override
                    public Gato adoptar() { return new Gato(); }
                }
                """,
                firmas(ANIMAL), firmas(PERRO), firmas(GATO), veterinarioPerro);
        assertErrores(1, r);
    }

    @Test
    void sobrescrituraConTipoPrimitivoDistintoEsError() {
        TablaSimbolosZ.DefinicionClaseExterna base = firmas("""
                public class Base {
                    public int obtener() { return 0; }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class Hija extends Base {
                    @Override
                    public double obtener() { return 0.0; }
                }
                """, base);
        assertErrores(1, r);
    }

    // ---------- VOID ----------

    @Test
    void sobrescrituraVoidConVoidEsValida() {
        TablaSimbolosZ.DefinicionClaseExterna base = firmas("""
                public class Base {
                    public void ejecutar() { }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class Hija extends Base {
                    @Override
                    public void ejecutar() { }
                }
                """, base);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void sobrescrituraVoidConTipoEsError() {
        TablaSimbolosZ.DefinicionClaseExterna base = firmas("""
                public class Base {
                    public void ejecutar() { }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class Hija extends Base {
                    @Override
                    public int ejecutar() { return 0; }
                }
                """, base);
        assertErrores(1, r);
    }

    @Test
    void sobrescrituraTipoConVoidEsError() {
        TablaSimbolosZ.DefinicionClaseExterna base = firmas("""
                public class Base {
                    public int ejecutar() { return 0; }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class Hija extends Base {
                    @Override
                    public void ejecutar() { }
                }
                """, base);
        assertErrores(1, r);
    }
}