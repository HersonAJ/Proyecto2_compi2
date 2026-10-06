package com.example.contacto_3xtrat3r3str3.zetariano.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;

import java.util.List;

public record NodoPrograma(int linea, int columna, NodoClase clase) implements NodoAST {

    public List<EstructuraC> aEstructurasC() {
        return List.of(clase.aEstructuraC());
    }

    public List<FuncionC> aFuncionesC(TablaSimbolosZ tabla) {
        return clase.aFuncionesC(tabla);
    }
}