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
            case LLAMADA_SUPER_CONSTRUCTOR -> throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: super(...) se implementará en Fase 3c");
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

    private void visitarImprimir(NodoSentencia.Imprimir i) {
        visitarExpresion(i.expresion());
        AccesoMemoria acc = ultimoResultado;
        String tipo = tipoPigDeExpresion(i.expresion());
        gestor.emitir(new ImprimirPig(acc, tipo));
    }

    // ========== EXPRESIONES ==========

    private void visitarExpresion(NodoExpr e) {
        switch (e.tipoNodo()) {
            case LITERAL_ENTERO -> ultimoResultado = new Literal(((NodoExpr.LiteralEntero) e).valor(), "numerus");
            case LITERAL_DECIMAL -> throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: decimalis no soportado (punto flotante)");
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
            case LLAMADA_METODO_SUPER -> throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: super.metodo() se implementará en Fase 3c");
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
        visitarExpresion(b.izquierda());
        AccesoMemoria izq = ultimoResultado;
        visitarExpresion(b.derecha());
        AccesoMemoria der = ultimoResultado;

        String tipoRes = tipoBinario(izq.getTipo(), b.operador(), der.getTipo());
        int idT = gestor.getContador().siguienteTemporal(tipoRes);
        AccesoTemporal t = new AccesoTemporal(idT, tipoRes);
        gestor.emitir(new OperacionBinaria(t, izq, b.operador(), der));
        ultimoResultado = t;
    }

    private void visitarUnaria(NodoExpr.Unaria u) {
        visitarExpresion(u.operando());
        AccesoMemoria op = ultimoResultado;
        String tipoRes = "!".equals(u.operador()) ? "bool" : "numerus";
        int idT = gestor.getContador().siguienteTemporal(tipoRes);
        AccesoTemporal t = new AccesoTemporal(idT, tipoRes);
        gestor.emitir(new OperacionUnaria(t, u.operador(), op));
        ultimoResultado = t;
    }

    private void visitarIncrementoDecremento(NodoExpr.IncrementoDecremento inc) {
        visitarExpresion(inc.operando());
        AccesoMemoria op = ultimoResultado;

        String opBinario = "++".equals(inc.operador()) ? "+" : "-";
        AccesoMemoria uno = new Literal(1, "numerus");
        int idT = gestor.getContador().siguienteTemporal("numerus");
        AccesoTemporal t = new AccesoTemporal(idT, "numerus");
        gestor.emitir(new OperacionBinaria(t, op, opBinario, uno));

        if (op instanceof AccesoAtributo1 attr) {
            gestor.emitir(new AsignacionAtributo(attr.getBase(), attr.getCampo(), true, t));
        } else {
            gestor.emitir(new AsignacionVariable(op, t));
        }
        ultimoResultado = op;
    }

    private void visitarTernaria(NodoExpr.Ternaria t) {
        int lSi = gestor.getContador().siguienteEtiqueta();
        int lNo = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        visitarExpresion(t.condicion());
        AccesoMemoria cond = ultimoResultado;
        gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lNo));
        gestor.emitir(new Salto(lSi));

        gestor.emitir(new DefinicionEtiqueta(lSi));
        visitarExpresion(t.siVerdadero());
        AccesoMemoria temp = ultimoResultado;
        gestor.emitir(new Salto(lFin));

        gestor.emitir(new DefinicionEtiqueta(lNo));
        visitarExpresion(t.siFalso());
        gestor.emitir(new DefinicionEtiqueta(lFin));

        ultimoResultado = temp;
    }

    private void visitarLlamadaFuncion(NodoExpr.LlamadaFuncion l) {
        List<AccesoMemoria> args = new ArrayList<>();
        for (NodoExpr arg : l.argumentos()) {
            visitarExpresion(arg);
            args.add(ultimoResultado);
        }
        int idT = gestor.getContador().siguienteTemporal("numerus");
        AccesoTemporal t = new AccesoTemporal(idT, "numerus");
        gestor.emitir(new Llamada(t, l.nombre(), args));
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

        List<TablaSimbolosZ.Firma> firmas = tabla.getMetodosDe(tipoObj, l.nombre());
        TablaSimbolosZ.Firma firma = resolverSobrecarga(firmas, tiposArgs, l.nombre());
        if (firma == null) {
            throw new IllegalStateException(
                    "GeneradorCuartetasZ: método no encontrado: " + l.nombre() + " en " + tipoObj);
        }

        String nombreRiscV = tipoObj + "_" + l.nombre();
        for (String t : tiposArgs) nombreRiscV += "_" + t;

        // El temporal del resultado toma el tipo de retorno del método (para que una
        // concatenación como obj.nombre() + "x" sepa que el operando es textum).
        String tipoRet = firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
        int idT = gestor.getContador().siguienteTemporal(tipoRet);
        AccesoTemporal t = new AccesoTemporal(idT, tipoRet);
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

        String nombreConstructor = inst.tipoClase() + "_constructor";
        for (String t : tiposArgs) nombreConstructor += "_" + t;

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

        int idT = gestor.getContador().siguienteTemporal("numerus");
        AccesoTemporal t = new AccesoTemporal(idT, "numerus");

        if (dims.size() == 1) {
            gestor.emitir(new NewArreglo(t, a.tipoBase(), dims.get(0)));
        } else {
            throw new UnsupportedOperationException(
                    "GeneradorCuartetasZ: arreglos multidimensionales pendientes (Fase 3b.2)");
        }
        ultimoResultado = t;
    }

    // ========== HELPERS ==========

    /** Tipo (vocabulario Pig) del resultado de una operación binaria. */
    private static String tipoBinario(String tipoIzq, String operador, String tipoDer) {
        return switch (operador) {
            case "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> "bool";
            case "+" -> ("textum".equals(tipoIzq) || "textum".equals(tipoDer)) ? "textum" : "numerus";
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
                var firmas = tabla.getMetodosDe(tipoObj, lm.nombre());
                var firma = resolverSobrecarga(firmas, tiposArgs, lm.nombre());
                yield firma != null && firma.tipoRetorno() != null ? tipoZaPig(firma.tipoRetorno()) : "numerus";
            }
            case BINARIA -> {
                NodoExpr.Binaria b = (NodoExpr.Binaria) e;
                yield tipoBinario(tipoPigDeExpresion(b.izquierda()), b.operador(), tipoPigDeExpresion(b.derecha()));
            }
            case UNARIA -> "!".equals(((NodoExpr.Unaria) e).operador()) ? "bool" : "numerus";
            default -> "numerus";
        };
    }

    /** Busca la firma cuyos tipos de parámetro (traducidos a Pig) coinciden con los de los argumentos. */
    private TablaSimbolosZ.Firma resolverSobrecarga(List<TablaSimbolosZ.Firma> firmas,
                                                    List<String> tiposArgs, String nombre) {
        for (TablaSimbolosZ.Firma f : firmas) {
            List<String> tiposFirma = f.tiposParametros().stream()
                    .map(GeneradorCuartetasZ::tipoZaPig)
                    .toList();
            if (tiposFirma.equals(tiposArgs)) return f;
        }
        return null;
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
}