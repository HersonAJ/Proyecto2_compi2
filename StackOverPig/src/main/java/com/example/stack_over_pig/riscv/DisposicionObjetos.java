package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Única fuente de verdad sobre cómo se ve un objeto en memoria y cómo se resuelven
 * sus miembros cuando hay herencia. La usan los dos emisores (main de Pig y funciones de Z)
 * y el generador de cuartetas de Pig, para que nadie calcule offsets por su cuenta.
 *
 * Disposición de un objeto (cada campo ocupa 8 bytes):
 *
 *     [ atributos de la raíz ][ atributos del hijo ][ atributos del nieto ] ...
 *
 * Los atributos del padre van PRIMERO, así 'nombre' queda en el mismo lugar en un
 * Animal y en un Perro, y el código escrito para Animal sirve para cualquier subclase.
 *
 * (En la etapa de despacho dinámico se agregará aquí, al inicio, el puntero a la tabla de métodos.)
 */
public class DisposicionObjetos {

    public static final int BYTES_CAMPO = 8;

    /** Un método encontrado, junto con la clase donde está escrito. */
    public record MetodoResuelto(String claseDeclarante, TablaSimbolosPig.Firma firma) {}

    private final TablaSimbolosPig tabla;

    public DisposicionObjetos(TablaSimbolosPig tabla) {
        this.tabla = tabla;
    }

    // ========== JERARQUÍA ==========

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

    // ========== ATRIBUTOS ==========

    /** Offset (en bytes, desde el inicio del objeto) del atributo. Si se repite el nombre gana el más derivado. */
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
        return encontrado * BYTES_CAMPO;
    }

    /** Bytes que ocupa un objeto de esa clase (incluye los atributos heredados). Mínimo 8. */
    public int tamano(String clase) {
        int campos = 0;
        for (String c : cadenaDesdeRaiz(clase)) {
            campos += tabla.buscarClase(c).get().atributos().size();
        }
        return Math.max(1, campos) * BYTES_CAMPO;
    }

    /** Busca el atributo en la clase y luego en sus ancestros (gana el más cercano). */
    public Optional<TablaSimbolosPig.AtributoClase> buscarAtributo(String clase, String campo) {
        List<String> cadena = cadenaDesdeRaiz(clase);
        for (int i = cadena.size() - 1; i >= 0; i--) {
            var atributo = tabla.buscarClase(cadena.get(i)).get().atributos().get(campo);
            if (atributo != null) return Optional.of(atributo);
        }
        return Optional.empty();
    }

    // ========== MÉTODOS Y CONSTRUCTORES ==========

    /**
     * Busca un método por nombre y tipos de argumento, empezando por la clase y subiendo por sus ancestros.
     * Primero intenta coincidencia exacta de tipos; si no hay, acepta subclases donde se espera la clase padre.
     */
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

    /**
     * Tipos de parámetros del constructor que corresponde a esos argumentos (los constructores no se heredan).
     * Una clase sin constructores declarados tiene uno implícito sin parámetros.
     */
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

    /** Nombre RISC-V del constructor: Clase_constructor_<tipos de los parámetros declarados>. */
    public static String nombreConstructor(String clase, List<String> tiposParametros) {
        StringBuilder sb = new StringBuilder(clase).append("_constructor");
        for (String t : tiposParametros) sb.append('_').append(t);
        return sb.toString();
    }

    /** Nombre RISC-V del método: ClaseDeclarante_metodo_<tipos de los parámetros declarados>. */
    public static String nombreMetodo(String claseDeclarante, String metodo, TablaSimbolosPig.Firma firma) {
        StringBuilder sb = new StringBuilder(claseDeclarante).append('_').append(metodo);
        for (String t : tipos(firma.parametros())) sb.append('_').append(t);
        return sb.toString();
    }

    // ========== AUXILIARES ==========

    private boolean coincide(List<TablaSimbolosPig.Parametro> parametros, List<String> tiposArgs, boolean exacto) {
        if (parametros.size() != tiposArgs.size()) return false;
        for (int i = 0; i < parametros.size(); i++) {
            String esperado = parametros.get(i).tipo();
            String recibido = tiposArgs.get(i);
            if (esperado.equals(recibido)) continue;
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