package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DisposicionObjetos {

    public static final int BYTES_CAMPO = 8;
    public static final int ENCABEZADO = 8;
    public record MetodoResuelto(String claseDeclarante, TablaSimbolosPig.Firma firma) {}

    private final TablaSimbolosPig tabla;

    public DisposicionObjetos(TablaSimbolosPig tabla) {
        this.tabla = tabla;
    }

    // JERARQUIA

    /** La clase y sus ancestros conocidos, de la raíz a la clase misma. Vacía si la clase no se conoce. */
    public List<String> cadenaDesdeRaiz(String clase) {
        LinkedList<String> cadena = new LinkedList<>();
        Set<String> vistos = new HashSet<>();
        String actual = clase;
        while (actual != null && vistos.add(actual)) {
            var def = tabla.buscarClase(actual);
            if (def.isEmpty()) break;
            cadena.addFirst(actual);
            actual = def.get().superclase();
        }
        return cadena;
    }

    // ATRIBUTOS

    public int offsetCampo(String clase, String campo) {
        int indice = 0;
        int encontrado = -1;
        for (String c : cadenaDesdeRaiz(clase)) {
            for (String atributo : tabla.buscarClase(c).get().atributos().keySet()) {
                if (atributo.equals(campo)) encontrado = indice;
                indice++;
            }
        }
        if (encontrado < 0) {
            throw new IllegalStateException(
                    "DisposicionObjetos: campo no encontrado: " + campo + " en " + clase);
        }
        return ENCABEZADO + encontrado * BYTES_CAMPO;
    }

    public int tamano(String clase) {
        int campos = 0;
        for (String c : cadenaDesdeRaiz(clase)) {
            campos += tabla.buscarClase(c).get().atributos().size();
        }
        return ENCABEZADO + campos * BYTES_CAMPO;
    }

    public Optional<TablaSimbolosPig.AtributoClase> buscarAtributo(String clase, String campo) {
        List<String> cadena = cadenaDesdeRaiz(clase);
        for (int i = cadena.size() - 1; i >= 0; i--) {
            var atributo = tabla.buscarClase(cadena.get(i)).get().atributos().get(campo);
            if (atributo != null) return Optional.of(atributo);
        }
        return Optional.empty();
    }

    // METODOS Y CONSTRUCTORES

    public Optional<MetodoResuelto> buscarMetodo(String clase, String nombre, List<String> tiposArgs) {
        List<String> cadena = cadenaDesdeRaiz(clase);
        for (boolean exacto : new boolean[]{true, false}) {
            for (int i = cadena.size() - 1; i >= 0; i--) {
                String c = cadena.get(i);
                for (TablaSimbolosPig.Firma f : tabla.buscarClase(c).get().metodos()) {
                    if (f.nombre().equals(nombre) && coincide(f.parametros(), tiposArgs, exacto)) {
                        return Optional.of(new MetodoResuelto(c, f));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<List<String>> buscarConstructor(String clase, List<String> tiposArgs) {
        var def = tabla.buscarClase(clase);
        if (def.isEmpty()) return Optional.empty();

        List<TablaSimbolosPig.Firma> declarados = def.get().constructores();
        if (declarados.isEmpty()) {
            return tiposArgs.isEmpty() ? Optional.of(List.of()) : Optional.empty();
        }
        for (boolean exacto : new boolean[]{true, false}) {
            for (TablaSimbolosPig.Firma f : declarados) {
                if (coincide(f.parametros(), tiposArgs, exacto)) return Optional.of(tipos(f.parametros()));
            }
        }
        return Optional.empty();
    }

    public static String nombreConstructor(String clase, List<String> tiposParametros) {
        StringBuilder sb = new StringBuilder(clase).append("_constructor");
        for (String t : tiposParametros) sb.append('_').append(t);
        return sb.toString();
    }

    public static String nombreMetodo(String claseDeclarante, String metodo, TablaSimbolosPig.Firma firma) {
        StringBuilder sb = new StringBuilder(claseDeclarante).append('_').append(metodo);
        for (String t : tipos(firma.parametros())) sb.append('_').append(t);
        return sb.toString();
    }

    // AUXILIARES

    private boolean coincide(List<TablaSimbolosPig.Parametro> parametros, List<String> tiposArgs, boolean exacto) {
        if (parametros.size() != tiposArgs.size()) return false;
        for (int i = 0; i < parametros.size(); i++) {
            String esperado = parametros.get(i).tipo();
            String recibido = tiposArgs.get(i);
            if (esperado.equals(recibido)) continue;
            if ("decimalis".equals(esperado) && "numerus".equals(recibido)) continue;
            if (exacto) return false;
            boolean ambasClases = tabla.buscarClase(esperado).isPresent() && tabla.buscarClase(recibido).isPresent();
            if (ambasClases && tabla.esSubtipo(recibido, esperado)) continue;
            return false;
        }
        return true;
    }

    private static List<String> tipos(List<TablaSimbolosPig.Parametro> parametros) {
        List<String> r = new ArrayList<>();
        for (TablaSimbolosPig.Parametro p : parametros) r.add(p.tipo());
        return r;
    }
}