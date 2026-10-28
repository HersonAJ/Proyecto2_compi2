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

public class TablaMetodosVirtuales {

    private static final int BYTES_SLOT = 8;

    private static class TablaClase {
        final List<String> etiquetas = new ArrayList<>();
        final Map<String, Integer> posicionPorFirma = new LinkedHashMap<>();
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
        if (!enProceso.add(clase.nombre())) return new TablaClase();

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
            if (posicion == null) {
                posicion = tabla.etiquetas.size();
                tabla.etiquetas.add(etiqueta);
                tabla.posicionPorFirma.put(firma, posicion);
            } else {
                tabla.etiquetas.set(posicion, etiqueta);
            }
            posicionDeEtiqueta.put(etiqueta, posicion);
        }

        tablas.put(clase.nombre(), tabla);
        return tabla;
    }

    // ========== CONSULTAS (las usan los emisores) ==========

    public boolean esVirtual(String etiquetaMetodo) {
        return posicionDeEtiqueta.containsKey(etiquetaMetodo);
    }

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

    public String seccionData() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, TablaClase> e : tablas.entrySet()) {
            int bytes = Math.max(1, e.getValue().etiquetas.size()) * BYTES_SLOT;
            sb.append("    .align 3\n");
            sb.append("    ").append(etiquetaTabla(e.getKey())).append(": .space ").append(bytes).append("\n");
        }
        return sb.toString();
    }

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

    // AUXILIARES
    private static String etiqueta(String clase, NodoMetodo m) {
        StringBuilder sb = new StringBuilder(clase).append('_').append(m.nombre());
        for (NodoParametroZ p : m.parametros()) sb.append('_').append(tipoZaPig(p.tipo()));
        return sb.toString();
    }

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