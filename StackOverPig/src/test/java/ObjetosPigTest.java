import com.example.stack_over_pig.piglatin.service.ResultadoCompilacionPig;
import com.example.stack_over_pig.piglatin.service.ServicioCompilacionPig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Paso 6b en Pig: novus ... : Tipo y null : Tipo.
 *
 * Se crea un proyecto temporal con los .z y .y importados, y se analiza el .pig
 * tal como lo haria la UI (pasando la carpeta raiz).
 */
class ObjetosPigTest {

    @TempDir
    Path raiz;

    // --- Helpers para escribir archivos ---

    private void escribir(String rutaRelativa, String contenido) throws IOException {
        Path archivo = raiz.resolve(rutaRelativa);
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, contenido);
    }

    private ResultadoCompilacionPig analizar(String pig) {
        return new ServicioCompilacionPig().analizar(pig, raiz);
    }

    // --- Proyecto base de clases .z ---

    private void escribirClasesBase() throws IOException {
        escribir("clases/Animal.z", """
                public class Animal {
                    String nombre;
                    public Animal() { }
                    public Animal(String n) { nombre = n; }
                }
                """);
        escribir("clases/Perro.z", """
                public class Perro extends Animal {
                    public Perro() { }
                    public Perro(String n) { super(n); }
                }
                """);
        escribir("clases/Gato.z", """
                public class Gato extends Animal {
                    public Gato() { }
                }
                """);
    }

    // ---------- PRUEBAS ----------

    @Test
    void novusSinTipoSigueSiendoValido() throws IOException {
        escribirClasesBase();
        ResultadoCompilacionPig r = analizar("""
                import clases.Animal.z

                VARIABILES>
                esto a : novus Animal();

                MAIOR>
                >> "ok" ;
                FINIS;
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void novusConTipoExplicitoIgualAlTipoReal() throws IOException {
        escribirClasesBase();
        ResultadoCompilacionPig r = analizar("""
                import clases.Animal.z

                VARIABILES>
                esto a : novus Animal() : Animal;

                MAIOR>
                FINIS;
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void novusSubclaseConTipoSuperclaseEsValido() throws IOException {
        escribirClasesBase();
        ResultadoCompilacionPig r = analizar("""
                import clases.Animal.z
                import clases.Perro.z

                VARIABILES>
                esto a : novus Perro() : Animal;

                MAIOR>
                FINIS;
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void novusSubclaseConTipoHermanoEsError() throws IOException {
        escribirClasesBase();
        ResultadoCompilacionPig r = analizar("""
                import clases.Animal.z
                import clases.Perro.z
                import clases.Gato.z

                VARIABILES>
                esto a : novus Perro() : Gato;

                MAIOR>
                FINIS;
                """);
        assertFalse(r.isExitoso(), "Perro no es subtipo de Gato");
        assertFalse(r.getErroresSemanticos().isEmpty());
    }

    @Test
    void novusSuperclaseConTipoSubclaseEsError() throws IOException {
        escribirClasesBase();
        ResultadoCompilacionPig r = analizar("""
                import clases.Animal.z
                import clases.Perro.z

                VARIABILES>
                esto a : novus Animal() : Perro;

                MAIOR>
                FINIS;
                """);
        assertFalse(r.isExitoso(), "Animal no es subtipo de Perro");
        assertFalse(r.getErroresSemanticos().isEmpty());
    }

    @Test
    void nullConTipoClaseEsValido() throws IOException {
        escribirClasesBase();
        ResultadoCompilacionPig r = analizar("""
                import clases.Animal.z

                VARIABILES>
                esto a : null : Animal;

                MAIOR>
                FINIS;
                """);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }

    @Test
    void nullConTipoPrimitivoEsError() throws IOException {
        ResultadoCompilacionPig r = analizar("""
                VARIABILES>
                esto a : null : numerus;

                MAIOR>
                FINIS;
                """);
        assertFalse(r.isExitoso(), "null no puede ser numerus");
        assertFalse(r.getErroresSemanticos().isEmpty());
    }

    @Test
    void nullSinTipoNoParsea() throws IOException {
        ResultadoCompilacionPig r = analizar("""
                VARIABILES>
                esto a : null;

                MAIOR>
                FINIS;
                """);
        assertFalse(r.isExitoso(), "null sin tipo debe fallar en el parser");
        assertFalse(r.getErroresSintacticos().isEmpty());
    }
}