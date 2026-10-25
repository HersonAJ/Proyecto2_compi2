package com.example.stack_over_pig.riscv.piglatin;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.piglatin.nodo.NodoExpr;
import com.example.stack_over_pig.piglatin.nodo.NodoSentencia;
import com.example.stack_over_pig.riscv.VisitanteGeneradorCuartetas;

import java.util.ArrayList;
import java.util.List;

/**
 * Recorre el AST de Pig y emite cuartetas (C3D) para la Fase 1.
 * Soporta: variables primitivas, operaciones, condicionales, ciclos,
 * escritura/lectura, incremento/decremento.
 * No soporta: arreglos, structs, objetos, metodos.
 */
public class GeneradorCuartetasPig implements VisitanteGeneradorCuartetas {

    private final GestorCodigoIntermedio gestor;
    private AccesoMemoria ultimoResultado;

    public GeneradorCuartetasPig(GestorCodigoIntermedio gestor) {
        this.gestor = gestor;
    }

    public List<Cuarteta> getCuartetas() {
        return gestor.getCuartetas();
    }

    public ContadorIds getContador() {
        return gestor.getContador();
    }

    // ========== SENTENCIAS ==========

    @Override
    public void visitar(NodoSentencia.DeclaracionVariable d) {
        if (d.inicializacion() != null) {
            d.inicializacion().aceptar(this);
            AccesoMemoria valor = ultimoResultado;
            AccesoVariable destino = new AccesoVariable(d.nombre(), tipoAC(d.tipo()));
            gestor.emitir(new AsignacionVariable(destino, valor));
        } else {
            // Declarar sin inicializar: emitir asignación de 0
            AccesoVariable destino = new AccesoVariable(d.nombre(), tipoAC(d.tipo()));
            AccesoMemoria cero = new Literal(0, "numerus");
            gestor.emitir(new AsignacionVariable(destino, cero));
        }
    }

    @Override
    public void visitar(NodoSentencia.Asignacion a) {
        a.valor().aceptar(this);
        AccesoMemoria valor = ultimoResultado;
        a.destino().aceptar(this);
        AccesoMemoria destino = ultimoResultado;
        gestor.emitir(new AsignacionVariable(destino, valor));
    }

    @Override
    public void visitar(NodoSentencia.Condicional c) {
        int lSi = gestor.getContador().siguienteEtiqueta();
        int lNo = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();

        c.condicion().aceptar(this);
        AccesoMemoria cond = ultimoResultado;

        gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lNo));
        gestor.emitir(new Salto(lSi));

        // Bloque si
        gestor.emitir(new DefinicionEtiqueta(lSi));
        for (NodoSentencia s : c.cuerpoSi()) {
            s.aceptar(this);
        }
        gestor.emitir(new Salto(lFin));

        // Bloque aliter (siempre se emite la etiqueta lNo)
        gestor.emitir(new DefinicionEtiqueta(lNo));
        for (NodoSentencia.RamaAliter rama : c.ramasAliter()) {
            int lSiguiente = gestor.getContador().siguienteEtiqueta();
            rama.condicion().aceptar(this);
            AccesoMemoria condRama = ultimoResultado;
            gestor.emitir(new Condicional1(condRama, "==", new Literal(0, "bool"), lSiguiente));
            for (NodoSentencia s : rama.cuerpo()) {
                s.aceptar(this);
            }
            gestor.emitir(new Salto(lFin));
            gestor.emitir(new DefinicionEtiqueta(lSiguiente));
        }
        if (c.cuerpoAliter() != null) {
            for (NodoSentencia s : c.cuerpoAliter()) {
                s.aceptar(this);
            }
        }

        gestor.emitir(new DefinicionEtiqueta(lFin));
    }
    @Override
    public void visitar(NodoSentencia.CicloDum c) {
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lRomper = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));

        c.condicion().aceptar(this);
        AccesoMemoria cond = ultimoResultado;
        gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lRomper));

        gestor.entrarCiclo(new ContextoCiclo(lInicio, lRomper));
        for (NodoSentencia s : c.cuerpo()) {
            s.aceptar(this);
        }
        gestor.salirCiclo();

        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lRomper));
    }

    @Override
    public void visitar(NodoSentencia.CicloFacere c) {
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lCondicion = gestor.getContador().siguienteEtiqueta();
        int lRomper = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));
        gestor.entrarCiclo(new ContextoCiclo(lCondicion, lRomper));

        for (NodoSentencia s : c.cuerpo()) {
            s.aceptar(this);
        }

        gestor.salirCiclo();
        gestor.emitir(new DefinicionEtiqueta(lCondicion));

        c.condicion().aceptar(this);
        AccesoMemoria cond = ultimoResultado;
        gestor.emitir(new Condicional1(cond, "!=", new Literal(0, "bool"), lInicio));

        gestor.emitir(new DefinicionEtiqueta(lRomper));
    }

    @Override
    public void visitar(NodoSentencia.CicloPer c) {
        // Inicialización
        if (c.inicializacion() != null) {
            c.inicializacion().aceptar(this);
        }

        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lContinuar = gestor.getContador().siguienteEtiqueta();
        int lRomper = gestor.getContador().siguienteEtiqueta();

        gestor.emitir(new DefinicionEtiqueta(lInicio));

        c.condicion().aceptar(this);
        AccesoMemoria cond = ultimoResultado;
        gestor.emitir(new Condicional1(cond, "==", new Literal(0, "bool"), lRomper));

        gestor.entrarCiclo(new ContextoCiclo(lContinuar, lRomper));
        for (NodoSentencia s : c.cuerpo()) {
            s.aceptar(this);
        }
        gestor.salirCiclo();

        gestor.emitir(new DefinicionEtiqueta(lContinuar));
        if (c.actualizacion() != null) {
            c.actualizacion().aceptar(this);
        }

        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lRomper));
    }

    @Override
    public void visitar(NodoSentencia.Escritura e) {
        for (NodoExpr valor : e.valores()) {
            valor.aceptar(this);
            AccesoMemoria acc = ultimoResultado;
            String tipoPig = tipoPigLatinDe(valor);
            gestor.emitir(new ImprimirPig(acc, tipoPig));
        }
    }

    @Override
    public void visitar(NodoSentencia.Lectura l) {
        if (l.variable() == null) return;
        AccesoVariable destino = new AccesoVariable(l.variable(), "numerus");
        gestor.emitir(new LeerPig(destino, "numerus"));
    }

    @Override
    public void visitar(NodoSentencia.IncrementoDecremento i) {
        i.operando().aceptar(this);
        AccesoMemoria op = ultimoResultado;

        String opBinario = "++".equals(i.operador()) ? "+" : "-";
        AccesoMemoria uno = new Literal(1, "numerus");

        int idT = gestor.getContador().siguienteTemporal("int");
        AccesoTemporal t = new AccesoTemporal(idT, "int");
        gestor.emitir(new OperacionBinaria(t, op, opBinario, uno));

        gestor.emitir(new AsignacionVariable(op, t));
        ultimoResultado = op;
    }

    @Override
    public void visitar(NodoSentencia.InterrupcionCiclo i) {
        ContextoCiclo ciclo = gestor.cicloActual();
        if (ciclo == null) return;

        if ("perge".equals(i.tipo())) {
            gestor.emitir(new Continuar1(ciclo.getEtiquetaContinuar()));
        } else {
            gestor.emitir(new Romper1(ciclo.getEtiquetaRomper()));
        }
    }

    @Override
    public void visitar(NodoSentencia.LlamadaFuncionSentencia l) {
        l.llamada().aceptar(this);
    }

    // ========== EXPRESIONES ==========

    @Override
    public void visitar(NodoExpr.LiteralEntero l) {
        ultimoResultado = new Literal(l.valor(), "numerus");
    }

    @Override
    public void visitar(NodoExpr.LiteralDecimal l) {
        ultimoResultado = new Literal(l.valor(), "decimalis");
    }

    @Override
    public void visitar(NodoExpr.LiteralTexto l) {
        ultimoResultado = new Literal(l.valor(), "textum");
    }

    @Override
    public void visitar(NodoExpr.LiteralCaracter l) {
        ultimoResultado = new Literal(l.valor(), "littera");
    }

    @Override
    public void visitar(NodoExpr.LiteralBool l) {
        ultimoResultado = new Literal(l.valor(), "bool");
    }

    @Override
    public void visitar(NodoExpr.Identificador i) {
        ultimoResultado = new AccesoVariable(i.nombre(), "numerus");
    }

    @Override
    public void visitar(NodoExpr.Binaria b) {
        b.izquierda().aceptar(this);
        AccesoMemoria izq = ultimoResultado;

        b.derecha().aceptar(this);
        AccesoMemoria der = ultimoResultado;

        int idT = gestor.getContador().siguienteTemporal("int");
        AccesoTemporal t = new AccesoTemporal(idT, "int");
        gestor.emitir(new OperacionBinaria(t, izq, b.operador(), der));
        ultimoResultado = t;
    }

    @Override
    public void visitar(NodoExpr.Unaria u) {
        u.operando().aceptar(this);
        AccesoMemoria op = ultimoResultado;

        int idT = gestor.getContador().siguienteTemporal("int");
        AccesoTemporal t = new AccesoTemporal(idT, "int");
        gestor.emitir(new OperacionUnaria(t, u.operador(), op));
        ultimoResultado = t;
    }

    @Override
    public void visitar(NodoExpr.LlamadaFuncion l) {
        List<AccesoMemoria> args = new ArrayList<>();
        for (NodoExpr arg : l.argumentos()) {
            arg.aceptar(this);
            args.add(ultimoResultado);
        }

        int idT = gestor.getContador().siguienteTemporal("int");
        AccesoTemporal t = new AccesoTemporal(idT, "int");
        gestor.emitir(new Llamada(t, l.nombre(), args));
        ultimoResultado = t;
    }

    // ========== HELPERS ==========

    private String tipoAC(String tipoPig) {
        return switch (tipoPig) {
            case "numerus"   -> "int";
            case "decimalis" -> "float";
            case "littera"   -> "char";
            case "bool"      -> "int";
            case "textum"    -> "char*";
            default          -> "int";
        };
    }

    private String tipoPigLatinDe(NodoExpr expr) {
        if (expr instanceof NodoExpr.LiteralEntero) return "numerus";
        if (expr instanceof NodoExpr.LiteralDecimal) return "decimalis";
        if (expr instanceof NodoExpr.LiteralTexto) return "textum";
        if (expr instanceof NodoExpr.LiteralCaracter) return "littera";
        if (expr instanceof NodoExpr.LiteralBool) return "bool";
        return "numerus";
    }
}