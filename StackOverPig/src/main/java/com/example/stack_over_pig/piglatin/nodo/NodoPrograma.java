package com.example.stack_over_pig.piglatin.nodo;

import com.example.stack_over_pig.c3d_v2.c.FuncionC;
import com.example.stack_over_pig.c3d_v2.c.ParametroC;
import com.example.stack_over_pig.c3d_v2.c.VariableLocalC;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.c.pig.ContextoTraduccionPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.LiteralPig;
import com.example.stack_over_pig.c3d_v2.c.pig.TipoPigC;
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.util.ArrayList;
import java.util.List;

// Nodo raíz del AST de PigLatin.
// Contiene: importaciones, variables globales (opcional) y el cuerpo del MAIOR.
public record NodoPrograma(int linea, int columna,
                           List<NodoImportacion> importaciones,
                           List<NodoSentencia> variablesGlobales,
                           List<NodoSentencia> cuerpoMain) implements NodoAST {

    private static void recogerLocales(TablaSimbolosPig tabla,
                                       List<NodoSentencia> sentencias,
                                       List<VariableLocalC> acumuladas,
                                       java.util.Set<String> yaDeclarados) {
        for (NodoSentencia s : sentencias) {
            switch (s) {
                case NodoSentencia.DeclaracionVariable d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());

                    boolean esObjeto = d.tipo() != null
                            && tabla.buscarClase(d.tipo()).isPresent();

                    tabla.declararVariable(d.nombre(), d.tipo(), 0, null,
                            !TipoPigC.esPrimitivo(d.tipo()), esObjeto, d.tipo());

                    String tipoC = TipoPigC.baseAC(d.tipo(), esObjeto);
                    acumuladas.add(new VariableLocalC(tipoC, d.nombre()));
                }
                case NodoSentencia.DeclaracionArreglo d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());
                    tabla.declararVariable(d.nombre(), d.tipo(), 1, d.tamano(),
                            !TipoPigC.esPrimitivo(d.tipo()), false, d.tipo());
                    acumuladas.add(new VariableLocalC(
                            TipoPigC.baseValorAC(d.tipo()),
                            d.nombre() + "[" + d.tamano() + "]"));
                }
                case NodoSentencia.DeclaracionStruct d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());
                    tabla.declararVariable(d.nombre(), d.tipo(), 0, null,
                            true, false, d.tipo());
                    acumuladas.add(new VariableLocalC(
                            "struct " + d.tipo(),
                            d.nombre()));
                }
                case NodoSentencia.Condicional c -> {
                    recogerLocales(tabla, c.cuerpoSi(), acumuladas, yaDeclarados);
                    for (NodoSentencia.RamaAliter rama : c.ramasAliter()) {
                        recogerLocales(tabla, rama.cuerpo(), acumuladas, yaDeclarados);
                    }
                    if (c.cuerpoAliter() != null) {
                        recogerLocales(tabla, c.cuerpoAliter(), acumuladas, yaDeclarados);
                    }
                }
                case NodoSentencia.CicloDum c -> recogerLocales(tabla, c.cuerpo(), acumuladas, yaDeclarados);
                case NodoSentencia.CicloFacere c -> recogerLocales(tabla, c.cuerpo(), acumuladas, yaDeclarados);
                case NodoSentencia.CicloPer c -> {
                    if (c.inicializacion() instanceof NodoSentencia.DeclaracionVariable d) {
                        if (!yaDeclarados.contains(d.nombre())) {
                            yaDeclarados.add(d.nombre());

                            boolean esObjeto = d.tipo() != null
                                    && tabla.buscarClase(d.tipo()).isPresent();

                            tabla.declararVariable(d.nombre(), d.tipo(), 0, null,
                                    !TipoPigC.esPrimitivo(d.tipo()), esObjeto, d.tipo());
                            acumuladas.add(new VariableLocalC(
                                    TipoPigC.baseAC(d.tipo(), esObjeto),
                                    d.nombre()));
                        }
                    }
                    recogerLocales(tabla, c.cuerpo(), acumuladas, yaDeclarados);
                }
                default -> {
                }
            }
        }
    }

}