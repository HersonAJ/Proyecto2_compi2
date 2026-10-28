package com.example.stack_over_pig.riscv.zetariano;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.LlamadaMetodo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.NewArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.NewObjeto;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.ImprimirPig;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.LeerPig;
import com.example.stack_over_pig.riscv.FuncionRiscV;
import com.example.stack_over_pig.zetariano.nodo.*;
import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ;

import java.util.*;

/**
 * Recorre el AST de Zetariano (constructor o método) y emite cuartetas.
 * Produce un FuncionRiscV con el nombre RISC-V, parámetros, tipos de variables y cuartetas.
 *
 * Todos los tipos que viajan en las cuartetas están en vocabulario Pig
 * (numerus, textum, littera, bool, decimalis o nombre de clase). Los tipos que vienen
 * de la tabla de símbolos de Z (int, String, char, boolean, double) se traducen con tipoZaPig.
 *
 * Una instancia de este generador sirve para UNA sola función (guarda estado por función).
 */
public class GeneradorCuartetasZ {

    private final TablaSimbolosZ tabla;
    private final String tipoClase;
    private final GestorCodigoIntermedio gestor;
    private final Map<String, String> tiposVariables = new LinkedHashMap<>();
    private AccesoMemoria ultimoResultado;

    public GeneradorCuartetasZ(TablaSimbolosZ tabla, String tipoClase) {
        this.tabla = tabla;
        this.tipoClase = tipoClase;
        this.gestor = new GestorCodigoIntermedio();
    }

    // ========== PUNTOS DE ENTRADA ==========

    public FuncionRiscV generarConstructor(NodoConstructor c) {
        // Registrar parámetros como variables locales.
        for (NodoParametroZ p : c.parametros()) {
            tiposVariables.put(p.nombre(), tipoZaPig(p.tipo()));
        }

        // super() implícito: si hay superclase y el constructor no lo escribe, se llama al constructor vacío del padre.
        if (tabla.getSuperclase() != null && !empiezaConSuper(c.cuerpo())) {
            emitirLlamadaConstructorPadre(new ArrayList<>(), new ArrayList<>());
        }

        // Emitir cuerpo.
        for (NodoSentencia s : c.cuerpo()) {
            visitarSentencia(s);
        }

        // Retorno implícito de this.
        gestor.emitir(new Retorno1(new AccesoVariable("this", tipoClase)));

        // Nombre RISC-V.
        String nombre = tipoClase + "_constructor";
        for (NodoParametroZ p : c.parametros()) {
            nombre += "_" + tipoZaPig(p.tipo());
        }

        List<FuncionRiscV.ParametroRiscV> params = new ArrayList<>();
        for (NodoParametroZ p : c.parametros()) {
            params.add(new FuncionRiscV.ParametroRiscV(p.nombre(), tipoZaPig(p.tipo())));
        }

        return new FuncionRiscV(
                nombre, tipoClase, params, true, tipoClase,
                new LinkedHashMap<>(tiposVariables),
                new ArrayList<>(gestor.getCuartetas()));
    }

    /**
     * Constructor por defecto implícito (clase sin constructores declarados): solo devuelve this.
     * Pendiente (Fase 3c): si la clase tiene superclase, debe llamar al constructor vacío del padre.
     */
    public FuncionRiscV generarConstructorPorDefecto() {
        if (tabla.getSuperclase() != null) {
            emitirLlamadaConstructorPadre(new ArrayList<>(), new ArrayList<>());
        }

        gestor.emitir(new Retorno1(new AccesoVariable("this", tipoClase)));
        return new FuncionRiscV(
                tipoClase + "_constructor", tipoClase, new ArrayList<>(), true, tipoClase,
                new LinkedHashMap<>(tiposVariables),
                new ArrayList<>(gestor.getCuartetas()));
    }

    public FuncionRiscV generarMetodo(NodoMetodo m) {
        for (NodoParametroZ p : m.parametros()) {
            tiposVariables.put(p.nombre(), tipoZaPig(p.tipo()));
        }

        for (NodoSentencia s : m.cuerpo()) {
            visitarSentencia(s);
        }

        // Retorno implícito si es void y no hay return explícito.
        if (m.tipoRetorno() == null) {
            gestor.emitir(new Retorno1(null));
        }

        String nombre = tipoClase + "_" + m.nombre();
        for (NodoParametroZ p : m.parametros()) {
            nombre += "_" + tipoZaPig(p.tipo());
        }

        List<FuncionRiscV.ParametroRiscV> params = new ArrayList<>();
        for (NodoParametroZ p : m.parametros()) {
            params.add(new FuncionRiscV.ParametroRiscV(p.nombre(), tipoZaPig(p.tipo())));
        }

        return new FuncionRiscV(
                nombre, tipoClase, params, true, tipoZaPig(m.tipoRetorno()),
                new LinkedHashMap<>(tiposVariables),
                new ArrayList<>(gestor.getCuartetas()));
    }

    // ========== SENTENCIAS ==========

    private void visitarSentencia(NodoSentencia s) {
        switch (s.tipoNodo()) {
            case DECLARACION_VARIABLE -> visitarDeclaracionVariable((NodoSentencia.DeclaracionVariable) s);
            case ASIGNACION -> visitarAsignacion((NodoSentencia.Asignacion) s);
            case EXPRESION_COMO_SENTENCIA -> visitarExpresionComoSentencia((NodoSentencia.ExpresionComoSentencia) s);
            case CONDICIONAL -> visitarCondicional((NodoSentencia.Condicional) s);
            case CICLO_PARA -> visitarCicloPara((NodoSentencia.CicloPara) s);
            case CICLO_MIENTRAS -> visitarCicloMientras((NodoSentencia.CicloMientras) s);
            case CICLO_HACER_MIENTRAS -> visitarCicloHacerMientras((NodoSentencia.CicloHacerMientras) s);
            case RETORNO -> visitarRetorno((NodoSentencia.Retorno) s);
            case IMPRIMIR -> visitarImprimir((NodoSentencia.Imprimir) s);
            case LEER -> gestor.emitir(new LeerPig(null, "numerus"));
            case ROMPER -> {
                ContextoCiclo ciclo = gestor.cicloActual();
                if (ciclo != null) gestor.emitir(new Romper1(ciclo.getEtiquetaRomper()));
            }
            case CONTINUAR -> {
                ContextoCiclo ciclo = gestor.cicloActual();
                if (ciclo != null) gestor.emitir(new Continuar1(ciclo.getEtiquetaContinuar()));
            }
            case SWITCH -> throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: switch se implementará en Fase 3b.2");
            case LLAMADA_SUPER_CONSTRUCTOR -> visitarSuperConstructor((NodoSentencia.LlamadaSuperConstructor) s);
            case CASO_SWITCH, CASO_DEFAULT -> { /* gestionados por Switch */ }
        }
    }

    private void visitarDeclaracionVariable(NodoSentencia.DeclaracionVariable d) {
        String tipo = tipoZaPig(d.tipo());
        tiposVariables.put(d.nombre(), tipo);

        if (d.inicializacion() != null) {
            visitarExpresion(d.inicializacion());
            AccesoMemoria valor = ultimoResultado;
            AccesoVariable destino = new AccesoVariable(d.nombre(), tipo);
            gestor.emitir(new AsignacionVariable(destino, valor));
        }
    }

    /** '=', '+=', '-=', '*='. La compuesta lee el destino, opera y vuelve a guardar. */
    private void visitarAsignacion(NodoSentencia.Asignacion a) {
        String opBinario = operadorBinarioDeCompuesta(a.operador());

        if (opBinario == null) {
            visitarExpresion(a.valor());
            AccesoMemoria valor = ultimoResultado;
            visitarExpresion(a.destino());
            AccesoMemoria destino = ultimoResultado;
            almacenarEn(destino, valor);
            return;
        }

        // Compuesta: destino (se evalúa una sola vez), luego valor.
        visitarExpresion(a.destino());
        AccesoMemoria destino = ultimoResultado;
        visitarExpresion(a.valor());
        AccesoMemoria valor = ultimoResultado;

        String tipoRes = tipoBinario(destino.getTipo(), opBinario, valor.getTipo());
        AccesoTemporal t = nuevoTemporal(tipoRes);
        gestor.emitir(new OperacionBinaria(t, destino, opBinario, valor));
        almacenarEn(destino, t);
    }

    private static String operadorBinarioDeCompuesta(String operador) {
        if (operador == null) return null;
        return switch (operador) {
            case "+=" -> "+";
            case "-=" -> "-";
            case "*=" -> "*";
            default -> null;   // '=' (o cualquier otro) es asignación simple
        };
    }

    /** Guarda 'valor' en 'destino' según el tipo de destino (arreglo, atributo o variable/temporal). */
    private void almacenarEn(AccesoMemoria destino, AccesoMemoria valor) {
        if (destino instanceof AccesoArreglo arr) {
            gestor.emitir(new AsignacionArreglo(arr.getBase(), arr.getIndice(), valor));
        } else if (destino instanceof AccesoAtributo1 attr) {
            gestor.emitir(new AsignacionAtributo(attr.getBase(), attr.getCampo(), true, valor));
        } else {
            gestor.emitir(new AsignacionVariable(destino, valor));
        }
    }

    private void visitarExpresionComoSentencia(NodoSentencia.ExpresionComoSentencia e) {
        visitarExpresion(e.expresion());
    }

    private void visitarCondicional(NodoSentencia.Condicional c) {
        int lSi = gestor.getContador().siguienteEtiqueta();
        int lNo = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        visitarExpresion(c.condicion());
        AccesoMemoria cond = ultimoResultado;

        gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lNo));
        gestor.emitir(new Salto(lSi));

        gestor.emitir(new DefinicionEtiqueta(lSi));
        for (NodoSentencia s : c.cuerpoSi()) visitarSentencia(s);
        gestor.emitir(new Salto(lFin));

        gestor.emitir(new DefinicionEtiqueta(lNo));
        if (c.cuerpoSino() != null) {
            for (NodoSentencia s : c.cuerpoSino()) visitarSentencia(s);
        }

        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    private void visitarCicloPara(NodoSentencia.CicloPara c) {
        if (c.inicializacion() != null) visitarSentencia(c.inicializacion());

        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lContinuar = gestor.getContador().siguienteEtiqueta();
        int lRomper = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));

        if (c.condicion() != null) {
            visitarExpresion(c.condicion());
            AccesoMemoria cond = ultimoResultado;
            gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lRomper));
        }

        gestor.entrarCiclo(new ContextoCiclo(lContinuar, lRomper));
        for (NodoSentencia s : c.cuerpo()) visitarSentencia(s);
        gestor.salirCiclo();

        gestor.emitir(new DefinicionEtiqueta(lContinuar));
        if (c.actualizacion() != null) visitarSentencia(c.actualizacion());

        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lRomper));
    }

    private void visitarCicloMientras(NodoSentencia.CicloMientras c) {
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lRomper = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));

        visitarExpresion(c.condicion());
        AccesoMemoria cond = ultimoResultado;
        gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lRomper));

        gestor.entrarCiclo(new ContextoCiclo(lInicio, lRomper));
        for (NodoSentencia s : c.cuerpo()) visitarSentencia(s);
        gestor.salirCiclo();

        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lRomper));
    }

    private void visitarCicloHacerMientras(NodoSentencia.CicloHacerMientras c) {
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lCondicion = gestor.getContador().siguienteEtiqueta();
        int lRomper = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));
        gestor.entrarCiclo(new ContextoCiclo(lCondicion, lRomper));

        for (NodoSentencia s : c.cuerpo()) visitarSentencia(s);

        gestor.salirCiclo();
        gestor.emitir(new DefinicionEtiqueta(lCondicion));

        visitarExpresion(c.condicion());
        AccesoMemoria cond = ultimoResultado;
        gestor.emitir(new Condicional1(cond, "!=", new Literal(0, "bool"), lInicio));

        gestor.emitir(new DefinicionEtiqueta(lRomper));
    }

    private void visitarRetorno(NodoSentencia.Retorno r) {
        if (r.valor() != null) {
            visitarExpresion(r.valor());
            gestor.emitir(new Retorno1(ultimoResultado));
        } else {
            gestor.emitir(new Retorno1(null));
        }
    }

    /**
     * println / print. El salto de línea se emite como un segundo ImprimirPig con "\n".
     * Un bool se imprime como true/false: se concatena con "" para reutilizar la conversión a texto.
     */
    private void visitarImprimir(NodoSentencia.Imprimir i) {
        visitarExpresion(i.expresion());
        AccesoMemoria acc = ultimoResultado;
        String tipo = tipoPigDeExpresion(i.expresion());

        if ("bool".equals(tipo)) {
            AccesoTemporal t = nuevoTemporal("textum");
            gestor.emitir(new OperacionBinaria(t, new Literal("", "textum"), "+", acc));
            acc = t;
            tipo = "textum";
        }
        gestor.emitir(new ImprimirPig(acc, tipo));

        if (i.saltoDeLinea()) {
            gestor.emitir(new ImprimirPig(new Literal("\n", "textum"), "textum"));
        }
    }

    // ========== EXPRESIONES ==========

    private void visitarExpresion(NodoExpr e) {
        switch (e.tipoNodo()) {
            case LITERAL_ENTERO -> ultimoResultado = new Literal(((NodoExpr.LiteralEntero) e).valor(), "numerus");
            case LITERAL_DECIMAL -> ultimoResultado =
                    new Literal(((NodoExpr.LiteralDecimal) e).valor(), "decimalis");
            case LITERAL_CADENA -> ultimoResultado = new Literal(((NodoExpr.LiteralCadena) e).valor(), "textum");
            case LITERAL_CARACTER -> ultimoResultado = new Literal(((NodoExpr.LiteralCaracter) e).valor(), "littera");
            case LITERAL_BOOL -> ultimoResultado = new Literal(((NodoExpr.LiteralBool) e).valor(), "bool");
            case LITERAL_NULO -> ultimoResultado = new Literal(null, "null");
            case LISTA_LITERAL -> throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: listas literales no soportadas aquí");
            case IDENTIFICADOR -> visitarIdentificador((NodoExpr.Identificador) e);
            case ACCESO_ARRAY -> visitarAccesoArray((NodoExpr.AccesoArray) e);
            case ACCESO_ATRIBUTO -> visitarAccesoAtributo((NodoExpr.AccesoAtributo) e);
            case BINARIA -> visitarBinaria((NodoExpr.Binaria) e);
            case UNARIA -> visitarUnaria((NodoExpr.Unaria) e);
            case INCREMENTO_DECREMENTO -> visitarIncrementoDecremento((NodoExpr.IncrementoDecremento) e);
            case TERNARIA -> visitarTernaria((NodoExpr.Ternaria) e);
            case LLAMADA_FUNCION -> visitarLlamadaFuncion((NodoExpr.LlamadaFuncion) e);
            case LLAMADA_METODO -> visitarLlamadaMetodo((NodoExpr.LlamadaMetodo) e);
            case INSTANCIA_OBJETO -> visitarInstanciaObjeto((NodoExpr.InstanciaObjeto) e);
            case ARREGLO_NUEVO -> visitarArregloNuevo((NodoExpr.ArregloNuevo) e);
            case OBJETO_ACTUAL -> ultimoResultado = new AccesoVariable("this", tipoClase);
            case LLAMADA_METODO_SUPER -> visitarLlamadaMetodoSuper((NodoExpr.LlamadaMetodoSuper) e);
        }
    }

    private void visitarIdentificador(NodoExpr.Identificador i) {
        String nombre = i.nombre();
        // 1. Local o parámetro.
        if (tiposVariables.containsKey(nombre)) {
            ultimoResultado = new AccesoVariable(nombre, tiposVariables.get(nombre));
            return;
        }
        // 2. Atributo (this.nombre implícito).
        var attrOpt = tabla.buscarAtributoEn(tipoClase, nombre);
        if (attrOpt.isPresent()) {
            var attr = attrOpt.get();
            AccesoVariable thisAcc = new AccesoVariable("this", tipoClase);
            ultimoResultado = new AccesoAtributo1(thisAcc, nombre, true, tipoZaPig(attr.tipo()));
            return;
        }
        throw new IllegalStateException("GeneradorCuartetasZ: identificador no declarado: " + nombre);
    }

    private void visitarAccesoArray(NodoExpr.AccesoArray a) {
        visitarExpresion(a.arreglo());
        AccesoMemoria base = ultimoResultado;
        visitarExpresion(a.indice());
        AccesoMemoria indice = ultimoResultado;
        ultimoResultado = new AccesoArreglo(base, indice, "numerus");
    }

    private void visitarAccesoAtributo(NodoExpr.AccesoAtributo a) {
        visitarExpresion(a.objeto());
        AccesoMemoria obj = ultimoResultado;
        String tipoObj = tipoPigDeExpresion(a.objeto());

        var attrOpt = tabla.buscarAtributoEn(tipoObj, a.atributo());
        if (attrOpt.isEmpty()) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: atributo no encontrado: " + a.atributo() + " en " + tipoObj);
        }
        ultimoResultado = new AccesoAtributo1(obj, a.atributo(), true, tipoZaPig(attrOpt.get().tipo()));
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

    /**
     * a && b : si a es falso, b NO se evalúa.   a || b : si a es verdadero, b NO se evalúa.
     * El resultado vive en un temporal bool que se fija antes de saltar a lFin.
     */
    private void visitarLogicaCortocircuito(NodoExpr.Binaria b) {
        boolean esAnd = "&&".equals(b.operador());
        int lFin = gestor.getContador().siguienteEtiqueta();
        AccesoTemporal res = nuevoTemporal("bool");

        // Valor por defecto si se corta el cálculo: && -> 0, || -> 1.
        gestor.emitir(new AsignacionVariable(res, new Literal(esAnd ? 0 : 1, "bool")));

        String saltarSi = esAnd ? "==" : "!=";

        visitarExpresion(b.izquierda());
        gestor.emitir(new Condicional1(ultimoResultado, saltarSi, new Literal(0, "bool"), lFin));

        visitarExpresion(b.derecha());
        gestor.emitir(new Condicional1(ultimoResultado, saltarSi, new Literal(0, "bool"), lFin));

        // Llegó hasta aquí sin cortar: && -> 1, || -> 0.
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

    /** ++x / --x devuelven el valor nuevo; x++ / x-- devuelven el valor anterior. */
    private void visitarIncrementoDecremento(NodoExpr.IncrementoDecremento inc) {
        visitarExpresion(inc.operando());
        AccesoMemoria op = ultimoResultado;

        AccesoTemporal viejo = null;
        if (!inc.prefijo()) {
            viejo = nuevoTemporal("numerus");
            gestor.emitir(new AsignacionVariable(viejo, op));
        }

        String opBinario = "++".equals(inc.operador()) ? "+" : "-";
        AccesoTemporal nuevo = nuevoTemporal("numerus");
        gestor.emitir(new OperacionBinaria(nuevo, op, opBinario, new Literal(1, "numerus")));
        almacenarEn(op, nuevo);

        ultimoResultado = inc.prefijo() ? op : viejo;
    }

    /** cond ? a : b : las dos ramas escriben en el mismo temporal. */
    private void visitarTernaria(NodoExpr.Ternaria t) {
        int lNo = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        String tipoRes = tipoPigDeExpresion(t.siVerdadero());
        if ("null".equals(tipoRes)) tipoRes = tipoPigDeExpresion(t.siFalso());
        AccesoTemporal res = nuevoTemporal(tipoRes);

        visitarExpresion(t.condicion());
        gestor.emitir(new Condicional1(ultimoResultado, "==", new Literal(0, "bool"), lNo));

        visitarExpresion(t.siVerdadero());
        gestor.emitir(new AsignacionVariable(res, ultimoResultado));
        gestor.emitir(new Salto(lFin));

        gestor.emitir(new DefinicionEtiqueta(lNo));
        visitarExpresion(t.siFalso());
        gestor.emitir(new AsignacionVariable(res, ultimoResultado));

        gestor.emitir(new DefinicionEtiqueta(lFin));
        ultimoResultado = res;
    }

    /**
     * 'metodo(args)' sin receptor: en Zetariano solo puede ser un método de la propia clase
     * (o heredado), o sea this.metodo(args).
     */
    private void visitarLlamadaFuncion(NodoExpr.LlamadaFuncion l) {
        List<AccesoMemoria> args = new ArrayList<>();
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : l.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigDeExpresion(arg));
        }

        TablaSimbolosZ.Firma firma =
                resolverSobrecarga(tabla.getMetodosDe(tipoClase, l.nombre()), tiposArgs, l.nombre());
        if (firma == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: método no encontrado: " + l.nombre() + " en " + tipoClase);
        }

        String nombreRiscV = nombreMetodo(claseDeclarante(firma, tipoClase), l.nombre(), firma);
        AccesoVariable receptor = new AccesoVariable("this", tipoClase);

        String tipoRet = firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
        AccesoTemporal t = nuevoTemporal(tipoRet);
        gestor.emitir(new LlamadaMetodo1(t, receptor, nombreRiscV, args));
        ultimoResultado = t;
    }

    private void visitarLlamadaMetodo(NodoExpr.LlamadaMetodo l) {
        visitarExpresion(l.objeto());
        AccesoMemoria obj = ultimoResultado;
        String tipoObj = tipoPigDeExpresion(l.objeto());

        List<AccesoMemoria> args = new ArrayList<>();
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : l.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigDeExpresion(arg));
        }

        TablaSimbolosZ.Firma firma =
                resolverSobrecarga(tabla.getMetodosDe(tipoObj, l.nombre()), tiposArgs, l.nombre());
        if (firma == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: método no encontrado: " + l.nombre() + " en " + tipoObj);
        }

        // El nombre sale de la FIRMA declarada (tipos de los parámetros), no de los argumentos.
        String nombreRiscV = nombreMetodo(claseDeclarante(firma, tipoObj), l.nombre(), firma);

        String tipoRet = firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
        AccesoTemporal t = nuevoTemporal(tipoRet);
        gestor.emitir(new LlamadaMetodo1(t, obj, nombreRiscV, args));
        ultimoResultado = t;
    }

    private void visitarInstanciaObjeto(NodoExpr.InstanciaObjeto inst) {
        List<AccesoMemoria> args = new ArrayList<>();
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : inst.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigDeExpresion(arg));
        }

        TablaSimbolosZ.Firma firma =
                resolverSobrecarga(tabla.getConstructoresDe(inst.tipoClase()), tiposArgs, inst.tipoClase());
        if (firma == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: constructor no encontrado en " + inst.tipoClase()
                            + " para argumentos " + tiposArgs);
        }

        String nombreConstructor = inst.tipoClase() + "_constructor";
        for (String t : firma.tiposParametros()) nombreConstructor += "_" + tipoZaPig(t);

        int idT = gestor.getContador().siguienteTemporal(inst.tipoClase());
        AccesoTemporal t = new AccesoTemporal(idT, inst.tipoClase());
        gestor.emitir(new NewObjeto(t, inst.tipoClase(), nombreConstructor, args));
        ultimoResultado = t;
    }

    private void visitarArregloNuevo(NodoExpr.ArregloNuevo a) {
        List<AccesoMemoria> dims = new ArrayList<>();
        for (NodoExpr d : a.dimensiones()) {
            visitarExpresion(d);
            dims.add(ultimoResultado);
        }

        AccesoTemporal t = nuevoTemporal("numerus");

        if (dims.size() == 1) {
            gestor.emitir(new NewArreglo(t, a.tipoBase(), dims.get(0)));
        } else {
            throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: arreglos multidimensionales pendientes (Fase 3b.2)");
        }
        ultimoResultado = t;
    }

    // ========== HELPERS ==========

    private AccesoTemporal nuevoTemporal(String tipo) {
        int idT = gestor.getContador().siguienteTemporal(tipo);
        return new AccesoTemporal(idT, tipo);
    }

    /** Tipo (vocabulario Pig) del resultado de una operación binaria. */
    private static String tipoBinario(String tipoIzq, String operador, String tipoDer) {
        return switch (operador) {
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> "bool";
            case "+" -> {
                if ("textum".equals(tipoIzq) || "textum".equals(tipoDer)) yield "textum";
                if ("decimalis".equals(tipoIzq) || "decimalis".equals(tipoDer)) yield "decimalis";
                yield "numerus";
            }
            case "-", "*", "/", "%" -> {
                if ("decimalis".equals(tipoIzq) || "decimalis".equals(tipoDer)) yield "decimalis";
                yield "numerus";
            }
            default -> "numerus";
        };
    }

    private String tipoPigDeExpresion(NodoExpr e) {
        return switch (e.tipoNodo()) {
            case LITERAL_ENTERO -> "numerus";
            case LITERAL_DECIMAL -> "decimalis";
            case LITERAL_CADENA -> "textum";
            case LITERAL_CARACTER -> "littera";
            case LITERAL_BOOL -> "bool";
            case LITERAL_NULO -> "null";
            case OBJETO_ACTUAL -> tipoClase;
            case IDENTIFICADOR -> {
                NodoExpr.Identificador id = (NodoExpr.Identificador) e;
                if (tiposVariables.containsKey(id.nombre())) yield tiposVariables.get(id.nombre());
                var attr = tabla.buscarAtributoEn(tipoClase, id.nombre());
                yield attr.map(a -> tipoZaPig(a.tipo())).orElse("numerus");
            }
            case ACCESO_ATRIBUTO -> {
                NodoExpr.AccesoAtributo at = (NodoExpr.AccesoAtributo) e;
                String tipoObj = tipoPigDeExpresion(at.objeto());
                var attr = tabla.buscarAtributoEn(tipoObj, at.atributo());
                yield attr.map(a -> tipoZaPig(a.tipo())).orElse("numerus");
            }
            case INSTANCIA_OBJETO -> ((NodoExpr.InstanciaObjeto) e).tipoClase();
            case LLAMADA_METODO -> {
                NodoExpr.LlamadaMetodo lm = (NodoExpr.LlamadaMetodo) e;
                String tipoObj = tipoPigDeExpresion(lm.objeto());
                List<String> tiposArgs = new ArrayList<>();
                for (NodoExpr arg : lm.argumentos()) tiposArgs.add(tipoPigDeExpresion(arg));
                var firma = resolverSobrecarga(tabla.getMetodosDe(tipoObj, lm.nombre()), tiposArgs, lm.nombre());
                yield firma != null && firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
            }
            case LLAMADA_FUNCION -> {
                NodoExpr.LlamadaFuncion lf = (NodoExpr.LlamadaFuncion) e;
                List<String> tiposArgs = new ArrayList<>();
                for (NodoExpr arg : lf.argumentos()) tiposArgs.add(tipoPigDeExpresion(arg));
                var firma = resolverSobrecarga(tabla.getMetodosDe(tipoClase, lf.nombre()), tiposArgs, lf.nombre());
                yield firma != null && firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
            }
            case BINARIA -> {
                NodoExpr.Binaria b = (NodoExpr.Binaria) e;
                yield tipoBinario(tipoPigDeExpresion(b.izquierda()), b.operador(), tipoPigDeExpresion(b.derecha()));
            }
            case UNARIA -> "!".equals(((NodoExpr.Unaria) e).operador()) ? "bool" : "numerus";
            case TERNARIA -> {
                NodoExpr.Ternaria t = (NodoExpr.Ternaria) e;
                String tv = tipoPigDeExpresion(t.siVerdadero());
                yield "null".equals(tv) ? tipoPigDeExpresion(t.siFalso()) : tv;
            }
            case LLAMADA_METODO_SUPER -> {
                NodoExpr.LlamadaMetodoSuper ls = (NodoExpr.LlamadaMetodoSuper) e;
                List<String> tiposArgs = new ArrayList<>();
                for (NodoExpr arg : ls.argumentos()) tiposArgs.add(tipoPigDeExpresion(arg));
                var firma = resolverSobrecarga(tabla.getMetodosEnPadre(tipoClase, ls.nombre()), tiposArgs, ls.nombre());
                yield firma != null && firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
            }
            case ACCESO_ARRAY -> {
                NodoExpr.AccesoArray aa = (NodoExpr.AccesoArray) e;
                yield tipoElementoDeArreglo(aa.arreglo());
            }
            default -> "numerus";
        };
    }

    /** Tipo de los elementos de un arreglo cuya expresión base es 'arreglo'. */
    private String tipoElementoDeArreglo(NodoExpr arreglo) {
        // 1. Identificador: variable local o parámetro.
        if (arreglo instanceof NodoExpr.Identificador id) {
            String t = tiposVariables.get(id.nombre());
            if (t != null) return t;
            var attr = tabla.buscarAtributoEn(tipoClase, id.nombre());
            if (attr.isPresent()) return tipoZaPig(attr.get().tipo());
            return "numerus";
        }
        // 2. Acceso a atributo: this.arr o expr.arr → tipo del atributo.
        if (arreglo instanceof NodoExpr.AccesoAtributo at) {
            String tipoObj = tipoPigDeExpresion(at.objeto());
            var attr = tabla.buscarAtributoEn(tipoObj, at.atributo());
            if (attr.isPresent()) return tipoZaPig(attr.get().tipo());
            return "numerus";
        }
        return "numerus";
    }

    /**
     * Elige la firma cuyos parámetros (traducidos a Pig) coinciden con los argumentos.
     * Primero busca coincidencia exacta; si no hay, acepta compatibilidad (subclase, null).
     */
    private TablaSimbolosZ.Firma resolverSobrecarga(List<TablaSimbolosZ.Firma> firmas,
                                                    List<String> tiposArgs, String nombre) {
        for (TablaSimbolosZ.Firma f : firmas) {
            if (tiposParametrosPig(f).equals(tiposArgs)) return f;
        }
        for (TablaSimbolosZ.Firma f : firmas) {
            List<String> params = tiposParametrosPig(f);
            if (params.size() != tiposArgs.size()) continue;
            boolean compatible = true;
            for (int i = 0; i < params.size(); i++) {
                if (!tipoCompatible(tiposArgs.get(i), params.get(i))) { compatible = false; break; }
            }
            if (compatible) return f;
        }
        return null;
    }

    private static List<String> tiposParametrosPig(TablaSimbolosZ.Firma f) {
        return f.tiposParametros().stream().map(GeneradorCuartetasZ::tipoZaPig).toList();
    }

    /** ¿Se puede pasar un argumento de tipo 'arg' a un parámetro de tipo 'param'? */
    private boolean tipoCompatible(String arg, String param) {
        if (arg == null || param == null) return false;
        if (arg.equals(param)) return true;
        if ("null".equals(arg)) return !esPrimitivoPig(param);
        if (esPrimitivoPig(arg) || esPrimitivoPig(param)) return false;
        if ("textum".equals(arg) || "textum".equals(param)) return false;
        return tabla.esSubtipo(arg, param);
    }

    private static boolean esPrimitivoPig(String t) {
        return "numerus".equals(t) || "decimalis".equals(t) || "littera".equals(t) || "bool".equals(t);
    }

    /** Nombre RISC-V de un método: Clase_metodo_<tipos de los parámetros declarados>. */
    private static String nombreMetodo(String clase, String metodo, TablaSimbolosZ.Firma f) {
        StringBuilder sb = new StringBuilder(clase).append('_').append(metodo);
        for (String t : f.tiposParametros()) sb.append('_').append(tipoZaPig(t));
        return sb.toString();
    }

    /** Clase donde está escrito el método (con herencia puede ser un ancestro de la clase del receptor). */
    private static String claseDeclarante(TablaSimbolosZ.Firma f, String porDefecto) {
        return f.claseDeclarante() != null ? f.claseDeclarante() : porDefecto;
    }

    /** Traduce un tipo de Zetariano al vocabulario Pig. Si ya está en Pig o es una clase, lo deja igual. */
    private static String tipoZaPig(String tipoZ) {
        if (tipoZ == null) return null;
        return switch (tipoZ) {
            case "int"     -> "numerus";
            case "double"  -> "decimalis";
            case "char"    -> "littera";
            case "String"  -> "textum";
            case "boolean" -> "bool";
            default        -> tipoZ;  // clase
        };
    }

    private static boolean empiezaConSuper(List<NodoSentencia> cuerpo) {
        return !cuerpo.isEmpty() && cuerpo.get(0) instanceof NodoSentencia.LlamadaSuperConstructor;
    }

    private void visitarSuperConstructor(NodoSentencia.LlamadaSuperConstructor s) {
        List<AccesoMemoria> args = new ArrayList<>();
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : s.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigDeExpresion(arg));
        }
        emitirLlamadaConstructorPadre(args, tiposArgs);
    }

    /** Llama al constructor del padre con 'this' como receptor. El resultado (this) se descarta. */
    private void emitirLlamadaConstructorPadre(List<AccesoMemoria> args, List<String> tiposArgs) {
        String padre = tabla.getSuperclase();
        TablaSimbolosZ.Firma firma = resolverSobrecarga(tabla.getConstructoresDe(padre), tiposArgs, padre);
        if (firma == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: constructor de " + padre + " no encontrado para " + tiposArgs);
        }
        String nombre = padre + "_constructor";
        for (String t : firma.tiposParametros()) nombre += "_" + tipoZaPig(t);
        gestor.emitir(new LlamadaMetodo1(null, new AccesoVariable("this", tipoClase), nombre, args));
    }

    /** super.metodo(args): llamada DIRECTA a la versión del padre (no pasa por la tabla de métodos). */
    private void visitarLlamadaMetodoSuper(NodoExpr.LlamadaMetodoSuper l) {
        List<AccesoMemoria> args = new ArrayList<>();
        args.add(new AccesoVariable("this", tipoClase));      // el receptor viaja como primer argumento (a0)
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : l.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigDeExpresion(arg));
        }

        TablaSimbolosZ.Firma firma =
                resolverSobrecarga(tabla.getMetodosEnPadre(tipoClase, l.nombre()), tiposArgs, l.nombre());
        if (firma == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: super." + l.nombre() + " no encontrado en las superclases de " + tipoClase);
        }

        String nombreRiscV = nombreMetodo(claseDeclarante(firma, tabla.getSuperclase()), l.nombre(), firma);
        String tipoRet = firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
        AccesoTemporal t = nuevoTemporal(tipoRet);
        gestor.emitir(new Llamada(t, nombreRiscV, args));
        ultimoResultado = t;
    }
}