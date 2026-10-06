package com.example.contacto_3xtrat3r3str3.piglatin.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.ParametroC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.VariableLocalC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.*;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig.ContextoTraduccionPig;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig.LiteralPig;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig.TipoPigC;
import com.example.contacto_3xtrat3r3str3.piglatin.semantica.TablaSimbolosPig;

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

    public List<ParametroC> aVariablesGlobalesC(TablaSimbolosPig tabla) {
        List<ParametroC> resultado = new ArrayList<>();
        java.util.Set<String> yaDeclarados = new java.util.HashSet<>();

        for (NodoSentencia s : variablesGlobales) {
            switch (s) {
                case NodoSentencia.DeclaracionVariable d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());

                    boolean esObjeto = d.tipo() != null
                            && tabla.buscarClase(d.tipo()).isPresent();

                    tabla.declararVariable(d.nombre(), d.tipo(), 0, null,
                            !TipoPigC.esPrimitivo(d.tipo()), esObjeto, d.tipo());

                    String tipoC = TipoPigC.baseAC(d.tipo(), esObjeto);
                    if (d.inicializacion() == null) {
                        resultado.add(new ParametroC(tipoC, d.nombre()));
                    } else if (esLiteralConstante(d.inicializacion())) {
                        String literal = literalC(d.inicializacion());
                        resultado.add(new ParametroC(tipoC, d.nombre() + " = " + literal));
                    } else {
                        resultado.add(new ParametroC(tipoC, d.nombre()));
                    }
                }
                case NodoSentencia.DeclaracionArreglo d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());
                    tabla.declararVariable(d.nombre(), d.tipo(), 1, d.tamano(),
                            !TipoPigC.esPrimitivo(d.tipo()), false, d.tipo());

                    String tipoBaseC = TipoPigC.baseValorAC(d.tipo());
                    String nombreConDim = d.nombre() + "[" + d.tamano() + "]";
                    resultado.add(new ParametroC(tipoBaseC, nombreConDim));
                }
                case NodoSentencia.DeclaracionStruct d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());
                    tabla.declararVariable(d.nombre(), d.tipo(), 0, null, true, false, d.tipo());
                    resultado.add(new ParametroC("struct " + d.tipo(), d.nombre()));
                }
                default -> {
                }
            }
        }
        return resultado;
    }

    private static boolean esLiteralConstante(NodoExpr expr) {
        return expr instanceof NodoExpr.LiteralEntero
                || expr instanceof NodoExpr.LiteralDecimal
                || expr instanceof NodoExpr.LiteralTexto
                || expr instanceof NodoExpr.LiteralCaracter
                || expr instanceof NodoExpr.LiteralBool;
    }

    private static String literalC(NodoExpr expr) {
        if (expr instanceof NodoExpr.LiteralEntero l) return String.valueOf(l.valor());
        if (expr instanceof NodoExpr.LiteralDecimal l) return String.valueOf(l.valor());
        if (expr instanceof NodoExpr.LiteralTexto l) return "\"" + l.valor() + "\"";
        if (expr instanceof NodoExpr.LiteralCaracter l) return "'" + l.valor() + "'";
        if (expr instanceof NodoExpr.LiteralBool l) return l.valor() ? "1" : "0";
        return "0";
    }

    public FuncionC aMainC(TablaSimbolosPig tabla) {
        GestorCodigoIntermedio gestor = new GestorCodigoIntermedio();
        ContextoTraduccionPig ctx = new ContextoTraduccionPig(gestor, tabla);

        // Variables locales del main.
        List<VariableLocalC> locales = new ArrayList<>();
        java.util.Set<String> yaDeclarados = new java.util.HashSet<>();

        // Variables globales que no se inicializaron directo en C.
        for (NodoSentencia s : variablesGlobales) {
            if (s instanceof NodoSentencia.DeclaracionVariable d
                    && d.inicializacion() != null
                    && !esLiteralConstante(d.inicializacion())) {
                // Inicializar aquí
                AccesoMemoria valor = d.inicializacion().aCodigoIntermedio(ctx);

                boolean esObjeto = d.tipo() != null
                        && tabla.buscarClase(d.tipo()).isPresent();
                String tipoC = TipoPigC.baseAC(d.tipo(), esObjeto);

                AccesoVariable destino = new AccesoVariable(d.nombre(), tipoC);
                gestor.emitir(new AsignacionVariable(destino, valor));
            }
        }

        // Declarar variables locales del main y traducir cuerpo.
        recogerLocales(tabla, cuerpoMain, locales, yaDeclarados);

        tabla.entrarScope("main");
        try {
            for (NodoSentencia s : cuerpoMain) {
                s.aCodigoIntermedio(ctx);
            }
        } finally {
            tabla.salirScope();
        }

        // return 0;
        gestor.emitir(new Retorno1(new LiteralPig(0, "numerus")));

        List<ParametroC> paramsC = new ArrayList<>();
        List<Cuarteta> cuartetas = gestor.getCuartetas();
        List<String> tiposTemp = gestor.getContador().getTiposTemporales();

        return new FuncionC("int", "main", paramsC, locales,
                new ArrayList<>(), cuartetas, tiposTemp);
    }
}