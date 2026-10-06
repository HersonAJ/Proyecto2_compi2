package com.example.stack_over_pig.zetariano.nodo;

import com.example.stack_over_pig.c3d_v2.c.EstructuraC;
import com.example.stack_over_pig.c3d_v2.c.FuncionC;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;

import java.util.List;

public record NodoPrograma(int linea, int columna, NodoClase clase) implements NodoAST {

    public List<EstructuraC> aEstructurasC() {
        return List.of(clase.aEstructuraC());
    }

    public List<FuncionC> aFuncionesC(TablaSimbolosZ tabla) {
        return clase.aFuncionesC(tabla);
    }
}