package com.example.contacto_3xtrat3r3str3.y.ast;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.ParametroC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.TipoC;
import com.example.contacto_3xtrat3r3str3.y.semantica.TablaSimbolos;

import java.util.ArrayList;
import java.util.List;

public sealed interface NodoPrograma extends NodoAST permits NodoPrograma.Programa {

    TipoNodoEstructura tipoNodo();

    record Programa(int linea, int columna,
                    List<NodoEstructura> estructuras,
                    List<NodoFuncion> funciones) implements NodoPrograma {
        @Override
        public TipoNodoEstructura tipoNodo() {
            return TipoNodoEstructura.PROGRAMA;
        }

        public List<FuncionC> aFuncionesC(TablaSimbolos tabla) {
            List<FuncionC> resultado = new ArrayList<>();
            for (NodoFuncion f : funciones) {
                if (f instanceof NodoFuncion.Funcion funcion) {
                    resultado.add(funcion.aFuncionC(tabla));
                }
            }
            return resultado;
        }

        public List<EstructuraC> aEstructurasC() {
            List<EstructuraC> resultado = new ArrayList<>();
            for (NodoEstructura e : estructuras) {
                if (e instanceof NodoEstructura.Estructura est) {
                    List<ParametroC> campos = new ArrayList<>();
                    for (NodoAtributo a : est.atributos()) {
                        if (a instanceof NodoAtributo.Atributo attr) {
                            String tipoC;
                            if (attr.tipoEstructura() != null) {
                                tipoC = "struct " + attr.tipoEstructura();
                            } else {
                                tipoC = TipoC.primitivoAC(attr.tipoPrimitivo());
                            }
                            if (attr.tamanoArreglo() > 0) {
                                campos.add(new ParametroC(tipoC,
                                        attr.nombre() + "[" + attr.tamanoArreglo() + "]"));
                            } else {
                                campos.add(new ParametroC(tipoC, attr.nombre()));
                            }
                        }
                    }
                    resultado.add(new EstructuraC(est.nombre(), campos));
                }
            }
            return resultado;
        }
    }
}