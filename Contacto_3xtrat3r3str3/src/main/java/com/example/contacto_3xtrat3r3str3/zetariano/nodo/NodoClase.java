package com.example.contacto_3xtrat3r3str3.zetariano.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.ParametroC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.z.TipoCZ;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;

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
                tipoBaseC = "struct " + a.tipo();
            }
            // Arreglo estilo Java: un '*' por cada dimensión.
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