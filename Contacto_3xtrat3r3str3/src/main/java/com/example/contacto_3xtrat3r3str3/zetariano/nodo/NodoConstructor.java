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

public record NodoConstructor(int linea, int columna, String nombre,
                              List<NodoParametroZ> parametros,
                              List<NodoSentencia> cuerpo) implements NodoAST {

    /**
     * Traduce este constructor a una FuncionC.
     * El nombre C es 'Clase_constructor[_tipo1_tipo2_...]'.
     */
    public FuncionC aFuncionC(TablaSimbolosZ tabla, String nombreClase) {
        GestorCodigoIntermedio gestor = new GestorCodigoIntermedio();

        // Nombre C del constructor.
        String nombreC = construirNombreC(nombreClase);

        ContextoTraduccionZ ctx = new ContextoTraduccionZ(gestor, tabla, nombreClase, nombreC);

        // Entrar al scope del constructor.
        tabla.entrarScope(nombreC);
        try {
            // Declarar parámetros en la tabla.
            for (NodoParametroZ p : parametros) {
                tabla.declararVariable(p.nombre(), p.tipo());
            }

            // Declarar variables locales.
            List<VariableLocalC> locales = new ArrayList<>();
            declararLocales(tabla, cuerpo, locales);

            // Traducir cuerpo.
            for (NodoSentencia s : cuerpo) {
                s.aCodigoIntermedio(ctx);
            }

            // Construir FuncionC.
            List<ParametroC> paramsC = new ArrayList<>();
            // 'this' como primer parámetro.
            paramsC.add(new ParametroC("struct " + nombreClase + "*", "this"));
            // Parámetros reales del constructor.
            for (NodoParametroZ p : parametros) {
                paramsC.add(aParametroC(p));
            }

            List<Cuarteta> cuartetas =
                    gestor.getCuartetas();
            List<String> tiposTemporales = gestor.getContador().getTiposTemporales();

            return new FuncionC("void", nombreC, paramsC, locales,
                    new ArrayList<>(), cuartetas, tiposTemporales);
        } finally {
            tabla.salirScope();
        }
    }

    // Helpers
    private String construirNombreC(String nombreClase) {
        StringBuilder sb = new StringBuilder();
        sb.append(nombreClase).append("_constructor");
        for (NodoParametroZ p : parametros) {
            sb.append('_').append(p.tipo());
        }
        return sb.toString();
    }

    private static ParametroC aParametroC(NodoParametroZ p) {
        String tipoC;
        if (TipoCZ.esPrimitivo(p.tipo())) {
            tipoC = TipoCZ.baseValorAC(p.tipo());
        } else {
            tipoC = "struct " + p.tipo() + "*";
        }
        return new ParametroC(tipoC, p.nombre());
    }

    private void declararLocales(TablaSimbolosZ tabla,
                                 List<NodoSentencia> sentencias,
                                 List<VariableLocalC> acumuladas) {
        for (NodoSentencia s : sentencias) {
            switch (s) {
                case NodoSentencia.DeclaracionVariable d -> {
                    if (d.dimensiones() > 0) {
                        // Pendiente: arreglos.
                        continue;
                    }
                    tabla.declararVariable(d.nombre(), d.tipo(), 0);
                    acumuladas.add(new VariableLocalC(
                            TipoCZ.baseAC(d.tipo(), !TipoCZ.esPrimitivo(d.tipo())),
                            d.nombre()));
                }
                case NodoSentencia.Condicional c -> {
                    declararLocales(tabla, c.cuerpoSi(), acumuladas);
                    if (c.cuerpoSino() != null) declararLocales(tabla, c.cuerpoSino(), acumuladas);
                }
                case NodoSentencia.CicloPara c -> {
                    if (c.inicializacion() instanceof NodoSentencia.DeclaracionVariable d) {
                        tabla.declararVariable(d.nombre(), d.tipo(), 0);
                        acumuladas.add(new VariableLocalC(
                                TipoCZ.baseAC(d.tipo(), !TipoCZ.esPrimitivo(d.tipo())),
                                d.nombre()));
                    }
                    declararLocales(tabla, c.cuerpo(), acumuladas);
                }
                case NodoSentencia.CicloMientras c -> declararLocales(tabla, c.cuerpo(), acumuladas);
                case NodoSentencia.CicloHacerMientras c -> declararLocales(tabla, c.cuerpo(), acumuladas);
                case NodoSentencia.Switch sw -> {
                    for (NodoSentencia.CasoSwitch caso : sw.casos()) {
                        declararLocales(tabla, caso.cuerpo(), acumuladas);
                    }
                    if (sw.casoDefault() != null) {
                        declararLocales(tabla, sw.casoDefault().cuerpo(), acumuladas);
                    }
                }
                default -> { }
            }
        }
    }
}