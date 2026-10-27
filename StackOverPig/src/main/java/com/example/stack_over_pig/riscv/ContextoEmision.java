package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Estado compartido entre el emisor de main (Pig) y el emisor de funciones (Z).
 *
 * Mantiene:
 *   - data / bss: contenido de las secciones .data y .bss.
 *   - text: cuerpo de main. Las funciones Z se escriben en un StringBuilder aparte.
 *   - cadenas: mapa texto escapado -> etiqueta, para no duplicar literales de texto.
 *   - contadorEtiquetas: contador global de etiquetas L<n> (compartido entre main y funciones).
 *   - contadorSaltos: contador global para saltos auxiliares (RAMAS_LARGAS).
 *   - contadorBuffers: contador global para buffers de lectura de texto.
 *   - rutinasUsadas: conjunto de rutinas de consola que se deben emitir al final.
 *   - tabla: tabla de símbolos del Pig (para consultar clases, variables, etc.).
 *   - tamanoHeap: bytes reservados para el heap estático.
 */
public class ContextoEmision {

    private final StringBuilder data = new StringBuilder();
    private final StringBuilder bss = new StringBuilder();
    private final StringBuilder text = new StringBuilder();
    private final StringBuilder funciones = new StringBuilder();
    private final DisposicionObjetos disposicion;

    private final Map<String, String> cadenas = new LinkedHashMap<>();
    private final Set<String> rutinasUsadas = new LinkedHashSet<>();
    private TablaMetodosVirtuales virtuales = TablaMetodosVirtuales.vacia();

    private int contadorEtiquetas = 0;
    private int contadorSaltos = 0;
    private int contadorBuffers = 0;

    private final TablaSimbolosPig tabla;
    private final int tamanoHeap;

    public ContextoEmision(TablaSimbolosPig tabla, int tamanoHeap) {
        this.tabla = tabla;
        this.tamanoHeap = tamanoHeap;
        this.disposicion = new DisposicionObjetos(tabla);
    }

    public StringBuilder getData()       { return data; }
    public StringBuilder getBss()        { return bss; }
    public StringBuilder getText()       { return text; }
    public StringBuilder getFunciones()  { return funciones; }

    public Map<String, String> getCadenas() { return cadenas; }
    public Set<String> getRutinasUsadas()   { return rutinasUsadas; }

    public TablaSimbolosPig getTabla() { return tabla; }
    public int getTamanoHeap()         { return tamanoHeap; }

    public int siguienteEtiqueta() { return contadorEtiquetas++; }
    public int siguienteSalto()    { return contadorSaltos++; }
    public int siguienteBuffer()   { return contadorBuffers++; }

    public DisposicionObjetos getDisposicion() { return  disposicion; }

    public TablaMetodosVirtuales getVirtuales() { return virtuales; }
    public void setVirtuales(TablaMetodosVirtuales v) { this.virtuales = v; }

    public String etiquetaCadena(String valor) {
        String existente = cadenas.get(valor);
        if (existente != null) return existente;
        String etq = "str_" + cadenas.size();
        cadenas.put(valor, etq);
        data.append(declaracionCadena(etq, desescapar(valor)));
        return etq;
    }

    /** Convierte las secuencias \n, \t, \r, \0, \\, \" y \' en el carácter real. */
    private static String desescapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '0' -> sb.append('\0');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    case '\'' -> sb.append('\'');
                    default -> sb.append('\\').append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String declaracionCadena(String etiqueta, String s) {
        StringBuilder sb = new StringBuilder("    ").append(etiqueta).append(":\n");
        StringBuilder texto = new StringBuilder();
        List<Integer> bytes = new ArrayList<>();
        for (int cp : s.codePoints().toArray()) {
            if (cp >= 32 && cp < 127) {
                vaciarBytes(sb, bytes);
                if (cp == '"' || cp == '\\') texto.append('\\');
                texto.append((char) cp);
            } else {
                vaciarTexto(sb, texto);
                for (byte b : new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8)) {
                    bytes.add(b & 0xFF);
                }
            }
        }
        vaciarTexto(sb, texto);
        vaciarBytes(sb, bytes);
        sb.append("        .byte 0\n");
        return sb.toString();
    }

    private static void vaciarTexto(StringBuilder sb, StringBuilder texto) {
        if (texto.length() == 0) return;
        sb.append("        .ascii \"").append(texto).append("\"\n");
        texto.setLength(0);
    }

    private static void vaciarBytes(StringBuilder sb, List<Integer> bytes) {
        if (bytes.isEmpty()) return;
        sb.append("        .byte ");
        for (int i = 0; i < bytes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(bytes.get(i));
        }
        sb.append("\n");
        bytes.clear();
    }
}
