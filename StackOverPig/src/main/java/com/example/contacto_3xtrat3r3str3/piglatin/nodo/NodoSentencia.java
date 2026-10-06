package com.example.contacto_3xtrat3r3str3.piglatin.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig.ContextoTraduccionPig;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.pig.TipoPigC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig.ImprimirPig;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig.LeerPig;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasPig.LiteralPig;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY.AsignacionAtributo;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.*;

import java.util.ArrayList;
import java.util.List;

public sealed interface NodoSentencia extends NodoAST permits
        NodoSentencia.DeclaracionVariable,
        NodoSentencia.DeclaracionArreglo,
        NodoSentencia.DeclaracionStruct,
        NodoSentencia.Asignacion,
        NodoSentencia.IncrementoDecremento,
        NodoSentencia.Condicional,
        NodoSentencia.CicloDum,
        NodoSentencia.CicloFacere,
        NodoSentencia.CicloPer,
        NodoSentencia.Lectura,
        NodoSentencia.Escritura,
        NodoSentencia.InterrupcionCiclo,
        NodoSentencia.LlamadaFuncionSentencia,
        NodoSentencia.LlamadaMetodoSentencia {

    TipoNodoSentencia tipoNodo();

    void aCodigoIntermedio(ContextoTraduccionPig ctx);

    // DECLARACIONES
    /** 'esto x : tipo valor'  ->  emite una asignación si hay inicializador. */
    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               NodoExpr inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_VARIABLE; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            if (inicializacion == null) return;
            AccesoMemoria valorAcc = inicializacion.aCodigoIntermedio(ctx);
            String tipoC = TipoPigC.baseAC(tipo, !TipoPigC.esPrimitivo(tipo));
            AccesoVariable destinoAcc = new AccesoVariable(nombre, tipoC);
            ctx.getGestor().emitir(new AsignacionVariable(destinoAcc, valorAcc));
        }
    }

    /** 'series a[N] : tipo { ... }'  ->  la declaración la maneja el recogedor. */
    record DeclaracionArreglo(int linea, int columna, String tipo, String nombre,
                              int tamano, List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_ARREGLO; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
        }
    }

    /** 'esto x : Tipo { ... }'  ->  asigna cada campo en orden posicional. */
    record DeclaracionStruct(int linea, int columna, String tipo, String nombre,
                             List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_STRUCT; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            if (inicializacion.isEmpty()) return;
            var estOpt = ctx.getTabla().buscarEstructura(tipo);
            if (estOpt.isEmpty()) return;
            var est = estOpt.get();
            var nombresCampos = new ArrayList<>(est.atributos().keySet());
            for (int i = 0; i < inicializacion.size() && i < nombresCampos.size(); i++) {
                AccesoMemoria valor = inicializacion.get(i).aCodigoIntermedio(ctx);
                AccesoVariable base = new AccesoVariable(nombre, "struct " + tipo);
                ctx.getGestor().emitir(new AsignacionAtributo(
                        base, nombresCampos.get(i), false, valor));
            }
        }
    }

    // ASIGNACIONES
    /** 'x = 5'  ->  emite 'x = 5'. */
    record Asignacion(int linea, int columna, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ASIGNACION; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            AccesoMemoria destinoAcc = destino.aCodigoIntermedio(ctx);
            AccesoMemoria valorAcc = valor.aCodigoIntermedio(ctx);
            ctx.getGestor().emitir(new AsignacionVariable(destinoAcc, valorAcc));
        }
    }

    /** 'x++' / 'x--'  ->  emite 'x = x ± 1'. */
    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.INCREMENTO_DECREMENTO; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            AccesoMemoria op = operando.aCodigoIntermedio(ctx);
            String opBinario = "++".equals(operador) ? "+" : "-";
            AccesoMemoria uno = new LiteralPig(1, "numerus");
            ctx.getGestor().emitir(new OperacionBinaria(op, op, opBinario, uno));
        }
    }

    // CONDICIONAL
    /** 'si ... aliter ... aliter ...'  ->  etiquetas + saltos. */
    record Condicional(int linea, int columna, NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       List<RamaAliter> ramasAliter,
                       List<NodoSentencia> cuerpoAliter) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CONDICIONAL; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            int lFin = c.siguienteEtiqueta();
            int lSiguiente = c.siguienteEtiqueta();

            // Rama si
            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "==", new LiteralPig(0, "numerus"), lSiguiente));
            for (NodoSentencia s : cuerpoSi) s.aCodigoIntermedio(ctx);
            g.emitir(new Salto(lFin));
            g.emitir(new DefinicionEtiqueta(lSiguiente));
            // Ramas aliter con condición
            for (RamaAliter rama : ramasAliter) {
                int lSiguienteRama = c.siguienteEtiqueta();
                AccesoMemoria condRama = rama.condicion().aCodigoIntermedio(ctx);
                g.emitir(new Condicional1(condRama, "==", new LiteralPig(0, "numerus"), lSiguienteRama));

                for (NodoSentencia s : rama.cuerpo()) s.aCodigoIntermedio(ctx);
                g.emitir(new Salto(lFin));

                g.emitir(new DefinicionEtiqueta(lSiguienteRama));
            }

            // Rama else final
            if (cuerpoAliter != null) {
                for (NodoSentencia s : cuerpoAliter) s.aCodigoIntermedio(ctx);
            }

            g.emitir(new DefinicionEtiqueta(lFin));
        }
    }

    /** Rama 'aliter (cond)' con su cuerpo. La gestiona Condicional. */
    record RamaAliter(int linea, int columna, NodoExpr condicion, List<NodoSentencia> cuerpo) {
    }

    // CICLOS
    /** 'dum (cond) { ... } finis'  ->  etiqueta inicio + cond + cuerpo + salto atrás. */
    record CicloDum(int linea, int columna, NodoExpr condicion,
                    List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_DUM; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();
            int lInicio = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();

            g.emitir(new DefinicionEtiqueta(lInicio));

            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "==", new LiteralPig(0, "numerus"), lRomper));
            g.entrarCiclo(new ContextoCiclo(lInicio, lRomper));
            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);
            g.salirCiclo();

            g.emitir(new Salto(lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    /** 'facere { ... } dum (cond);'  ->  cuerpo + etiqueta cond + salto si verdad. */
    record CicloFacere(int linea, int columna, List<NodoSentencia> cuerpo,
                       NodoExpr condicion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_FACERE; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            int lInicio = c.siguienteEtiqueta();
            int lCondicion = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();

            g.emitir(new DefinicionEtiqueta(lInicio));
            g.entrarCiclo(new ContextoCiclo(lCondicion, lRomper));

            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);

            g.salirCiclo();
            g.emitir(new DefinicionEtiqueta(lCondicion));

            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "!=", new LiteralPig(0, "numerus"), lInicio));

            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    /** 'per (init; cond; act) { ... }'  ->  init + etiquetas + salto atrás. */
    record CicloPer(int linea, int columna, NodoSentencia inicializacion,
                    NodoExpr condicion, NodoSentencia actualizacion,
                    List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_PER; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            if (inicializacion != null) inicializacion.aCodigoIntermedio(ctx);
            int lInicio = c.siguienteEtiqueta();
            int lContinuar = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();

            g.emitir(new DefinicionEtiqueta(lInicio));

            if (condicion != null) {
                AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
                g.emitir(new Condicional1(condAcc, "==", new LiteralPig(0, "numerus"), lRomper));
            }

            g.entrarCiclo(new ContextoCiclo(lContinuar, lRomper));
            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);
            g.salirCiclo();

            g.emitir(new DefinicionEtiqueta(lContinuar));
            if (actualizacion != null) actualizacion.aCodigoIntermedio(ctx);

            g.emitir(new Salto(lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    // LECTURA / ESCRITURA
    /** 'var <<'  o  '<<'  ->  LeerPig (con destino o a temporal descartable). */
    record Lectura(int linea, int columna, String variable) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LECTURA; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            if (variable == null) {
                int idT = ctx.getGestor().getContador().siguienteTemporal("textum");
                AccesoTemporal t = new AccesoTemporal(idT, "textum");
                ctx.getGestor().emitir(new LeerPig(t, "textum"));
                return;
            }
            var simboloOpt = ctx.getTabla().buscarVariable(variable);
            if (simboloOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Variable no encontrada: '" + variable + "' (línea " + linea + ")");
            }
            var simbolo = simboloOpt.get();
            AccesoVariable destino = new AccesoVariable(variable,
                    TipoPigC.baseAC(simbolo.tipo(), simbolo.esObjeto()));
            ctx.getGestor().emitir(new LeerPig(destino, simbolo.tipo()));
        }
    }

    /** '>> a >> b >> c;'  ->  un ImprimirPig por cada valor. */
    record Escritura(int linea, int columna, List<NodoExpr> valores) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ESCRITURA; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            for (NodoExpr valor : valores) {
                AccesoMemoria acc = valor.aCodigoIntermedio(ctx);
                String tipoPig = tipoPigLatinDe(valor, ctx);
                ctx.getGestor().emitir(new ImprimirPig(acc, tipoPig));
            }
        }

        /** Determina el tipo .pig del valor para elegir el formato de printf. */
        private String tipoPigLatinDe(NodoExpr expr, ContextoTraduccionPig ctx) {
            if (expr instanceof NodoExpr.LiteralEntero) return "numerus";
            if (expr instanceof NodoExpr.LiteralDecimal) return "decimalis";
            if (expr instanceof NodoExpr.LiteralTexto) return "textum";
            if (expr instanceof NodoExpr.LiteralCaracter) return "littera";
            if (expr instanceof NodoExpr.LiteralBool) return "bool";

            if (expr instanceof NodoExpr.Identificador id) {
                return ctx.getTabla().buscarVariable(id.nombre())
                        .map(s -> s.tipo())
                        .orElse("numerus");
            }
            if (expr instanceof NodoExpr.LlamadaFuncion llamada) {
                return ctx.getTabla().buscarFuncion(llamada.nombre())
                        .map(f -> f.tipoRetorno())
                        .orElse("numerus");
            }
            if (expr instanceof NodoExpr.LlamadaMetodo llamada) {
                try {
                    if (llamada.objeto() instanceof NodoExpr.Identificador id) {
                        var v = ctx.getTabla().buscarVariable(id.nombre());
                        if (v.isPresent() && v.get().esObjeto()) {
                            var clase = ctx.getTabla().buscarClase(v.get().tipo());
                            if (clase.isPresent()) {
                                for (var m : clase.get().metodos()) {
                                    if (m.nombre().equals(llamada.nombre())) {
                                        return m.tipoRetorno() != null ? m.tipoRetorno() : "numerus";
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                }
                return "numerus";
            }

            return "numerus";
        }
    }

    // INTERRUPCION DE CICLO
    /** 'perge' o 'interrumpe'  ->  goto a la etiqueta del ciclo activo. */
    record InterrupcionCiclo(int linea, int columna, String tipo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.INTERRUPCION_CICLO; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            ContextoCiclo cc = ctx.getGestor().cicloActual();
            if (cc == null) {
                throw new IllegalStateException(
                        "'" + tipo + "' fuera de un ciclo (línea " + linea + ")");
            }
            if ("perge".equals(tipo)) {
                ctx.getGestor().emitir(new Continuar1(cc.getEtiquetaContinuar()));
            } else {
                ctx.getGestor().emitir(new Romper1(cc.getEtiquetaRomper()));
            }
        }
    }

    // LLAMADAS COMO SENTENCIA
    /** Llamada a función suelta  ->  evalúa y descarta. */
    record LlamadaFuncionSentencia(int linea, int columna, NodoExpr.LlamadaFuncion llamada) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LLAMADA_FUNCION_SENTENCIA; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            llamada.aCodigoIntermedio(ctx);
        }
    }

    /** Llamada a metodo suelta  ->  evalúa y descarta. */
    record LlamadaMetodoSentencia(int linea, int columna, NodoExpr.LlamadaMetodo llamada) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LLAMADA_METODO_SENTENCIA; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionPig ctx) {
            llamada.aCodigoIntermedio(ctx);
        }
    }
}