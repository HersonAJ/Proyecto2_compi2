import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Paso 4: validacion de la herencia y de @Override. */
class HerenciaValidacionZTest {

    private static final String ANIMAL = """
            public class Animal {
                public void comer() { }
                public int edad() { return 1; }
                protected void dormir() { }
                private void secreto() { }
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

    private ResultadoCompilacionZ hijoDeAnimal(String cuerpo) {
        return analizar("public class Perro extends Animal {\n" + cuerpo + "\n}", firmas(ANIMAL));
    }

    private void assertErrores(int esperados, ResultadoCompilacionZ r) {
        assertEquals(esperados, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    // SUPERCLASE Y CICLOS

    @Test
    void superclaseInexistenteSeReportaUnaSolaVez() {
        ResultadoCompilacionZ r = analizar("""
                public class Perro extends Fantasma {
                    @Override
                    public void comer() { }
                }
                """);
        assertErrores(1, r);   // no se suman errores en cascada por los metodos
    }

    @Test
    void unaClaseNoPuedeHeredarDeSiMisma() {
        assertErrores(1, analizar("public class A extends A { }"));
    }

    @Test
    void herenciaCircularEntreClases() {
        ResultadoCompilacionZ r = analizar("public class C extends A { }",
                firmas("public class A extends B { }"),
                firmas("public class B extends A { }"));
        assertErrores(1, r);
    }

    // @OVERRIDE

    @Test
    void overrideCorrecto() {
        assertTrue(hijoDeAnimal("@Override public void comer() { }").isExitoso());
    }

    @Test
    void overrideSinNadaQueSobrescribir() {
        assertErrores(1, hijoDeAnimal("@Override public void volar() { }"));
    }

    @Test
    void sobrescribirSinOverrideEsErrorMientrasLaReglaSeaObligatoria() {
        // Si la encargada dice que no es obligatorio, cambia OVERRIDE_OBLIGATORIO y esta prueba
        assertErrores(1, hijoDeAnimal("public void comer() { }"));
    }

    @Test
    void sobrecargaNoEsSobrescritura() {
        assertTrue(hijoDeAnimal("public void comer(int x) { }").isExitoso());
        assertErrores(1, hijoDeAnimal("@Override public void comer(int x) { }"));
    }

    @Test
    void overrideEnElNieto() {
        TablaSimbolosZ.DefinicionClaseExterna perro = firmas("""
                public class Perro extends Animal {
                    @Override
                    public void comer() { }
                }
                """);
        ResultadoCompilacionZ r = analizar("""
                public class Cachorro extends Perro {
                    @Override
                    public void comer() { }
                    @Override
                    public void dormir() { }
                }
                """, firmas(ANIMAL), perro);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    // REGLAS DE LA SOBRESCRITURA

    @Test
    void elTipoDeRetornoDebeSerElMismo() {
        assertErrores(1, hijoDeAnimal("@Override public void edad() { }"));
        assertTrue(hijoDeAnimal("@Override public int edad() { return 2; }").isExitoso());
    }

    @Test
    void noSePuedeReducirLaVisibilidadPeroSiAmpliarla() {
        assertErrores(1, hijoDeAnimal("@Override protected void comer() { }"));
        assertTrue(hijoDeAnimal("@Override public void dormir() { }").isExitoso());
    }

    @Test
    void unMetodoPrivadoDelPadreNoSeSobrescribe() {
        // sin @Override es un metodo nuevo; con @Override no hay nada que sobrescribir
        assertTrue(hijoDeAnimal("public void secreto() { }").isExitoso());
        assertErrores(1, hijoDeAnimal("@Override public void secreto() { }"));
    }
}