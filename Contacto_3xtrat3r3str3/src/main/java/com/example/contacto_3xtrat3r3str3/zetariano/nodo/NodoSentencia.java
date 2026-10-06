package com.example.contacto_3xtrat3r3str3.zetariano.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.z.*;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ.ImprimirZ;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ.LiteralZ;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ.RomperSwitch;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ.Switch1;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.*;

import java.util.ArrayList;
import java.util.List;

//Nodos de sentencia del AST del lenguaje .z Cada record sabe traducirse a cuartetas con aCodigoIntermedio()
public sealed interface NodoSentencia extends NodoAST permits
        NodoSentencia.DeclaracionVariable,
        NodoSentencia.Asignacion,
        NodoSentencia.ExpresionComoSentencia,
        NodoSentencia.Condicional,
        NodoSentencia.Switch,
        NodoSentencia.CasoSwitch,
        NodoSentencia.CasoDefault,
        NodoSentencia.CicloPara,
        NodoSentencia.CicloMientras,
        NodoSentencia.CicloHacerMientras,
        NodoSentencia.Retorno,
        NodoSentencia.Imprimir,
        NodoSentencia.Leer,
        NodoSentencia.Romper,
        NodoSentencia.Continuar {

    TipoNodoSentencia tipoNodo();

    void aCodigoIntermedio(ContextoTraduccionZ ctx);

    // DECLARACION
    /** 'int x = 25;'  ->  emite una asignación si hay inicializador. */
    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               int dimensiones, NodoExpr inicializacion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.DECLARACION_VARIABLE; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            if (inicializacion == null) return;

            // ListaLiteral: pendiente.
            if (inicializacion instanceof NodoExpr.ListaLiteral) {
                throw new UnsupportedOperationException(
                        "Inicialización de arreglos con { ... } aún no soportada (línea " + linea + ")");
            }

            // Caso general: inicialización con expresión (ej. new int[5], new Persona(...)).
            AccesoMemoria valorAcc = inicializacion.aCodigoIntermedio(ctx);

            String tipoC;
            if (dimensiones == 0) {
                tipoC = TipoCZ.esPrimitivo(tipo)
                        ? TipoCZ.baseValorAC(tipo)
                        : "struct " + tipo + "*";
            } else {
                String tipoBaseC = TipoCZ.esPrimitivo(tipo)
                        ? TipoCZ.baseValorAC(tipo)
                        : "struct " + tipo;
                tipoC = tipoBaseC + "*".repeat(dimensiones);
            }
            AccesoVariable destinoAcc = new AccesoVariable(nombre, tipoC);

            ctx.getGestor().emitir(new AsignacionVariable(destinoAcc, valorAcc));
        }
    }

    // ASIGNACION
    /** 'x = 5;', 'x += 3;', 'x -= 2;', 'x *= 2;'  ->  emite la cuarteta correspondiente. */
    record Asignacion(int linea, int columna, String operador, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ASIGNACION; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();

            AccesoMemoria destinoAcc = destino.aCodigoIntermedio(ctx);
            AccesoMemoria valorAcc = valor.aCodigoIntermedio(ctx);

            switch (operador) {
                case "=" -> g.emitir(new AsignacionVariable(destinoAcc, valorAcc));
                case "+=" -> emitirOperacionCompuesta(g, destinoAcc, valorAcc, "+");
                case "-=" -> emitirOperacionCompuesta(g, destinoAcc, valorAcc, "-");
                case "*=" -> emitirOperacionCompuesta(g, destinoAcc, valorAcc, "*");
                default -> throw new IllegalStateException(
                        "Operador de asignación no soportado: '" + operador + "' (línea " + linea + ")");
            }
        }

        /** Emite 'x op= y' como 'x = x op y'. */
        private void emitirOperacionCompuesta(GestorCodigoIntermedio g,
                                              AccesoMemoria destino,
                                              AccesoMemoria valor,
                                              String opBinario) {
            g.emitir(new OperacionBinaria(destino, destino, opBinario, valor));
        }
    }

    // EXPRESION COMO SENTENCIA
    /** Expresión suelta: 'p1.saludar();', 'a++;', 'calcular(x);'  ->  evalúa y descarta. */
    record ExpresionComoSentencia(int linea, int columna, NodoExpr expresion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.EXPRESION_COMO_SENTENCIA;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            expresion.aCodigoIntermedio(ctx);
        }
    }

    // CONDICIONAL
    /** 'if (cond) {...} else {...}'  ->  etiquetas + saltos. */
    record Condicional(int linea, int columna, NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       List<NodoSentencia> cuerpoSino) implements NodoSentencia { // null si no hay 'else'
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CONDICIONAL; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            int lSi = c.siguienteEtiqueta();
            int lFin = c.siguienteEtiqueta();

            // Evaluar condición.
            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            int lNo = (cuerpoSino != null) ? c.siguienteEtiqueta() : lFin;

            g.emitir(new Condicional1(condAcc, "==", new LiteralZ(0, "int"), lNo));
            g.emitir(new Salto(lSi));

            // Bloque si.
            g.emitir(new DefinicionEtiqueta(lSi));
            for (NodoSentencia s : cuerpoSi) s.aCodigoIntermedio(ctx);
            g.emitir(new Salto(lFin));

            // Bloque else (si existe).
            if (cuerpoSino != null) {
                g.emitir(new DefinicionEtiqueta(lNo));
                for (NodoSentencia s : cuerpoSino) s.aCodigoIntermedio(ctx);
            }

            g.emitir(new DefinicionEtiqueta(lFin));
        }
    }

    // SWITCH
    /** 'switch (expr) { case ... default ... }'  ->  cuarteta Switch1. */
    record Switch(int linea, int columna, NodoExpr expresion,
                  List<CasoSwitch> casos, CasoDefault casoDefault) implements NodoSentencia { // casoDefault puede ser null
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.SWITCH; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            // 1. Evaluar la expresión del switch.
            AccesoMemoria exprAcc = expresion.aCodigoIntermedio(ctx);
            // 2. Activar bandera de switch.
            boolean anterior = ctx.isDentroDeSwitch();
            ctx.setDentroDeSwitch(true);
            // 3. Capturar cada caso.
            List<Switch1.Caso> casosTraducidos = new ArrayList<>();
            for (CasoSwitch caso : casos) {
                AccesoMemoria valorCaso = caso.valor().aCodigoIntermedio(ctx);
                g.empezarCaptura();
                for (NodoSentencia s : caso.cuerpo()) {
                    s.aCodigoIntermedio(ctx);
                }
                List<Cuarteta> cuerpoCapturado = g.terminarCaptura();
                casosTraducidos.add(new Switch1.Caso(valorCaso, cuerpoCapturado));
            }
            // 4. Capturar default.
            List<Cuarteta> cuerpoDefault = null;
            if (casoDefault != null) {
                g.empezarCaptura();
                for (NodoSentencia s : casoDefault.cuerpo()) {
                    s.aCodigoIntermedio(ctx);
                }
                cuerpoDefault = g.terminarCaptura();
            }
            // 5. Restaurar bandera.
            ctx.setDentroDeSwitch(anterior);
            // 6. Emitir el switch completo.
            g.emitir(new Switch1(exprAcc, casosTraducidos, cuerpoDefault));
        }
    }

    /** Caso individual dentro de 'switch'. Lo gestiona Switch. */
    record CasoSwitch(int linea, int columna, NodoExpr valor, List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CASO_SWITCH; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            throw new IllegalStateException(
                    "CasoSwitch solo puede usarse dentro de un switch (línea " + linea + ")");
        }
    }

    /** Caso 'default' dentro de 'switch'. Lo gestiona Switch. */
    record CasoDefault(int linea, int columna, List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CASO_DEFAULT; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            throw new IllegalStateException(
                    "CasoDefault solo puede usarse dentro de un switch (línea " + linea + ")");
        }
    }

    // CICLOS
    /** 'for (init; cond; act) { ... }'  ->  init + etiquetas + salto atrás. */
    record CicloPara(int linea, int columna,
                     NodoSentencia inicializacion,
                     NodoExpr condicion,
                     NodoSentencia actualizacion,
                     List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_PARA; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();
            // Inicialización (opcional).
            if (inicializacion != null) {
                inicializacion.aCodigoIntermedio(ctx);
            }
            int lInicio = c.siguienteEtiqueta();
            int lContinuar = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();
            g.emitir(new DefinicionEtiqueta(lInicio));
            // Condición (opcional).
            if (condicion != null) {
                AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
                g.emitir(new Condicional1(condAcc, "==", new LiteralZ(0, "int"), lRomper));
            }
            g.entrarCiclo(new ContextoCiclo(lContinuar, lRomper));
            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);
            g.salirCiclo();
            g.emitir(new DefinicionEtiqueta(lContinuar));
            // Actualización (opcional).
            if (actualizacion != null) {
                actualizacion.aCodigoIntermedio(ctx);
            }
            g.emitir(new Salto(lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    /** 'while (cond) { ... }'  ->  etiqueta inicio + cond + cuerpo + salto atrás. */
    record CicloMientras(int linea, int columna, NodoExpr condicion, List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_MIENTRAS; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            int lInicio = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();

            g.emitir(new DefinicionEtiqueta(lInicio));

            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "==", new LiteralZ(0, "int"), lRomper));
            g.entrarCiclo(new ContextoCiclo(lInicio, lRomper));

            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);
            g.salirCiclo();

            g.emitir(new Salto(lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    /** 'do { ... } while (cond);'  ->  cuerpo + etiqueta cond + salto si verdad. */
    record CicloHacerMientras(int linea, int columna, List<NodoSentencia> cuerpo, NodoExpr condicion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CICLO_HACER_MIENTRAS; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
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
            g.emitir(new Condicional1(condAcc, "!=", new LiteralZ(0, "int"), lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    // RETORNO
    /** 'return valor;' o 'return;'  ->  emite 'return ...'. */
    record Retorno(int linea, int columna, NodoExpr valor) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.RETORNO; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            AccesoMemoria valorAcc = (valor != null) ? valor.aCodigoIntermedio(ctx) : null;
            ctx.getGestor().emitir(new Retorno1(valorAcc));
        }
    }

    // IMPRESION / LECTURA
    /** 'println(expr)' / 'print(expr)'  ->  ImprimirZ (con o sin \n). */
    record Imprimir(int linea, int columna, boolean saltoDeLinea, NodoExpr expresion) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.IMPRIMIR; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            AccesoMemoria valorAcc = expresion.aCodigoIntermedio(ctx);
            ctx.getGestor().emitir(new ImprimirZ(valorAcc, valorAcc.getTipo(), saltoDeLinea));
        }
    }

    /** 'readln();'  ->  sin destino no emite nada. */
    record Leer(int linea, int columna) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.LEER; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
        }
    }

    /** 'break;'  ->  break de switch o goto de fin de ciclo. */
    record Romper(int linea, int columna) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.ROMPER; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            // Si esta dentro de un switch, emite 'break;' de C.
            if (ctx.isDentroDeSwitch()) {
                ctx.getGestor().emitir(new RomperSwitch());
                return;
            }
            // Si no, es un break de ciclo normal.
            ContextoCiclo cc = ctx.getGestor().cicloActual();
            if (cc == null) {
                throw new IllegalStateException(
                        "'break' fuera de un ciclo o switch (línea " + linea + ")");
            }
            ctx.getGestor().emitir(new Romper1(cc.getEtiquetaRomper()));
        }
    }

    /** 'continue;'  ->  goto de continuación del ciclo activo. */
    record Continuar(int linea, int columna) implements NodoSentencia {
        @Override public TipoNodoSentencia tipoNodo() { return TipoNodoSentencia.CONTINUAR; }

        @Override
        public void aCodigoIntermedio(ContextoTraduccionZ ctx) {
            ContextoCiclo cc = ctx.getGestor().cicloActual();
            if (cc == null) {
                throw new IllegalStateException(
                        "'continue' fuera de un ciclo (línea " + linea + ")");
            }
            ctx.getGestor().emitir(new Continuar1(cc.getEtiquetaContinuar()));
        }
    }
}