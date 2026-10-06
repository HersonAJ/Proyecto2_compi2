package com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.contacto_3xtrat3r3str3.piglatin.semantica.TablaSimbolosPig;

// Contexto de traducción para PigLatin
public class ContextoTraduccionPig {

    private final GestorCodigoIntermedio gestor;
    private final TablaSimbolosPig tabla;

    // Nombre de la función actual (null si estamos en main).
    private String funcionActual;

    // Si estamos dentro de un switch (no aplica a .pig, pero por consistencia).
    private boolean dentroDeSwitch = false;

    public ContextoTraduccionPig(GestorCodigoIntermedio gestor, TablaSimbolosPig tabla) {
        this.gestor = gestor;
        this.tabla = tabla;
    }

    public GestorCodigoIntermedio getGestor() { return gestor; }
    public TablaSimbolosPig getTabla()        { return tabla; }

    public String getFuncionActual() { return funcionActual; }
    public void setFuncionActual(String funcionActual) { this.funcionActual = funcionActual; }

    public boolean isDentroDeSwitch() { return dentroDeSwitch; }
    public void setDentroDeSwitch(boolean v) { this.dentroDeSwitch = v; }
}