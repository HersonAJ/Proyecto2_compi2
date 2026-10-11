import com.example.stack_over_pig.comun.JerarquiaClases;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JerarquiaClasesTest {

    private JerarquiaClases con(Map<String, String> superclases) {
        return new JerarquiaClases(superclases::get);
    }

    @Test
    void cadenaVaDeLaMasDerivadaALaRaiz() {
        JerarquiaClases j = con(Map.of("Cachorro", "Perro", "Perro", "Animal"));
        assertEquals(List.of("Cachorro", "Perro", "Animal"), j.cadena("Cachorro"));
        assertEquals(List.of("Animal"), j.cadena("Animal"));
    }

    @Test
    void esSubtipoEsReflexivoYTransitivo() {
        JerarquiaClases j = con(Map.of("Cachorro", "Perro", "Perro", "Animal"));
        assertTrue(j.esSubtipo("Perro", "Perro"));
        assertTrue(j.esSubtipo("Cachorro", "Animal"));
        assertFalse(j.esSubtipo("Animal", "Perro"));
        assertFalse(j.esSubtipo("Perro", "Gato"));
        assertFalse(j.esSubtipo(null, "Animal"));
    }

    @Test
    void detectaCiclosYNoSeCuelga() {
        JerarquiaClases j = con(Map.of("A", "B", "B", "A", "C", "C"));
        assertTrue(j.tieneCiclo("A"));
        assertTrue(j.tieneCiclo("C"));                       // una clase que se hereda a si misma
        assertEquals(List.of("A", "B"), j.cadena("A"));      // la cadena se corta al repetir
        assertFalse(con(Map.of("Perro", "Animal")).tieneCiclo("Perro"));
    }
}