package com.example.stack_over_pig.zetariano.nodo;

import com.example.stack_over_pig.c3d_v2.c.EstructuraC;
import com.example.stack_over_pig.c3d_v2.c.FuncionC;
import com.example.stack_over_pig.c3d_v2.c.ParametroC;
import com.example.stack_over_pig.c3d_v2.c.z.TipoCZ;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;

import java.util.ArrayList;
import java.util.List;

public record NodoClase(
        int linea, int columna, String nombre,
        List<NodoAtributoZ> atributos,
        List<NodoConstructor> constructores,
        List<NodoMetodo> metodos) implements NodoAST {

    public EstructuraC aEstructuraC() {
        List<ParametroC> campos = new ArrayList<>();
        for (NodoAtributoZ a : atributos) {
            String tipoBaseC;
            if (TipoCZ.esPrimitivo(a.tipo())) {
                tipoBaseC = TipoCZ.baseValorAC(a.tipo());
            } else {
                tipoBaseC = "struct " + a.tipo() + "*";   // ← puntero para clases
            }
            String tipoC = tipoBaseC + "*".repeat(a.dimensiones());
            campos.add(new ParametroC(tipoC, a.nombre()));
        }
        return new EstructuraC(nombre, campos);
    }

    public List<FuncionC> aFuncionesC(TablaSimbolosZ tabla) {
        List<FuncionC> funciones = new ArrayList<>();
        for (NodoConstructor c : constructores) {
            funciones.add(c.aFuncionC(tabla, nombre));
        }
        for (NodoMetodo m : metodos) {
            funciones.add(m.aFuncionC(tabla, nombre));
        }
        return funciones;
    }
}