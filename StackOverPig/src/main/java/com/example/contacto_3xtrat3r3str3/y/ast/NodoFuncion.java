package com.example.contacto_3xtrat3r3str3.y.ast;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.*;
import com.example.contacto_3xtrat3r3str3.y.semantica.TablaSimbolos;

import java.util.ArrayList;
import java.util.List;

public sealed interface NodoFuncion extends NodoAST permits NodoFuncion.Funcion {

    TipoNodoEstructura tipoNodo();

    record Funcion(int linea, int columna,
                   String nombre,
                   List<NodoParametro> parametros,
                   String tipoRetorno,
                   List<NodoSentencia> cuerpo) implements NodoFuncion {

        @Override
        public TipoNodoEstructura tipoNodo() {
            return TipoNodoEstructura.FUNCION;
        }

        public FuncionC aFuncionC(TablaSimbolos tabla) {
            GestorCodigoIntermedio gestor = new GestorCodigoIntermedio();
            ContextoTraduccion ctx = new ContextoTraduccion(gestor, tabla);

            // 1. Recolectar estructuras locales y registrar alias.
            List<EstructuraC> estructurasLocales = new ArrayList<>();
            recogerEstructurasLocales(ctx, cuerpo, estructurasLocales);

            List<VariableLocalC> variablesLocales = new ArrayList<>();

            tabla.entrarScope(nombre);
            try {
                declararParametrosEnScope(tabla);
                recogerYDeclararVariablesLocales(ctx, tabla, cuerpo, variablesLocales);

                for (NodoSentencia s : cuerpo) {
                    s.aCodigoIntermedio(ctx);
                }
            } finally {
                tabla.salirScope();
                ctx.limpiarAliasEstructuras();
            }

            List<ParametroC> paramsC = new ArrayList<>();
            for (NodoParametro p : parametros) {
                if (p instanceof NodoParametro.Parametro par) {
                    paramsC.add(aParametroC(ctx, par));
                }
            }

            String tipoRetornoC = (tipoRetorno == null)
                    ? "void"
                    : TipoC.primitivoAC(tipoRetorno);
            List<Cuarteta> cuartetas = gestor.getCuartetas();
            List<String> tiposTemporales = gestor.getContador().getTiposTemporales();

            return new FuncionC(tipoRetornoC, nombre, paramsC, variablesLocales,
                    estructurasLocales, cuartetas, tiposTemporales);
        }

        // ============================================================
        // Estructuras locales
        // ============================================================

        private void recogerEstructurasLocales(ContextoTraduccion ctx,
                                               List<NodoSentencia> sentencias,
                                               List<EstructuraC> acumuladas) {
            for (NodoSentencia s : sentencias) {
                if (s instanceof NodoSentencia.DeclaracionEstructuraLocal d) {
                    NodoEstructura.Estructura est = d.estructura();
                    String nombreY = est.nombre();
                    String nombreC = nombreY + "_" + nombre;   // "Punto_test"

                    ctx.registrarAliasEstructura(nombreY, nombreC);

                    List<ParametroC> campos = new ArrayList<>();
                    for (NodoAtributo a : est.atributos()) {
                        if (a instanceof NodoAtributo.Atributo attr) {
                            String tipoC;
                            if (attr.tipoEstructura() != null) {
                                // Referencia a otra estructura: resolver alias si existe.
                                tipoC = "struct " + ctx.nombreEstructuraC(attr.tipoEstructura());
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
                    acumuladas.add(new EstructuraC(nombreC, campos));
                }
            }
        }

        // ============================================================
        // Declaración en la tabla de símbolos
        // ============================================================

        private void declararParametrosEnScope(TablaSimbolos tabla) {
            for (NodoParametro p : parametros) {
                if (p instanceof NodoParametro.Parametro par) {
                    if (par.esEstructura()) {
                        tabla.declararVariable(par.nombre(), par.tipoEstructura(),
                                false, 0, true, par.tipoEstructura(), List.of());
                    } else if (par.esArreglo()) {
                        tabla.declararVariable(par.nombre(), par.tipoPrimitivo(),
                                true, 1, false, null, List.of());
                    } else {
                        tabla.declararVariable(par.nombre(), par.tipoPrimitivo());
                    }
                }
            }
        }

        private void recogerYDeclararVariablesLocales(ContextoTraduccion ctx,
                                                      TablaSimbolos tabla,
                                                      List<NodoSentencia> sentencias,
                                                      List<VariableLocalC> acumuladas) {
            for (NodoSentencia s : sentencias) {
                switch (s) {
                    case NodoSentencia.DeclaracionVariable d -> {
                        tabla.declararVariable(d.nombre(), d.tipo());
                        if (d.tipo() != null) {
                            String tipoC;
                            if (esPrimitivoY(d.tipo())) {
                                tipoC = TipoC.primitivoAC(d.tipo());
                            } else {
                                tipoC = "struct " + ctx.nombreEstructuraC(d.tipo());
                            }
                            acumuladas.add(new VariableLocalC(tipoC, d.nombre()));
                        }
                    }
                    case NodoSentencia.DeclaracionArreglo d -> {
                        tabla.declararVariable(d.nombre(), d.tipo(),
                                true, 1, false, null, List.of(d.tamano()));
                        acumuladas.add(new VariableLocalC(
                                TipoC.primitivoAC(d.tipo()),
                                d.nombre() + "[" + d.tamano() + "]"));
                    }
                    case NodoSentencia.DeclaracionMatriz d -> {
                        tabla.declararVariable(d.nombre(), d.tipo(),
                                true, 2, false, null, List.of(d.filas(), d.columnas()));

                        String tipoBaseC = TipoC.primitivoAC(d.tipo());
                        String nombreConDim = d.nombre() + "[" + d.filas() + "][" + d.columnas() + "]";

                        String inicializadorC = null;
                        if (d.inicializacion() != null) {
                            StringBuilder sb = new StringBuilder("{");
                            for (int i = 0; i < d.inicializacion().size(); i++) {
                                if (i > 0) sb.append(", ");
                                sb.append("{");
                                List<NodoExpr> fila = d.inicializacion().get(i);
                                for (int j = 0; j < fila.size(); j++) {
                                    if (j > 0) sb.append(", ");
                                    sb.append(literalC(fila.get(j)));
                                }
                                sb.append("}");
                            }
                            sb.append("}");
                            inicializadorC = sb.toString();
                        }

                        acumuladas.add(new VariableLocalC(tipoBaseC, nombreConDim, inicializadorC));
                    }
                    case NodoSentencia.DeclaracionEstructura d -> {
                        tabla.declararVariable(d.nombre(), d.tipoEstructura(),
                                false, 0, true, d.tipoEstructura(), List.of());
                        acumuladas.add(new VariableLocalC(
                                "struct " + ctx.nombreEstructuraC(d.tipoEstructura()),
                                d.nombre()));
                    }
                    case NodoSentencia.Condicional c -> {
                        recogerYDeclararVariablesLocales(ctx, tabla, c.cuerpoSi(), acumuladas);
                        if (c.cuerpoSino() != null)
                            recogerYDeclararVariablesLocales(ctx, tabla, c.cuerpoSino(), acumuladas);
                        if (c.cuerpoContrario() != null)
                            recogerYDeclararVariablesLocales(ctx, tabla, c.cuerpoContrario(), acumuladas);
                    }
                    case NodoSentencia.CicloPara c -> {
                        tabla.declararVariable(c.nombreVariable(), c.tipoInicializacion());
                        acumuladas.add(new VariableLocalC(
                                TipoC.primitivoAC(c.tipoInicializacion()), c.nombreVariable()));
                        recogerYDeclararVariablesLocales(ctx, tabla, c.cuerpo(), acumuladas);
                    }
                    case NodoSentencia.CicloMientras c ->
                            recogerYDeclararVariablesLocales(ctx, tabla, c.cuerpo(), acumuladas);
                    case NodoSentencia.CicloHacerMientras c ->
                            recogerYDeclararVariablesLocales(ctx, tabla, c.cuerpo(), acumuladas);
                    case NodoSentencia.Elegir e -> {
                        for (NodoSentencia.CasoElegir caso : e.casos()) {
                            recogerYDeclararVariablesLocales(ctx, tabla, caso.cuerpo(), acumuladas);
                        }
                        if (e.siempre() != null) {
                            recogerYDeclararVariablesLocales(ctx, tabla, e.siempre().cuerpo(), acumuladas);
                        }
                    }
                    default -> { }
                }
            }
        }

        private static boolean esPrimitivoY(String tipo) {
            return "entero".equals(tipo) || "flotante".equals(tipo)
                    || "caracter".equals(tipo) || "cadena".equals(tipo)
                    || "bool".equals(tipo);
        }

        private static ParametroC aParametroC(ContextoTraduccion ctx, NodoParametro.Parametro p) {
            if (p.esEstructura()) {
                return new ParametroC(
                        "struct " + ctx.nombreEstructuraC(p.tipoEstructura()) + "*",
                        p.nombre());
            }
            if (p.esArreglo()) {
                return new ParametroC(TipoC.primitivoAC(p.tipoPrimitivo()) + "*", p.nombre());
            }
            return new ParametroC(TipoC.primitivoAC(p.tipoPrimitivo()), p.nombre());
        }
    }

        private void declararVariablesLocales(TablaSimbolos tabla, List<NodoSentencia> sentencias) {
            for (NodoSentencia s : sentencias) {
                switch (s) {
                    case NodoSentencia.DeclaracionVariable d -> {
                        tabla.declararVariable(d.nombre(), d.tipo());
                    }
                    case NodoSentencia.DeclaracionArreglo d -> {
                        tabla.declararVariable(d.nombre(), d.tipo(),
                                true, 1, false, null, List.of(d.tamano()));
                    }
                    case NodoSentencia.DeclaracionMatriz d -> {
                        tabla.declararVariable(d.nombre(), d.tipo(),
                                true, 2, false, null, List.of(d.filas(), d.columnas()));
                    }
                    case NodoSentencia.DeclaracionEstructura d -> {
                        tabla.declararVariable(d.nombre(), d.tipoEstructura(),
                                false, 0, true, d.tipoEstructura(), List.of());
                    }
                    case NodoSentencia.Condicional c -> {
                        declararVariablesLocales(tabla, c.cuerpoSi());
                        if (c.cuerpoSino() != null) declararVariablesLocales(tabla, c.cuerpoSino());
                        if (c.cuerpoContrario() != null) declararVariablesLocales(tabla, c.cuerpoContrario());
                    }
                    case NodoSentencia.CicloPara c -> {
                        tabla.declararVariable(c.nombreVariable(), c.tipoInicializacion());
                        declararVariablesLocales(tabla, c.cuerpo());
                    }
                    case NodoSentencia.CicloMientras c -> declararVariablesLocales(tabla, c.cuerpo());
                    case NodoSentencia.CicloHacerMientras c -> declararVariablesLocales(tabla, c.cuerpo());
                    case NodoSentencia.Elegir e -> {
                        for (NodoSentencia.CasoElegir caso : e.casos()) {
                            declararVariablesLocales(tabla, caso.cuerpo());
                        }
                        if (e.siempre() != null) {
                            declararVariablesLocales(tabla, e.siempre().cuerpo());
                        }
                    }
                    default -> { /* no declara nada */ }
                }
            }
        }

        // Conversión de parámetros a C
        private static ParametroC aParametroC(NodoParametro.Parametro p) {
            if (p.esEstructura()) {
                return new ParametroC(p.tipoEstructura() + "*", p.nombre());
            }
            if (p.esArreglo()) {
                return new ParametroC(TipoC.primitivoAC(p.tipoPrimitivo()) + "*", p.nombre());
            }
            return new ParametroC(TipoC.primitivoAC(p.tipoPrimitivo()), p.nombre());
        }

    private static String literalC(NodoExpr expr) {
        if (expr instanceof NodoExpr.LiteralEntero l) return String.valueOf(l.valor());
        if (expr instanceof NodoExpr.LiteralFlotante l) return String.valueOf(l.valor());
        if (expr instanceof NodoExpr.LiteralCaracter l) return "'" + l.valor() + "'";
        if (expr instanceof NodoExpr.LiteralCadena l) return "\"" + l.valor() + "\"";
        if (expr instanceof NodoExpr.LiteralBool l) return l.valor() ? "1" : "0";
        return "0";
    }
    }
