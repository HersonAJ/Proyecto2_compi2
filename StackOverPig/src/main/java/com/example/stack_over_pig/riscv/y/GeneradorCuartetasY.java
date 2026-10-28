package com.example.stack_over_pig.riscv.y;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.ImprimirPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.LeerPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.riscv.FuncionRiscV;
import com.example.stack_over_pig.y.ast.*;
import com.example.stack_over_pig.y.semantica.TablaSimbolos;

import java.util.*;

/**
 * Genera cuartetas (C3D) para una función del lenguaje Y.
 * Un GeneradorCuartetasY sirve para UNA sola función (estado por función).
 */
public class GeneradorCuartetasY {

    private final TablaSimbolos tabla;
    private final GestorCodigoIntermedio gestor;
    private final Map<String, String> tiposVariables = new LinkedHashMap<>();
    private AccesoMemoria ultimoResultado;
    private String nombreFuncionActual;
    private String tipoRetornoActual;

    public GeneradorCuartetasY(TablaSimbolos tabla) {
        this.tabla = tabla;
        this.gestor = new GestorCodigoIntermedio();
    }

    // PUNTO DE ENTRADA

    public FuncionRiscV generarFuncion(NodoFuncion.Funcion f) {
        this.nombreFuncionActual = f.nombre();
        this.tipoRetornoActual = f.tipoRetorno();
        List<FuncionRiscV.ParametroRiscV> params = new ArrayList<>();
        String nombre = f.nombre();
        for (NodoParametro p : f.parametros()) {
            if (p instanceof NodoParametro.Parametro par) {
                String tipoPig = par.esEstructura() ? par.tipoEstructura()
                        : tipoYaPig(par.tipoPrimitivo());
                boolean porRef = par.esArreglo() || par.esEstructura();
                tiposVariables.put(par.nombre(), tipoPig);
                params.add(new FuncionRiscV.ParametroRiscV(par.nombre(), tipoPig, porRef));
                nombre += "_" + tipoPig;
            }
        }
        for (NodoSentencia s : f.cuerpo()) {
            visitarSentencia(s);
        }

        if (f.tipoRetorno() == null) {
            gestor.emitir(new Retorno1(null));
        }

        String tipoRet = f.tipoRetorno() != null ? tipoYaPig(f.tipoRetorno()) : null;

        return new FuncionRiscV(
                nombre,
                null,
                params,
                false,
                tipoRet,
                new LinkedHashMap<>(tiposVariables),
                new ArrayList<>(gestor.getCuartetas()));
    }

    //SENTENCIAS

    private void visitarSentencia(NodoSentencia s) {
        switch (s.tipoNodo()) {
            case DECLARACION_VARIABLE -> visitarDeclaracionVariable((NodoSentencia.DeclaracionVariable) s);
            case ASIGNACION -> visitarAsignacion((NodoSentencia.Asignacion) s);
            case INCREMENTO_DECREMENTO -> visitarIncremento((NodoSentencia.IncrementoDecremento) s);
            case CONDICIONAL -> visitarCondicional((NodoSentencia.Condicional) s);
            case CICLO_PARA -> visitarCicloPara((NodoSentencia.CicloPara) s);
            case CICLO_MIENTRAS -> visitarCicloMientras((NodoSentencia.CicloMientras) s);
            case CICLO_HACER_MIENTRAS -> visitarCicloHacerMientras((NodoSentencia.CicloHacerMientras) s);
            case RETORNO -> visitarRetorno((NodoSentencia.Retorno) s);
            case IMPRIMIR -> visitarImprimir((NodoSentencia.Imprimir) s);
            case ROMPER -> {
                ContextoCiclo c = gestor.cicloActual();
                if (c != null) gestor.emitir(new Romper1(c.getEtiquetaRomper()));
            }
            case CONTINUAR -> {
                ContextoCiclo c = gestor.cicloActual();
                if (c != null) gestor.emitir(new Continuar1(c.getEtiquetaContinuar()));
            }
            case DECLARACION_ESTRUCTURA -> visitarDeclaracionEstructura((NodoSentencia.DeclaracionEstructura) s);
            case DECLARACION_ARREGLO -> visitarDeclaracionArreglo((NodoSentencia.DeclaracionArreglo) s);
            case LEER -> gestor.emitir(new LeerPig(null, "numerus"));
            case ELEGIR -> visitarElegir((NodoSentencia.Elegir) s);
            case DECLARACION_MATRIZ, DECLARACION_ESTRUCTURA_LOCAL ->
                    throw new UnsupportedOperationException(
                            "GeneradorCuartetasY: sentencia no soportada en esta tanda: " + s.tipoNodo());
        }
    }

    private void visitarDeclaracionVariable(NodoSentencia.DeclaracionVariable d) {
        String tipo = tipoYaPig(d.tipo());
        tiposVariables.put(d.nombre(), tipo);
        if (d.inicializacion() != null) {
            visitarExpresion(d.inicializacion());
            gestor.emitir(new AsignacionVariable(
                    new AccesoVariable(d.nombre(), tipo), ultimoResultado));
        }
    }

    private void visitarAsignacion(NodoSentencia.Asignacion a) {
        visitarExpresion(a.valor());
        AccesoMemoria valor = ultimoResultado;
        visitarExpresion(a.destino());
        AccesoMemoria destino = ultimoResultado;

        if (destino instanceof AccesoArreglo arr) {
            gestor.emitir(new AsignacionArreglo(arr.getBase(), arr.getIndice(), valor));
        } else if (destino instanceof AccesoAtributo1 attr) {
            gestor.emitir(new AsignacionAtributo(attr.getBase(), attr.getCampo(), true, valor));
        } else {
            gestor.emitir(new AsignacionVariable(destino, valor));
        }
    }

    private void visitarIncremento(NodoSentencia.IncrementoDecremento i) {
        visitarExpresion(i.destino());
        AccesoMemoria dest = ultimoResultado;
        String op = "++".equals(i.operador()) ? "+" : "-";
        AccesoTemporal t = nuevoTemporal("numerus");
        gestor.emitir(new OperacionBinaria(t, dest, op, new Literal(1, "numerus")));
        gestor.emitir(new AsignacionVariable(dest, t));
    }

    private void visitarCondicional(NodoSentencia.Condicional c) {
        int lSi = gestor.getContador().siguienteEtiqueta();
        int lNo = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        visitarExpresion(c.condicion());
        gestor.emitir(new Condicional1(ultimoResultado, "==", new Literal(0, "bool"), lNo));
        gestor.emitir(new Salto(lSi));

        gestor.emitir(new DefinicionEtiqueta(lSi));
        for (NodoSentencia s : c.cuerpoSi()) visitarSentencia(s);
        gestor.emitir(new Salto(lFin));

        gestor.emitir(new DefinicionEtiqueta(lNo));
        for (NodoSentencia.RamaSino rama : c.ramasSino()) {
            int lSig = gestor.getContador().siguienteEtiqueta();
            visitarExpresion(rama.condicion());
            gestor.emitir(new Condicional1(ultimoResultado, "==", new Literal(0, "bool"), lSig));
            for (NodoSentencia s : rama.cuerpo()) visitarSentencia(s);
            gestor.emitir(new Salto(lFin));
            gestor.emitir(new DefinicionEtiqueta(lSig));
        }
        if (c.cuerpoContrario() != null) {
            for (NodoSentencia s : c.cuerpoContrario()) visitarSentencia(s);
        }

        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    private void visitarElegir(NodoSentencia.Elegir e) {
        int lFin = gestor.getContador().siguienteEtiqueta();
        List<Integer> etiquetasCasos = new ArrayList<>();
        for (int i = 0; i < e.casos().size(); i++) {
            etiquetasCasos.add(gestor.getContador().siguienteEtiqueta());
        }
        int lSiempre = e.siempre() != null
                ? gestor.getContador().siguienteEtiqueta()
                : lFin;
        visitarExpresion(e.expresion());
        AccesoTemporal tExp = nuevoTemporal("numerus");
        gestor.emitir(new AsignacionVariable(tExp, ultimoResultado));
        for (int i = 0; i < e.casos().size(); i++) {
            NodoSentencia.CasoElegir caso = e.casos().get(i);
            visitarExpresion(caso.valor());
            AccesoTemporal tCaso = nuevoTemporal("numerus");
            gestor.emitir(new AsignacionVariable(tCaso, ultimoResultado));
            gestor.emitir(new Condicional1(tExp, "==", tCaso, etiquetasCasos.get(i)));
        }
        gestor.emitir(new Salto(lSiempre));
        gestor.entrarCiclo(new ContextoCiclo(lFin, lFin));
        for (int i = 0; i < e.casos().size(); i++) {
            gestor.emitir(new DefinicionEtiqueta(etiquetasCasos.get(i)));
            for (NodoSentencia s : e.casos().get(i).cuerpo()) {
                visitarSentencia(s);
            }
        }
        if (e.siempre() != null) {
            gestor.emitir(new DefinicionEtiqueta(lSiempre));
            for (NodoSentencia s : e.siempre().cuerpo()) {
                visitarSentencia(s);
            }
        }
        gestor.salirCiclo();
        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    private void visitarCicloPara(NodoSentencia.CicloPara c) {
        String tipo = tipoYaPig(c.tipoInicializacion());
        tiposVariables.put(c.nombreVariable(), tipo);

        visitarExpresion(c.valorInicial());
        gestor.emitir(new AsignacionVariable(new AccesoVariable(c.nombreVariable(), tipo), ultimoResultado));

        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lCont = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));

        visitarExpresion(c.condicion());
        gestor.emitir(new Condicional1(ultimoResultado, "==", new Literal(0, "bool"), lFin));

        gestor.entrarCiclo(new ContextoCiclo(lCont, lFin));
        for (NodoSentencia s : c.cuerpo()) visitarSentencia(s);
        gestor.salirCiclo();

        gestor.emitir(new DefinicionEtiqueta(lCont));
        AccesoMemoria var = new AccesoVariable(c.nombreVariable(), tipo);
        String op = "++".equals(c.operadorActualizacion()) ? "+" : "-";
        AccesoTemporal t = nuevoTemporal("numerus");
        gestor.emitir(new OperacionBinaria(t, var, op, new Literal(1, "numerus")));
        gestor.emitir(new AsignacionVariable(var, t));

        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    private void visitarCicloMientras(NodoSentencia.CicloMientras c) {
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));
        visitarExpresion(c.condicion());
        gestor.emitir(new Condicional1(ultimoResultado, "==", new Literal(0, "bool"), lFin));

        gestor.entrarCiclo(new ContextoCiclo(lInicio, lFin));
        for (NodoSentencia s : c.cuerpo()) visitarSentencia(s);
        gestor.salirCiclo();

        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    private void visitarCicloHacerMientras(NodoSentencia.CicloHacerMientras c) {
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lCond = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));
        gestor.entrarCiclo(new ContextoCiclo(lCond, lFin));
        for (NodoSentencia s : c.cuerpo()) visitarSentencia(s);
        gestor.salirCiclo();

        gestor.emitir(new DefinicionEtiqueta(lCond));
        visitarExpresion(c.condicion());
        gestor.emitir(new Condicional1(ultimoResultado, "!=", new Literal(0, "bool"), lInicio));
        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    private void visitarRetorno(NodoSentencia.Retorno r) {
        if (r.valor() != null) {
            visitarExpresion(r.valor());
            gestor.emitir(new Retorno1(ultimoResultado));
        } else {
            gestor.emitir(new Retorno1(null));
        }
    }

    private void visitarImprimir(NodoSentencia.Imprimir i) {
        visitarExpresion(i.expresion());
        String tipo = tipoPigDeExpresion(i.expresion());
        if ("bool".equals(tipo)) {
            AccesoTemporal t = nuevoTemporal("textum");
            gestor.emitir(new OperacionBinaria(t, new Literal("", "textum"), "+", ultimoResultado));
            ultimoResultado = t;
            tipo = "textum";
        }
        gestor.emitir(new ImprimirPig(ultimoResultado, tipo));
        gestor.emitir(new ImprimirPig(new Literal("\n", "textum"), "textum"));
    }

    // EXPRESIONES

    private void visitarExpresion(NodoExpr e) {
        switch (e.tipoNodo()) {
            case LITERAL_ENTERO -> ultimoResultado =
                    new Literal(((NodoExpr.LiteralEntero) e).valor(), "numerus");
            case LITERAL_FLOTANTE -> ultimoResultado =
                    new Literal(((NodoExpr.LiteralFlotante) e).valor(), "decimalis");
            case LITERAL_CADENA -> ultimoResultado =
                    new Literal(((NodoExpr.LiteralCadena) e).valor(), "textum");
            case LITERAL_CARACTER -> ultimoResultado =
                    new Literal(((NodoExpr.LiteralCaracter) e).valor(), "littera");
            case LITERAL_BOOL -> ultimoResultado =
                    new Literal(((NodoExpr.LiteralBool) e).valor(), "bool");
            case IDENTIFICADOR -> visitarIdentificador((NodoExpr.Identificador) e);
            case BINARIA -> visitarBinaria((NodoExpr.Binaria) e);
            case UNARIA -> visitarUnaria((NodoExpr.Unaria) e);
            case LLAMADA_FUNCION -> visitarLlamadaFuncion((NodoExpr.LlamadaFuncion) e);
            case LEER -> throw new UnsupportedOperationException(
                    "GeneradorCuartetasY: leer() como expresión no soportado en esta tanda");
            case ACCESO_ARRAY -> visitarAccesoArray((NodoExpr.AccesoArray) e);
            case ACCESO_ATRIBUTO -> visitarAccesoAtributo((NodoExpr.AccesoAtributo) e);
        }
    }

    private void visitarIdentificador(NodoExpr.Identificador i) {
        String nombre = i.nombre();
        if (tiposVariables.containsKey(nombre)) {
            ultimoResultado = new AccesoVariable(nombre, tiposVariables.get(nombre));
            return;
        }
        throw new IllegalStateException("GeneradorCuartetasY: identificador no declarado: " + nombre);
    }

    private void visitarBinaria(NodoExpr.Binaria b) {
        if ("&&".equals(b.operador()) || "||".equals(b.operador())) {
            visitarLogicaCortocircuito(b);
            return;
        }
        visitarExpresion(b.izquierda());
        AccesoMemoria izq = ultimoResultado;
        visitarExpresion(b.derecha());
        AccesoMemoria der = ultimoResultado;

        String tipoRes = tipoBinario(izq.getTipo(), b.operador(), der.getTipo());
        AccesoTemporal t = nuevoTemporal(tipoRes);
        gestor.emitir(new OperacionBinaria(t, izq, b.operador(), der));
        ultimoResultado = t;
    }

    private void visitarLogicaCortocircuito(NodoExpr.Binaria b) {
        boolean esAnd = "&&".equals(b.operador());
        int lFin = gestor.getContador().siguienteEtiqueta();
        AccesoTemporal res = nuevoTemporal("bool");
        gestor.emitir(new AsignacionVariable(res, new Literal(esAnd ? 0 : 1, "bool")));
        String saltarSi = esAnd ? "==" : "!=";

        visitarExpresion(b.izquierda());
        gestor.emitir(new Condicional1(ultimoResultado, saltarSi, new Literal(0, "bool"), lFin));
        visitarExpresion(b.derecha());
        gestor.emitir(new Condicional1(ultimoResultado, saltarSi, new Literal(0, "bool"), lFin));

        gestor.emitir(new AsignacionVariable(res, new Literal(esAnd ? 1 : 0, "bool")));
        gestor.emitir(new DefinicionEtiqueta(lFin));
        ultimoResultado = res;
    }

    private void visitarUnaria(NodoExpr.Unaria u) {
        visitarExpresion(u.operando());
        AccesoMemoria op = ultimoResultado;
        String tipoRes = "!".equals(u.operador()) ? "bool" : "numerus";
        AccesoTemporal t = nuevoTemporal(tipoRes);
        gestor.emitir(new OperacionUnaria(t, u.operador(), op));
        ultimoResultado = t;
    }

    private void visitarLlamadaFuncion(NodoExpr.LlamadaFuncion l) {
        List<AccesoMemoria> args = new ArrayList<>();
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : l.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigDeExpresion(arg));
        }

        TablaSimbolos.DefinicionFuncion def = tabla.buscarFuncion(l.nombre())
                .orElseThrow(() -> new IllegalStateException(
                        "GeneradorCuartetasY: función no encontrada: " + l.nombre()));

        String nombreRiscV = l.nombre();
        for (String t : def.tipoParametros()) nombreRiscV += "_" + tipoYaPig(t);

        String tipoRet = def.tipoRetorno() != null ? tipoYaPig(def.tipoRetorno()) : "numerus";
        AccesoTemporal t = nuevoTemporal(tipoRet);
        gestor.emitir(new Llamada(t, nombreRiscV, args));
        ultimoResultado = t;
    }

    private void visitarDeclaracionEstructura(NodoSentencia.DeclaracionEstructura d) {
        String tipo = d.tipoEstructura();
        tiposVariables.put(d.nombre(), tipo);

        var def = tabla.buscarEstructura(tipo)
                .orElseThrow(() -> new IllegalStateException(
                        "GeneradorCuartetasY: estructura no encontrada: " + tipo));
        List<String> campos = new ArrayList<>(def.atributos().keySet());
        AccesoVariable base = new AccesoVariable(d.nombre(), tipo);
        for (int i = 0; i < d.inicializacion().size() && i < campos.size(); i++) {
            visitarExpresion(d.inicializacion().get(i));
            gestor.emitir(new AsignacionAtributo(base, campos.get(i), false, ultimoResultado));
        }
        for (int i = d.inicializacion().size(); i < campos.size(); i++) {
            gestor.emitir(new AsignacionAtributo(base, campos.get(i), false, new Literal(0, "numerus")));
        }
    }

    private void visitarDeclaracionArreglo(NodoSentencia.DeclaracionArreglo d) {
        String tipo = tipoYaPig(d.tipo());
        tiposVariables.put(d.nombre(), tipo);

        AccesoVariable base = new AccesoVariable(d.nombre(), tipo);
        for (int i = 0; i < d.inicializacion().size(); i++) {
            visitarExpresion(d.inicializacion().get(i));
            gestor.emitir(new AsignacionArreglo(base, new Literal(i, "numerus"), ultimoResultado));
        }
        for (int i = d.inicializacion().size(); i < d.tamano(); i++) {
            gestor.emitir(new AsignacionArreglo(base, new Literal(i, "numerus"),
                    new Literal(0, "numerus")));
        }
    }

    private void visitarAccesoArray(NodoExpr.AccesoArray a) {
        visitarExpresion(a.arreglo());
        AccesoMemoria base = ultimoResultado;
        visitarExpresion(a.indice());
        AccesoMemoria indice = ultimoResultado;
        String tipoElem = tipoPigDeExpresion(a.arreglo());
        ultimoResultado = new AccesoArreglo(base, indice, tipoElem);
    }

    private void visitarAccesoAtributo(NodoExpr.AccesoAtributo a) {
        visitarExpresion(a.objeto());
        AccesoMemoria obj = ultimoResultado;
        String tipoObj = tipoPigDeExpresion(a.objeto());

        var def = tabla.buscarEstructura(tipoObj)
                .orElseThrow(() -> new IllegalStateException(
                        "GeneradorCuartetasY: acceso a atributo sobre tipo no struct: " + tipoObj));
        String tipoCampo = def.atributos().get(a.atributo());
        if (tipoCampo == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasY: campo no encontrado: " + a.atributo() + " en " + tipoObj);
        }
        ultimoResultado = new AccesoAtributo1(obj, a.atributo(), false, tipoYaPig(tipoCampo));
    }

    // HELPERS

    private AccesoTemporal nuevoTemporal(String tipo) {
        return new AccesoTemporal(gestor.getContador().siguienteTemporal(tipo), tipo);
    }

    private static String tipoBinario(String izq, String op, String der) {
        return switch (op) {
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> "bool";
            case "+" -> ("textum".equals(izq) || "textum".equals(der)) ? "textum" : "numerus";
            default -> "numerus";
        };
    }

    private String tipoPigDeExpresion(NodoExpr e) {
        return switch (e.tipoNodo()) {
            case LITERAL_ENTERO -> "numerus";
            case LITERAL_FLOTANTE -> "decimalis";
            case LITERAL_CADENA -> "textum";
            case LITERAL_CARACTER -> "littera";
            case LITERAL_BOOL -> "bool";
            case IDENTIFICADOR -> tiposVariables.getOrDefault(
                    ((NodoExpr.Identificador) e).nombre(), "numerus");
            case BINARIA -> {
                NodoExpr.Binaria b = (NodoExpr.Binaria) e;
                yield tipoBinario(tipoPigDeExpresion(b.izquierda()),
                        b.operador(), tipoPigDeExpresion(b.derecha()));
            }
            case UNARIA -> "!".equals(((NodoExpr.Unaria) e).operador()) ? "bool" : "numerus";
            case LLAMADA_FUNCION -> {
                NodoExpr.LlamadaFuncion lf = (NodoExpr.LlamadaFuncion) e;
                yield tabla.buscarFuncion(lf.nombre())
                        .map(d -> d.tipoRetorno() != null ? tipoYaPig(d.tipoRetorno()) : "numerus")
                        .orElse("numerus");
            }
            case ACCESO_ATRIBUTO -> {
                NodoExpr.AccesoAtributo at = (NodoExpr.AccesoAtributo) e;
                String tipoObj = tipoPigDeExpresion(at.objeto());
                var def = tabla.buscarEstructura(tipoObj);
                yield def.map(d -> tipoYaPig(d.atributos().get(at.atributo())))
                        .orElse("numerus");
            }
            case ACCESO_ARRAY -> tipoPigDeExpresion(((NodoExpr.AccesoArray) e).arreglo());
            default -> "numerus";
        };
    }

    static String tipoYaPig(String tipoY) {
        if (tipoY == null) return null;
        return switch (tipoY) {
            case "entero"   -> "numerus";
            case "flotante" -> "decimalis";
            case "caracter" -> "littera";
            case "cadena"   -> "textum";
            case "bool"     -> "bool";
            default         -> tipoY;
        };
    }
}