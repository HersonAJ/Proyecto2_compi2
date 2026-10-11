package com.example.stack_over_pig.zetariano.semantica;

import com.example.stack_over_pig.zetariano.nodo.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte un NodoClase en su definicion resumida (DefinicionClaseExterna): atributos,
 * constructores y metodos con su visibilidad, sin los cuerpos.
 *
 * Es el unico lugar donde se construye esa definicion, para que la clase de un archivo
 * y las clases que vienen de otros archivos se describan exactamente igual.
 */
public final class ExtractorFirmasZ {

    private ExtractorFirmasZ() {}

    /** 'paquete' es la carpeta del archivo ("" si esta en la raiz). */
    public static TablaSimbolosZ.DefinicionClaseExterna extraer(NodoClase clase, String paquete) {
        String nombreClase = clase.nombre();

        Map<String, TablaSimbolosZ.SimboloAtributo> atributos = new LinkedHashMap<>();
        for (NodoAtributoZ a : clase.atributos()) {
            atributos.put(a.nombre(), new TablaSimbolosZ.SimboloAtributo(
                    a.nombre(), a.tipo(), a.dimensiones(), a.visibilidad(), nombreClase));
        }

        Map<String, List<TablaSimbolosZ.Firma>> constructores = new LinkedHashMap<>();
        for (NodoConstructor c : clase.constructores()) {
            constructores
                    .computeIfAbsent(c.nombre(), k -> new ArrayList<>())
                    .add(new TablaSimbolosZ.Firma(c.nombre(), parametros(c.parametros()), null,
                            c.visibilidad(), false, nombreClase));
        }

        Map<String, List<TablaSimbolosZ.Firma>> metodos = new LinkedHashMap<>();
        for (NodoMetodo m : clase.metodos()) {
            metodos
                    .computeIfAbsent(m.nombre(), k -> new ArrayList<>())
                    .add(new TablaSimbolosZ.Firma(m.nombre(), parametros(m.parametros()), m.tipoRetorno(),
                            m.visibilidad(), m.esOverride(), nombreClase));
        }

        return new TablaSimbolosZ.DefinicionClaseExterna(
                nombreClase, clase.superclase(), clase.visibilidad(), paquete,
                atributos, constructores, metodos);
    }

    private static List<TablaSimbolosZ.Parametro> parametros(List<NodoParametroZ> nodos) {
        List<TablaSimbolosZ.Parametro> resultado = new ArrayList<>();
        for (NodoParametroZ p : nodos) {
            resultado.add(new TablaSimbolosZ.Parametro(p.nombre(), p.tipo(), 0));
        }
        return resultado;
    }
}
