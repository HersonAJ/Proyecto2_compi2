package com.example.contacto_3xtrat3r3str3.c3d_v2.c;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.contacto_3xtrat3r3str3.y.semantica.TablaSimbolos;

import java.util.HashMap;
import java.util.Map;

public class ContextoTraduccion {

    private final GestorCodigoIntermedio gestor;
    private final TablaSimbolos tabla;

    private String tipoRetornoFuncionActual;

    // Alias de estructuras locales: nombre en .y -> nombre único en C.
    // Ej: "Punto" -> "Punto_test" cuando se declara dentro de la función 'test'.
    private final Map<String, String> aliasEstructuras = new HashMap<>();

    public ContextoTraduccion(GestorCodigoIntermedio gestor, TablaSimbolos tabla) {
        this.gestor = gestor;
        this.tabla = tabla;
    }

    public GestorCodigoIntermedio getGestor() { return gestor; }
    public TablaSimbolos getTabla()           { return tabla; }

    public String getTipoRetornoFuncionActual() {
        return tipoRetornoFuncionActual;
    }

    public void setTipoRetornoFuncionActual(String tipoRetornoFuncionActual) {
        this.tipoRetornoFuncionActual = tipoRetornoFuncionActual;
    }

    // ===== Alias de estructuras =====

    public String nombreEstructuraC(String nombreY) {
        return aliasEstructuras.getOrDefault(nombreY, nombreY);
    }

    public void registrarAliasEstructura(String nombreY, String nombreC) {
        aliasEstructuras.put(nombreY, nombreC);
    }

    public void limpiarAliasEstructuras() {
        aliasEstructuras.clear();
    }
}