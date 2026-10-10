import com.example.stack_over_pig.zetariano.nodo.NodoClase;
import com.example.stack_over_pig.zetariano.nodo.NodoExpr;
import com.example.stack_over_pig.zetariano.nodo.NodoSentencia;
import com.example.stack_over_pig.zetariano.nodo.Visibilidad;
import com.example.stack_over_pig.zetariano.service.ResultadoCompilacionZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/** Verifica que el parser y el AST de Zetariano capturan lo nuevo del proyecto 2. */
class AstHerenciaZTest {

    /** Parsea sin errores sintacticos y devuelve la clase (los errores semanticos no importan aqui). */
    private NodoClase parsear(String codigo) {
        ResultadoCompilacionZ r = new ServicioCompilacionZ().analizar(codigo);
        assertTrue(r.getErroresSintacticos().isEmpty(),
                "errores sintacticos: " + r.getErroresSintacticos());
        assertNotNull(r.getPrograma(), "no se construyo el AST");
        return r.getPrograma().clase();
    }

    @Test
    void herenciaYVisibilidadPorDefecto() {
        NodoClase animal = parsear("""
                public class Animal {
                    String nombre;
                    public void comer() { println(nombre + " está comiendo."); }
                }
                """);
        assertEquals(Visibilidad.PUBLICO, animal.visibilidad());
        assertNull(animal.superclase());
        assertEquals(Visibilidad.PAQUETE, animal.atributos().get(0).visibilidad());
        assertEquals(Visibilidad.PUBLICO, animal.metodos().get(0).visibilidad());
        assertFalse(animal.metodos().get(0).esOverride());

        NodoClase perro = parsear("""
                public class Perro extends Animal {
                    public void ladrar() { println(nombre + " está ladrando: ¡Guau, guau!"); }
                }
                """);
        assertEquals("Animal", perro.superclase());
    }

    @Test
    void claseSinPublicYOverride() {
        NodoClase pdf = parsear("""
                class ImpresoraPDF extends Impresora {
                    @Override
                    public void imprimir() { println("Imprimiendo en formato .PDF optimizado..."); }
                }
                """);
        assertEquals(Visibilidad.PAQUETE, pdf.visibilidad());
        assertEquals("Impresora", pdf.superclase());
        assertTrue(pdf.metodos().get(0).esOverride());
        assertEquals(Visibilidad.PUBLICO, pdf.metodos().get(0).visibilidad());
    }

    @Test
    void modificadoresDeAtributos() {
        NodoClase c = parsear("""
                class A {
                    private int x;
                    protected int y;
                    public int z;
                    int w;
                }
                """);
        assertEquals(Visibilidad.PRIVADO, c.atributos().get(0).visibilidad());
        assertEquals(Visibilidad.PROTEGIDO, c.atributos().get(1).visibilidad());
        assertEquals(Visibilidad.PUBLICO, c.atributos().get(2).visibilidad());
        assertEquals(Visibilidad.PAQUETE, c.atributos().get(3).visibilidad());
    }

    @Test
    void thisExplicito() {
        NodoClase p = parsear("""
                class Persona {
                    String nombre;
                    public Persona(String nombre) { this.nombre = nombre; }
                }
                """);
        NodoSentencia primera = p.constructores().get(0).cuerpo().get(0);
        NodoSentencia.Asignacion asignacion = assertInstanceOf(NodoSentencia.Asignacion.class, primera);
        NodoExpr.AccesoAtributo destino = assertInstanceOf(NodoExpr.AccesoAtributo.class, asignacion.destino());
        assertInstanceOf(NodoExpr.ObjetoActual.class, destino.objeto());
        assertEquals("nombre", destino.atributo());
    }

    @Test
    void sintaxisInvalidaDebeFallar() {
        String[] invalidos = {
                "class A { @Override int x; }",     // @Override solo va en metodos
                "class A extends { }",              // extends sin nombre
                "class A { @Foo public void f() { } }"  // otra anotacion
        };
        for (String codigo : invalidos) {
            // cubre tanto el error lexico (@Foo) como los sintacticos
            ResultadoCompilacionZ r = new ServicioCompilacionZ().analizar(codigo);
            assertFalse(r.isExitoso(), "deberia fallar: " + codigo);
        }
    }
}
