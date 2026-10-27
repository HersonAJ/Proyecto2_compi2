package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
}
