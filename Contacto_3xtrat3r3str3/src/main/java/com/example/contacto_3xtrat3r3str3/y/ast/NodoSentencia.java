package com.example.contacto_3xtrat3r3str3.y.ast;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.ContextoTraduccion;
import com.example.contacto_3xtrat3r3str3.c3d_v2.c.TipoC;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasY.*;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.*;

import java.util.List;

// Nodos de sentencia del AST del lenguaje .y. Cada record sabe traducirse a cuartetas con aCodigoIntermedio()
public sealed interface NodoSentencia extends NodoAST permits
        NodoSentencia.DeclaracionVariable,
        NodoSentencia.DeclaracionArreglo,
        NodoSentencia.DeclaracionMatriz,
        NodoSentencia.DeclaracionEstructura,
        NodoSentencia.DeclaracionEstructuraLocal,
        NodoSentencia.Asignacion,
        NodoSentencia.IncrementoDecremento,
        NodoSentencia.Condicional,
        NodoSentencia.Elegir,
        NodoSentencia.CasoElegir,
        NodoSentencia.SiempreElegir,
        NodoSentencia.CicloPara,
        NodoSentencia.CicloMientras,
        NodoSentencia.CicloHacerMientras,
        NodoSentencia.Retorno,
        NodoSentencia.Imprimir,
        NodoSentencia.Leer,
        NodoSentencia.Romper,
        NodoSentencia.Continuar {
    TipoNodoSentencia tipoNodo();

    void aCodigoIntermedio(ContextoTraduccion ctx);

    // DECLARACIONES

    // 'entero x = 5'  ->  emite una asignación si hay inicializador
    record DeclaracionVariable(int linea, int columna, String tipo, String nombre,
                               NodoExpr inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_VARIABLE;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            if (inicializacion != null) {
                AccesoMemoria valor = inicializacion.aCodigoIntermedio(ctx);
                AccesoVariable destino = new AccesoVariable(nombre, tipo);
                ctx.getGestor().emitir(new AsignacionVariable(destino, valor));
            }
        }
    }

    // 'entero a[5] = {1,2,3,4,5}'  ->  emite una asignación por elemento
    record DeclaracionArreglo(int linea, int columna, String tipo, String nombre,
                              int tamano, List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ARREGLO;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            for (int i = 0; i < inicializacion.size(); i++) {
                AccesoMemoria valor = inicializacion.get(i).aCodigoIntermedio(ctx);
                AccesoVariable base = new AccesoVariable(nombre, tipo + "*");
                Literal indice = new Literal(i, "entero");
                g.emitir(new AsignacionArreglo(base, indice, valor));
            }
        }
    }

    // 'entero m[3][3]'  ->  sin cuarteta: la declaración la maneja el recogedor.
    record DeclaracionMatriz(int linea, int columna, String tipo, String nombre,
                             int filas, int columnas,
                             List<List<NodoExpr>> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_MATRIZ;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
        }
    }

    // 'Punto p = {10, 20}'  ->  asigna cada campo en orden posicional.
    record DeclaracionEstructura(int linea, int columna, String tipoEstructura, String nombre,
                                 List<NodoExpr> inicializacion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ESTRUCTURA;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            if (inicializacion.isEmpty()) return;

            // La inicialización es posicional: se asignan los campos en el orden
            // en que aparecen en la estructura.
            var estOpt = ctx.getTabla().buscarEstructura(tipoEstructura);
            if (estOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Estructura no declarada: '" + tipoEstructura + "' (línea " + linea + ")");
            }
            var est = estOpt.get();
            var nombresCampos = List.copyOf(est.atributos().keySet());

            for (int i = 0; i < inicializacion.size() && i < nombresCampos.size(); i++) {
                AccesoMemoria valor = inicializacion.get(i).aCodigoIntermedio(ctx);
                AccesoVariable base = new AccesoVariable(nombre, tipoEstructura);
                g.emitir(new AsignacionAtributo(base, nombresCampos.get(i), false, valor));
            }
        }
    }

    // Estructura declarada dentro de una función. No emite cuarteta
    record DeclaracionEstructuraLocal(int linea, int columna,
                                      NodoEstructura.Estructura estructura) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.DECLARACION_ESTRUCTURA_LOCAL;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            // No es una instrucción ejecutable; solo info de tipos.
        }
    }

    // ASIGNACION E INCREMENTO/DECREMENTO
    // 'x = 5'  ->  emite 'x = 5'
    record Asignacion(int linea, int columna, NodoExpr destino, NodoExpr valor) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ASIGNACION;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();

            AccesoMemoria destinoAcc = destino.aCodigoIntermedio(ctx);
            AccesoMemoria valorAcc = valor.aCodigoIntermedio(ctx);

            g.emitir(new AsignacionVariable(destinoAcc, valorAcc));
        }
    }

    // 'x++'  ->  emite 'x = x + 1'
    record IncrementoDecremento(int linea, int columna, String operador,
                                String nombre) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.INCREMENTO_DECREMENTO;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            String tipoC = ctx.getTabla().buscarVariable(nombre)
                    .map(TipoC::aTipoC)
                    .orElse("int");
            AccesoVariable x = new AccesoVariable(nombre, tipoC);
            String opBinario = "++".equals(operador) ? "+" : "-";
            Literal uno = new Literal(1, "entero");
            g.emitir(new OperacionBinaria(x, x, opBinario, uno));
        }
    }

    // CONDICIONAL

    // 'si (cond) entonces ... sino ... contrario ...'  ->  etiquetas + saltos.
    record Condicional(int linea, int columna,
                       NodoExpr condicion,
                       List<NodoSentencia> cuerpoSi,
                       NodoExpr condicionSino,
                       List<NodoSentencia> cuerpoSino,
                       List<NodoSentencia> cuerpoContrario) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CONDICIONAL;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            int lEtiquetaSi = c.siguienteEtiqueta();
            int lEtiquetaFin = c.siguienteEtiqueta();
            int lEtiquetaSino = (condicionSino != null) ? c.siguienteEtiqueta() : -1;

            // Evaluar condición principal
            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "!=", new Literal(0, "entero"), lEtiquetaSi));
            g.emitir(new Salto(condicionSino != null ? lEtiquetaSino : lEtiquetaFin));

            // Bloque 'si'
            g.emitir(new DefinicionEtiqueta(lEtiquetaSi));
            for (NodoSentencia s : cuerpoSi) s.aCodigoIntermedio(ctx);
            g.emitir(new Salto(lEtiquetaFin));

            // Bloque 'sino'
            if (condicionSino != null) {
                g.emitir(new DefinicionEtiqueta(lEtiquetaSino));
                AccesoMemoria condSinoAcc = condicionSino.aCodigoIntermedio(ctx);
                g.emitir(new Condicional1(condSinoAcc, "!=", new Literal(0, "entero"), lEtiquetaFin));
                for (NodoSentencia s : cuerpoSino) s.aCodigoIntermedio(ctx);
                g.emitir(new Salto(lEtiquetaFin));
            }

            // Bloque 'contrario'
            if (cuerpoContrario != null) {
                for (NodoSentencia s : cuerpoContrario) s.aCodigoIntermedio(ctx);
            }

            g.emitir(new DefinicionEtiqueta(lEtiquetaFin));
        }
    }

    // ELEGIR
    // 'elegir (x): caso 1: ... siempre: ...'  ->  etiquetas + saltos.
    record Elegir(int linea, int columna, NodoExpr expresion,
                  List<CasoElegir> casos, SiempreElegir siempre) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ELEGIR;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            AccesoMemoria exprAcc = expresion.aCodigoIntermedio(ctx);
            int lFin = c.siguienteEtiqueta();

            // Cada caso: etiqueta propia + comparación
            int[] etiquetasCasos = new int[casos.size()];
            for (int i = 0; i < casos.size(); i++) {
                etiquetasCasos[i] = c.siguienteEtiqueta();
            }
            int lSiempre = (siempre != null) ? c.siguienteEtiqueta() : lFin;

            // Evaluar cada caso y saltar a su etiqueta si coincide
            for (int i = 0; i < casos.size(); i++) {
                CasoElegir caso = casos.get(i);
                AccesoMemoria valorCaso = caso.valor().aCodigoIntermedio(ctx);
                g.emitir(new Condicional1(exprAcc, "==", valorCaso, etiquetasCasos[i]));
            }
            // Si ningún caso coincide: al 'siempre' o al fin
            g.emitir(new Salto(lSiempre));

            // Cuerpos de los casos
            for (int i = 0; i < casos.size(); i++) {
                g.emitir(new DefinicionEtiqueta(etiquetasCasos[i]));
                for (NodoSentencia s : casos.get(i).cuerpo()) s.aCodigoIntermedio(ctx);
                g.emitir(new Salto(lFin));
            }

            // Cuerpo 'siempre'
            if (siempre != null) {
                g.emitir(new DefinicionEtiqueta(lSiempre));
                for (NodoSentencia s : siempre.cuerpo()) s.aCodigoIntermedio(ctx);
                g.emitir(new Salto(lFin));
            }

            g.emitir(new DefinicionEtiqueta(lFin));
        }
    }

    // Caso individual dentro de 'elegir'. Lo gestiona Elegir.
    record CasoElegir(int linea, int columna, NodoExpr valor,
                      List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ELEGIR;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            // Gestionado por Elegir.aCodigoIntermedio.
        }
    }

    // 'siempre' (default) dentro de 'elegir'. Lo gestiona Elegir
    record SiempreElegir(int linea, int columna,
                         List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ELEGIR;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
        }
    }

    // CICLOS
    // 'para(entero i=0; i<10; i++): ...'  -> </10;>  init + etiquetas + salto atrás.
    record CicloPara(int linea, int columna,
                     String tipoInicializacion, String nombreVariable,
                     NodoExpr valorInicial, NodoExpr condicion,
                     String operadorActualizacion,
                     List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_PARA;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            // Inicialización: tipo nombre = valorInicial
            AccesoMemoria valorInit = valorInicial.aCodigoIntermedio(ctx);
            AccesoVariable var = new AccesoVariable(nombreVariable, tipoInicializacion);
            g.emitir(new AsignacionVariable(var, valorInit));

            int lInicio = c.siguienteEtiqueta();
            int lContinuar = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();

            g.emitir(new DefinicionEtiqueta(lInicio));

            // Condición: si NO se cumple, salta al fin
            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "==", new Literal(0, "entero"), lRomper));

            // Entrar al ciclo (para romper/continuar)
            g.entrarCiclo(new ContextoCiclo(lContinuar, lRomper));

            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);

            g.salirCiclo();

            // Actualización
            g.emitir(new DefinicionEtiqueta(lContinuar));
            AccesoVariable varAct = new AccesoVariable(nombreVariable, tipoInicializacion);
            String opBin = "++".equals(operadorActualizacion) ? "+" : "-";
            g.emitir(new OperacionBinaria(varAct, varAct, opBin, new Literal(1, "entero")));

            g.emitir(new Salto(lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    // 'mientras(cond) hacer ...'  ->  etiqueta inicio + cond + cuerpo + salto atrás.
    record CicloMientras(int linea, int columna,
                         NodoExpr condicion,
                         List<NodoSentencia> cuerpo) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_MIENTRAS;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            int lInicio = c.siguienteEtiqueta();
            int lRomper = c.siguienteEtiqueta();

            g.emitir(new DefinicionEtiqueta(lInicio));

            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);
            g.emitir(new Condicional1(condAcc, "==", new Literal(0, "entero"), lRomper));

            g.entrarCiclo(new ContextoCiclo(lInicio, lRomper));

            for (NodoSentencia s : cuerpo) s.aCodigoIntermedio(ctx);

            g.salirCiclo();

            g.emitir(new Salto(lInicio));
            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    // 'hacer: ... mientras(cond)'  ->  cuerpo + etiqueta cond + salto si verdad.
    record CicloHacerMientras(int linea, int columna,
                              List<NodoSentencia> cuerpo,
                              NodoExpr condicion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CICLO_HACER_MIENTRAS;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
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
            g.emitir(new Condicional1(condAcc, "!=", new Literal(0, "entero"), lInicio));

            g.emitir(new DefinicionEtiqueta(lRomper));
        }
    }

    // RETORNO
    // 'retornar expr'  ->  emite 'return expr'.
    record Retorno(int linea, int columna, NodoExpr valor) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.RETORNO;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            AccesoMemoria valorAcc = (valor != null) ? valor.aCodigoIntermedio(ctx) : null;
            ctx.getGestor().emitir(new Retorno1(valorAcc));
        }
    }

    // FUNCIONES ESPECIALES
    // 'imprimir(expr)'  ->  emite 'print expr'
    record Imprimir(int linea, int columna, NodoExpr expresion) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.IMPRIMIR;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            AccesoMemoria valorAcc = expresion.aCodigoIntermedio(ctx);
            ctx.getGestor().emitir(new Imprimir1(valorAcc, valorAcc.getTipo()));
        }
    }

    // 'leer()'  ->  sin destino no emite nada
    record Leer(int linea, int columna) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.LEER;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            // 'leer()' sin destino no hace nada útil.
        }
    }

    // CONTROL DE CICLOS
    // 'romper'  ->  goto a la etiqueta de fin del ciclo activo.
    record Romper(int linea, int columna) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.ROMPER;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            ContextoCiclo cc = ctx.getGestor().cicloActual();
            if (cc == null) {
                throw new IllegalStateException(
                        "'romper' fuera de un ciclo (línea " + linea + ")");
            }
            ctx.getGestor().emitir(new Romper1(cc.getEtiquetaRomper()));
        }
    }

    // 'continuar'  ->  goto a la etiqueta de continuación del ciclo activo.
    record Continuar(int linea, int columna) implements NodoSentencia {
        @Override
        public TipoNodoSentencia tipoNodo() {
            return TipoNodoSentencia.CONTINUAR;
        }

        @Override
        public void aCodigoIntermedio(ContextoTraduccion ctx) {
            ContextoCiclo cc = ctx.getGestor().cicloActual();
            if (cc == null) {
                throw new IllegalStateException(
                        "'continuar' fuera de un ciclo (línea " + linea + ")");
            }
            ctx.getGestor().emitir(new Continuar1(cc.getEtiquetaContinuar()));
        }
    }
}