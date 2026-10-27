package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.zetariano.nodo.NodoClase;
import com.example.stack_over_pig.zetariano.nodo.NodoMetodo;
import com.example.stack_over_pig.zetariano.nodo.NodoParametroZ;
import com.example.stack_over_pig.zetariano.nodo.Visibilidad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tablas de métodos (para el polimorfismo).
 *
 * Idea: cada clase tiene una tabla, que es una lista de direcciones de funciones. Cada objeto guarda,
 * en su primera palabra, un puntero a la tabla de SU clase real. Llamar a un método virtual es:
 * leer la tabla del objeto, tomar la dirección que está en la posición ("slot") del método y saltar ahí.
 *
 * Reglas para asignar posiciones:
 *   - La tabla de una subclase empieza como copia de la de su padre (mismas posiciones).
 *   - Un método que sobrescribe a uno del padre (mismo nombre y mismos tipos de parámetros) REEMPLAZA
 *     la entrada de esa posición: así 'imprimir' es la posición 0 tanto en Impresora como en ImpresoraPDF,
 *     pero cada tabla apunta a su propia versión.
 *   - Un método nuevo se agrega al final.
 *   - Los métodos 'private' no entran a la tabla (no se heredan ni se sobrescriben): se llaman directo.
 *
 * Esta clase no cambia ninguna cuarteta: el emisor mira el nombre de la función que va a llamar
 * (ClaseDeclarante_metodo_tipos) y, si está aquí, hace la llamada por la tabla en vez de un salto fijo.
 */
public class TablaMetodosVirtuales {

    private static final int BYTES_SLOT = 8;

    private static class TablaClase {
        final List<String> etiquetas = new ArrayList<>();                 // posición -> función
        final Map<String, Integer> posicionPorFirma = new LinkedHashMap<>(); // "nombre(tipos)" -> posición
    }

    private final Map<String, TablaClase> tablas = new LinkedHashMap<>();
    private final Map<String, Integer> posicionDeEtiqueta = new LinkedHashMap<>();

    private TablaMetodosVirtuales() {
    }

    public static TablaMetodosVirtuales vacia() {
        return new TablaMetodosVirtuales();
    }

    /** Construye las tablas de todas las clases dadas (deben incluir los ancestros). */
    public static TablaMetodosVirtuales construir(List<NodoClase> clases) {
        TablaMetodosVirtuales t = new TablaMetodosVirtuales();
        Map<String, NodoClase> porNombre = new LinkedHashMap<>();
        for (NodoClase c : clases) porNombre.putIfAbsent(c.nombre(), c);
        for (NodoClase c : porNombre.values()) {
            t.construirClase(c, porNombre, new HashSet<>());
        }
        return t;
    }

    private TablaClase construirClase(NodoClase clase, Map<String, NodoClase> porNombre, Set<String> enProceso) {
        TablaClase hecha = tablas.get(clase.nombre());
        if (hecha != null) return hecha;
        if (!enProceso.add(clase.nombre())) return new TablaClase();   // herencia circular: se corta aquí

        TablaClase tabla = new TablaClase();

        NodoClase padre = clase.superclase() == null ? null : porNombre.get(clase.superclase());
        if (padre != null) {
            TablaClase deLaPadre = construirClase(padre, porNombre, enProceso);
            tabla.etiquetas.addAll(deLaPadre.etiquetas);
            tabla.posicionPorFirma.putAll(deLaPadre.posicionPorFirma);
        }

        for (NodoMetodo m : clase.metodos()) {
            if (m.visibilidad() == Visibilidad.PRIVADO) continue;

            String firma = firma(m);
            String etiqueta = etiqueta(clase.nombre(), m);

            Integer posicion = tabla.posicionPorFirma.get(firma);
            if (posicion == null) {                       // método nuevo
                posicion = tabla.etiquetas.size();
                tabla.etiquetas.add(etiqueta);
                tabla.posicionPorFirma.put(firma, posicion);
            } else {                                      // sobrescribe al del padre
                tabla.etiquetas.set(posicion, etiqueta);
            }
            posicionDeEtiqueta.put(etiqueta, posicion);
        }

        tablas.put(clase.nombre(), tabla);
        return tabla;
    }

    // ========== CONSULTAS (las usan los emisores) ==========

    /** true si llamar a esta función debe hacerse por la tabla de métodos del objeto. */
    public boolean esVirtual(String etiquetaMetodo) {
        return posicionDeEtiqueta.containsKey(etiquetaMetodo);
    }

    /** Desplazamiento en bytes dentro de la tabla. */
    public int desplazamiento(String etiquetaMetodo) {
        return posicionDeEtiqueta.get(etiquetaMetodo) * BYTES_SLOT;
    }

    public boolean tieneTabla(String clase) {
        return tablas.containsKey(clase);
    }

    public String etiquetaTabla(String clase) {
        return "vt_" + clase;
    }

    // ========== CÓDIGO A EMITIR ==========

    /** Reserva el espacio de cada tabla en la sección de datos. */
    public String seccionData() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, TablaClase> e : tablas.entrySet()) {
            int bytes = Math.max(1, e.getValue().etiquetas.size()) * BYTES_SLOT;
            sb.append("    .align 3\n");
            sb.append("    ").append(etiquetaTabla(e.getKey())).append(": .space ").append(bytes).append("\n");
        }
        return sb.toString();
    }

    /** Llena las tablas con las direcciones de las funciones. Va al inicio de _start, antes de llamar a main. */
    public String codigoInicializacion() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, TablaClase> e : tablas.entrySet()) {
            List<String> etiquetas = e.getValue().etiquetas;
            for (int i = 0; i < etiquetas.size(); i++) {
                sb.append("    la   t0, ").append(etiquetaTabla(e.getKey())).append("\n");
                sb.append("    la   t1, ").append(etiquetas.get(i)).append("\n");
                sb.append("    sd   t1, ").append(i * BYTES_SLOT).append("(t0)\n");
            }
        }
        if (sb.length() > 0) sb.insert(0, "    # tablas de métodos (polimorfismo)\n");
        return sb.toString();
    }

    // ========== AUXILIARES ==========

    /** Etiqueta de la función de un método: Clase_metodo_<tipos de parámetros en vocabulario Pig>. */
    private static String etiqueta(String clase, NodoMetodo m) {
        StringBuilder sb = new StringBuilder(clase).append('_').append(m.nombre());
        for (NodoParametroZ p : m.parametros()) sb.append('_').append(tipoZaPig(p.tipo()));
        return sb.toString();
    }

    /** Lo que identifica un método para saber si sobrescribe a otro: nombre + tipos de parámetros. */
    private static String firma(NodoMetodo m) {
        StringBuilder sb = new StringBuilder(m.nombre()).append('(');
        for (NodoParametroZ p : m.parametros()) sb.append(tipoZaPig(p.tipo())).append(',');
        return sb.append(')').toString();
    }

    private static String tipoZaPig(String tipoZ) {
        if (tipoZ == null) return null;
        return switch (tipoZ) {
            case "int"     -> "numerus";
            case "double"  -> "decimalis";
            case "char"    -> "littera";
            case "String"  -> "textum";
            case "boolean" -> "bool";
            default        -> tipoZ;
        };
    }
}