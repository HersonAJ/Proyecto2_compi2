import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ.DefinicionClaseExterna;
import com.example.stack_over_pig.zetariano.service.CargadorClasesZ;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Paso 6a: paquete = carpeta, y las superclases se cargan solas desde la carpeta de la clase hija. */
class CargadorClasesZTest {

    @TempDir
    Path raiz;

    private Path escribir(String rutaRelativa, String contenido) throws IOException {
        Path archivo = raiz.resolve(rutaRelativa);
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, contenido);
        return archivo;
    }

    @Test
    void elPaqueteEsLaCarpetaRelativaALaRaiz() {
        CargadorClasesZ cargador = new CargadorClasesZ(raiz);
        assertEquals("", cargador.paqueteDe(raiz.resolve("Animal.z")));
        assertEquals("zoo", cargador.paqueteDe(raiz.resolve("zoo").resolve("Animal.z")));
        assertEquals("a.b", cargador.paqueteDe(raiz.resolve("a").resolve("b").resolve("X.z")));
    }

    @Test
    void cargaLasSuperclasesEnCadenaSinIncluirLaPropia() throws IOException {
        escribir("zoo/Animal.z", "public class Animal { }");
        escribir("zoo/Perro.z", "public class Perro extends Animal { }");
        Path cachorro = escribir("zoo/Cachorro.z", "public class Cachorro extends Perro { }");

        Map<String, DefinicionClaseExterna> cargadas =
                new CargadorClasesZ(raiz).cargarAncestros(cachorro, Map.of());

        assertEquals(List.of("Perro", "Animal"), List.copyOf(cargadas.keySet()));
        assertEquals("zoo", cargadas.get("Animal").paquete());
    }

    @Test
    void noRecargaLoQueYaSeConoce() throws IOException {
        escribir("Animal.z", "public class Animal { }");
        escribir("Perro.z", "public class Perro extends Animal { }");
        Path cachorro = escribir("Cachorro.z", "public class Cachorro extends Perro { }");

        Map<String, DefinicionClaseExterna> conocidas = Map.of(
                "Animal", ServicioCompilacionZ.recolectarFirmas("public class Animal { }").orElseThrow());
        Map<String, DefinicionClaseExterna> cargadas =
                new CargadorClasesZ(raiz).cargarAncestros(cachorro, conocidas);

        assertEquals(List.of("Perro"), List.copyOf(cargadas.keySet()));
    }

    @Test
    void siFaltaElArchivoDeLaSuperclaseNoFallaYSeReportaDespues() throws IOException {
        Path perro = escribir("Perro.z", "public class Perro extends Fantasma { }");

        assertTrue(new CargadorClasesZ(raiz).cargarAncestros(perro, Map.of()).isEmpty());

        ResultadoCompilacionZ r = new ServicioCompilacionZ().analizarArchivo(perro, raiz);
        assertFalse(r.isExitoso());
        assertEquals(1, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    @Test
    void unArchivoQueNoDeclaraLaClaseBuscadaNoSeUsa() throws IOException {
        escribir("Animal.z", "public class Otro { }");
        Path perro = escribir("Perro.z", "public class Perro extends Animal { }");

        assertTrue(new CargadorClasesZ(raiz).cargarAncestros(perro, Map.of()).isEmpty());
    }

    @Test
    void laHerenciaCircularNoCuelgaLaCarga() throws IOException {
        Path a = escribir("A.z", "public class A extends B { }");
        escribir("B.z", "public class B extends A { }");

        Map<String, DefinicionClaseExterna> cargadas = new CargadorClasesZ(raiz).cargarAncestros(a, Map.of());
        assertEquals(List.of("B"), List.copyOf(cargadas.keySet()));

        ResultadoCompilacionZ r = new ServicioCompilacionZ().analizarArchivo(a, raiz);
        assertFalse(r.isExitoso());
        assertEquals(1, r.getErroresSemanticos().size(), r.getErroresSemanticos().toString());
    }

    @Test
    void analizarArchivoUsaLosMiembrosHeredadosDeLaCarpeta() throws IOException {
        escribir("zoo/Animal.z", """
                public class Animal {
                    String nombre;
                    public void comer() { }
                }
                """);
        Path perro = escribir("zoo/Perro.z", """
                public class Perro extends Animal {
                    public void ladrar() { println(nombre); comer(); }
                }
                """);

        ResultadoCompilacionZ r = new ServicioCompilacionZ().analizarArchivo(perro, raiz);
        assertTrue(r.isExitoso(), r.getErroresSemanticos().toString());
    }
}