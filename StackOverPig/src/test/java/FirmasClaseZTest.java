import com.example.stack_over_pig.zetariano.nodo.Visibilidad;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;
import com.example.stack_over_pig.zetariano.service.ServicioCompilacionZ;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica que la definicion resumida de una clase lleva herencia, visibilidad, override y paquete. */
class FirmasClaseZTest {

    @Test
    void firmasIncluyenHerenciaVisibilidadYPaquete() {
        TablaSimbolosZ.DefinicionClaseExterna def = ServicioCompilacionZ.recolectarFirmas("""
                public class Perro extends Animal {
                    private String raza;
                    public Perro() { }
                    @Override
                    protected void comer() { }
                }
                """, "zoo").orElseThrow();

        assertEquals("Perro", def.nombre());
        assertEquals("Animal", def.superclase());
        assertEquals("zoo", def.paquete());
        assertEquals(Visibilidad.PUBLICO, def.visibilidad());

        TablaSimbolosZ.SimboloAtributo raza = def.atributos().get("raza");
        assertEquals(Visibilidad.PRIVADO, raza.visibilidad());
        assertEquals("Perro", raza.claseDeclarante());

        TablaSimbolosZ.Firma comer = def.metodos().get("comer").get(0);
        assertTrue(comer.esOverride());
        assertEquals(Visibilidad.PROTEGIDO, comer.visibilidad());
        assertEquals("Perro", comer.claseDeclarante());

        assertEquals(1, def.constructores().get("Perro").size());
    }

    @Test
    void sinExtendsLaSuperclaseEsNullYElPaqueteVacioPorDefecto() {
        TablaSimbolosZ.DefinicionClaseExterna def = ServicioCompilacionZ.recolectarFirmas("""
                class Animal {
                    String nombre;
                }
                """).orElseThrow();

        assertNull(def.superclase());
        assertEquals("", def.paquete());
        assertEquals(Visibilidad.PAQUETE, def.visibilidad());
        assertEquals(Visibilidad.PAQUETE, def.atributos().get("nombre").visibilidad());
    }
}