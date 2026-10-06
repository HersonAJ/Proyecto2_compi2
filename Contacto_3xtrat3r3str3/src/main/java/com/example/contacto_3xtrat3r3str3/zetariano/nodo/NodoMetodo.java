package com.example.contacto_3xtrat3r3str3.zetariano.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.GestorCodigoIntermedio;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.ParametroC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.VariableLocalC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.z.ContextoTraduccionZ;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.z.TipoCZ;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;

import java.util.ArrayList;
import java.util.List;

//Nodo del AST que representa un metodo de la clase
public record NodoMetodo(int linea, int columna, String nombre,
                         List<NodoParametroZ> parametros,
                         String tipoRetorno,
                         List<NodoSentencia> cuerpo) implements NodoAST {

    /** Traduce el metodo a una FuncionC lista para el generador C. */
    public FuncionC aFuncionC(TablaSimbolosZ tabla, String nombreClase) {
        GestorCodigoIntermedio gestor = new GestorCodigoIntermedio();
        String nombreC = construirNombreC(nombreClase);
        ContextoTraduccionZ ctx = new ContextoTraduccionZ(gestor, tabla, nombreClase, nombreC);

        tabla.entrarScope(nombreC);
        try {
            for (NodoParametroZ p : parametros) {
                tabla.declararVariable(p.nombre(), p.tipo());
            }

            List<VariableLocalC> locales = new ArrayList<>();
            declararLocales(tabla, cuerpo, locales);

            for (NodoSentencia s : cuerpo) {
                s.aCodigoIntermedio(ctx);
            }

            List<ParametroC> paramsC = new ArrayList<>();
            paramsC.add(new ParametroC("struct " + nombreClase + "*", "this"));
            for (NodoParametroZ p : parametros) {
                paramsC.add(aParametroC(p));
            }

            String tipoRetornoC = (tipoRetorno == null)
                    ? "void"
                    : tipoRetornoC(tipoRetorno);

            List<Cuarteta> cuartetas =
                    gestor.getCuartetas();
            List<String> tiposTemporales = gestor.getContador().getTiposTemporales();

            return new FuncionC(tipoRetornoC, nombreC, paramsC, locales,
                    new ArrayList<>(), cuartetas, tiposTemporales);
        } finally {
            tabla.salirScope();
        }
    }

    /** Nombre C: Clase_metodo_tipo1_tipo2. */
    private String construirNombreC(String nombreClase) {
        StringBuilder sb = new StringBuilder();
        sb.append(nombreClase).append('_').append(nombre);
        for (NodoParametroZ p : parametros) {
            sb.append('_').append(p.tipo());
        }
        return sb.toString();
    }

    /** Tipo de retorno en C. */
    private static String tipoRetornoC(String tipoZ) {
        if (TipoCZ.esPrimitivo(tipoZ)) {
            return TipoCZ.baseValorAC(tipoZ);
        }
        return "struct " + tipoZ + "*";
    }

    /** Convierte un parámetro del AST a ParametroC. */
    private static ParametroC aParametroC(NodoParametroZ p) {
        String tipoC;
        if (TipoCZ.esPrimitivo(p.tipo())) {
            tipoC = TipoCZ.baseValorAC(p.tipo());
        } else {
            tipoC = "struct " + p.tipo() + "*";
        }
        return new ParametroC(tipoC, p.nombre());
    }

    /** Declara todas las variables locales del metodo. */
    private void declararLocales(TablaSimbolosZ tabla,
                                 List<NodoSentencia> sentencias,
                                 List<VariableLocalC> acumuladas) {
        declararLocales(tabla, sentencias, acumuladas, new java.util.HashSet<>());
    }

    /** Declara las variables locales recursivamente, evitando repetidos. */
    private void declararLocales(TablaSimbolosZ tabla,
                                 List<NodoSentencia> sentencias,
                                 List<VariableLocalC> acumuladas,
                                 java.util.Set<String> yaDeclarados) {
        for (NodoSentencia s : sentencias) {
            switch (s) {
                case NodoSentencia.DeclaracionVariable d -> {
                    if (yaDeclarados.contains(d.nombre())) continue;
                    yaDeclarados.add(d.nombre());

                    if (d.dimensiones() == 0) {
                        // Variable simple u objeto.
                        tabla.declararVariable(d.nombre(), d.tipo(), 0);
                        acumuladas.add(new VariableLocalC(
                                TipoCZ.baseAC(d.tipo(), !TipoCZ.esPrimitivo(d.tipo())),
                                d.nombre()));
                    } else {
                        // Arreglo: en C es puntero. La reserva real viene del 'new'.
                        tabla.declararVariable(d.nombre(), d.tipo(), d.dimensiones());
                        String tipoBaseC = TipoCZ.esPrimitivo(d.tipo())
                                ? TipoCZ.baseValorAC(d.tipo())
                                : "struct " + d.tipo();
                        String tipoC = tipoBaseC + "*".repeat(d.dimensiones());
                        acumuladas.add(new VariableLocalC(tipoC, d.nombre()));
                    }
                }
                case NodoSentencia.Condicional c -> {
                    declararLocales(tabla, c.cuerpoSi(), acumuladas, yaDeclarados);
                    if (c.cuerpoSino() != null) {
                        declararLocales(tabla, c.cuerpoSino(), acumuladas, yaDeclarados);
                    }
                }
                case NodoSentencia.CicloPara c -> {
                    if (c.inicializacion() instanceof NodoSentencia.DeclaracionVariable d) {
                        if (!yaDeclarados.contains(d.nombre())) {
                            yaDeclarados.add(d.nombre());
                            if (d.dimensiones() == 0) {
                                tabla.declararVariable(d.nombre(), d.tipo(), 0);
                                acumuladas.add(new VariableLocalC(
                                        TipoCZ.baseAC(d.tipo(), !TipoCZ.esPrimitivo(d.tipo())),
                                        d.nombre()));
                            } else {
                                tabla.declararVariable(d.nombre(), d.tipo(), d.dimensiones());
                                String tipoBaseC = TipoCZ.esPrimitivo(d.tipo())
                                        ? TipoCZ.baseValorAC(d.tipo())
                                        : "struct " + d.tipo();
                                String tipoC = tipoBaseC + "*".repeat(d.dimensiones());
                                acumuladas.add(new VariableLocalC(tipoC, d.nombre()));
                            }
                        }
                    }
                    declararLocales(tabla, c.cuerpo(), acumuladas, yaDeclarados);
                }
                case NodoSentencia.CicloMientras c ->
                        declararLocales(tabla, c.cuerpo(), acumuladas, yaDeclarados);
                case NodoSentencia.CicloHacerMientras c ->
                        declararLocales(tabla, c.cuerpo(), acumuladas, yaDeclarados);
                case NodoSentencia.Switch sw -> {
                    for (NodoSentencia.CasoSwitch caso : sw.casos()) {
                        declararLocales(tabla, caso.cuerpo(), acumuladas, yaDeclarados);
                    }
                    if (sw.casoDefault() != null) {
                        declararLocales(tabla, sw.casoDefault().cuerpo(), acumuladas, yaDeclarados);
                    }
                }
                default -> {
                }
            }
        }
    }
}