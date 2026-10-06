package com.example.contacto_3xtrat3r3str3.c3d_v2.c.z;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;

// Contexto de traducción para el lenguaje Z
public class ContextoTraduccionZ {

    private final GestorCodigoIntermedio gestor;
    private final TablaSimbolosZ tabla;
    private final String nombreClase;       // la clase que se esta traduciendo
    private final String nombreMetodo;      // el metodo/constructor actual
    private boolean dentroDeSwitch = false;

    public ContextoTraduccionZ(GestorCodigoIntermedio gestor,
                               TablaSimbolosZ tabla,
                               String nombreClase,
                               String nombreMetodo) {
        this.gestor = gestor;
        this.tabla = tabla;
        this.nombreClase = nombreClase;
        this.nombreMetodo = nombreMetodo;
    }

    public GestorCodigoIntermedio getGestor() { return gestor; }
    public TablaSimbolosZ getTabla()          { return tabla; }
    public String getNombreClase()            { return nombreClase; }
    public String getNombreMetodo()           { return nombreMetodo; }
    public boolean isDentroDeSwitch() { return dentroDeSwitch; }
    public void setDentroDeSwitch(boolean v) { this.dentroDeSwitch = v; }
}