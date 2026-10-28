package com.example.stack_over_pig.riscv.piglatin;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionAtributo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.LlamadaMetodo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.NewObjeto;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.piglatin.nodo.NodoExpr;
import com.example.stack_over_pig.piglatin.nodo.NodoSentencia;
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;
import com.example.stack_over_pig.riscv.DisposicionObjetos;
import com.example.stack_over_pig.riscv.VisitanteGeneradorCuartetas;

import java.util.ArrayList;
import java.util.List;

/**
 * Recorre el AST de Pig y emite cuartetas (C3D).
 * Fase 1: variables primitivas, operaciones, condicionales, ciclos,
 * escritura/lectura, incremento/decremento.
 * Fase 2: arreglos (declaracion, acceso, asignacion).
 * No soporta aun: structs, objetos, metodos.
 */
public class GeneradorCuartetasPig implements VisitanteGeneradorCuartetas {

    private final GestorCodigoIntermedio gestor;
    private final TablaSimbolosPig tabla;
    private AccesoMemoria ultimoResultado;
    private final DisposicionObjetos disposicion;

    public GeneradorCuartetasPig(GestorCodigoIntermedio gestor, TablaSimbolosPig tabla) {
        this.gestor = gestor;
        this.tabla = tabla;
        this.disposicion = new DisposicionObjetos(tabla);
    }

    public List<Cuarteta> getCuartetas() {
        return gestor.getCuartetas();
    }

    public ContadorIds getContador() {
        return gestor.getContador();
    }

    // SENTENCIAS

    @Override
    public void visitar(NodoSentencia.DeclaracionVariable d) {
        boolean esEstructura = tabla.buscarEstructura(d.tipo()).isPresent();
        boolean esObjeto = tabla.buscarClase(d.tipo()).isPresent();

        int offset = tabla.asignarOffsetVariable(d.nombre());
        if (!tabla.declararVariable(d.nombre(), d.tipo(), 0, null, esEstructura, esObjeto, d.tipo(), offset)) {
            tabla.actualizarOffsetVariable(d.nombre(), offset);
        }

        if (d.inicializacion() != null) {
            d.inicializacion().aceptar(this);
            AccesoMemoria valor = ultimoResultado;
            AccesoVariable destino = new AccesoVariable(d.nombre(), tipoAC(d.tipo()));
            gestor.emitir(new AsignacionVariable(destino, valor));
        } else {
            AccesoVariable destino = new AccesoVariable(d.nombre(), tipoAC(d.tipo()));
            AccesoMemoria cero = new Literal(0, "numerus");
            gestor.emitir(new AsignacionVariable(destino, cero));
        }
    }

    @Override
    public void visitar(NodoSentencia.DeclaracionArreglo d) {
        int offset = tabla.asignarOffsetVariableConTamano(d.nombre(), d.tamano());
        if (!tabla.declararVariable(d.nombre(), d.tipo(), 1, d.tamano(), false, false, d.tipo(), offset)) {
            tabla.actualizarOffsetVariable(d.nombre(), offset);
        }

        int inicializados = 0;
        if (d.inicializacion() != null) {
            for (int i = 0; i < d.inicializacion().size(); i++) {
                d.inicializacion().get(i).aceptar(this);
                AccesoMemoria valor = ultimoResultado;
                AccesoVariable base = new AccesoVariable(d.nombre(), tipoAC(d.tipo()));
                AccesoMemoria indice = new Literal(i, "numerus");
                gestor.emitir(new AsignacionArreglo(base, indice, valor));
            }
            inicializados = d.inicializacion().size();
        }
        rellenarConCeros(d.nombre(), d.tipo(), inicializados, d.tamano());
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

        gestor.emitir(new DefinicionEtiqueta(lSi));
        for (NodoSentencia s : c.cuerpoSi()) {
            s.aceptar(this);
        }
        gestor.emitir(new Salto(lFin));

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
        String tipo = tabla.buscarVariable(l.variable())
                .map(TablaSimbolosPig.SimboloVariable::tipo)
                .orElse("numerus");
        AccesoVariable destino = new AccesoVariable(l.variable(), tipoAC(tipo));
        gestor.emitir(new LeerPig(destino, tipo));
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

    // EXPRESIONES

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
    public void visitar(NodoExpr.LiteralNulo l) {
        ultimoResultado = new Literal(null, "null");
    }

    @Override
    public void visitar(NodoExpr.Identificador i) {
        String tipo = tabla.buscarVariable(i.nombre())
                .map(TablaSimbolosPig.SimboloVariable::tipo)
                .orElse("numerus");
        ultimoResultado = new AccesoVariable(i.nombre(), tipoAC(tipo));
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
        String nombreRiscV = l.nombre();
        var definicion = tabla.buscarFuncion(l.nombre());
        if (definicion.isPresent()) {
            StringBuilder sb = new StringBuilder(l.nombre());
            for (TablaSimbolosPig.Parametro p : definicion.get().parametros()) {
                sb.append('_').append(p.tipo());
            }
            nombreRiscV = sb.toString();
        }

        int idT = gestor.getContador().siguienteTemporal("int");
        AccesoTemporal t = new AccesoTemporal(idT, "int");
        gestor.emitir(new Llamada(t, nombreRiscV, args));
        ultimoResultado = t;
    }

    @Override
    public void visitar(NodoExpr.AccesoArray a) {
        a.arreglo().aceptar(this);
        AccesoMemoria base = ultimoResultado;

        a.indice().aceptar(this);
        AccesoMemoria indice = ultimoResultado;

        String tipoElemento = "int";
        if (a.arreglo() instanceof NodoExpr.Identificador id) {
            tipoElemento = tipoAC(tabla.buscarVariable(id.nombre())
                    .map(TablaSimbolosPig.SimboloVariable::tipo)
                    .orElse("numerus"));
        }

        ultimoResultado = new AccesoArreglo(base, indice, tipoElemento);
    }

    // HELPERS

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
        if (expr instanceof NodoExpr.Identificador id) {
            return tabla.buscarVariable(id.nombre())
                    .map(TablaSimbolosPig.SimboloVariable::tipo)
                    .orElse("numerus");
        }
        if (expr instanceof NodoExpr.AccesoArray aa && aa.arreglo() instanceof NodoExpr.Identificador id) {
            return tabla.buscarVariable(id.nombre())
                    .map(TablaSimbolosPig.SimboloVariable::tipo)
                    .orElse("numerus");
        }
        if (expr instanceof NodoExpr.AccesoAtributo aa && aa.objeto() instanceof NodoExpr.Identificador id) {
            var varOpt = tabla.buscarVariable(id.nombre());
            if (varOpt.isPresent()) {
                String tipoVar = varOpt.get().tipo();
                // 1. Buscar en estructuras.
                var estOpt = tabla.buscarEstructura(tipoVar);
                if (estOpt.isPresent()) {
                    String t = estOpt.get().atributos().get(aa.atributo());
                    if (t != null) return t;
                }
                // 2. Buscar en clases (incluye atributos heredados).
                var atributoClase = disposicion.buscarAtributo(tipoVar, aa.atributo());
                if (atributoClase.isPresent()) return atributoClase.get().tipo();
            }
            return "numerus";
        }

        if (expr instanceof NodoExpr.InstanciaObjeto io) return io.tipoClase();
        if (expr instanceof NodoExpr.LlamadaMetodo lm) {
            List<String> tiposArgs = new ArrayList<>();
            for (NodoExpr arg : lm.argumentos()) tiposArgs.add(tipoPigLatinDe(arg));
            return disposicion.buscarMetodo(tipoPigLatinDe(lm.objeto()), lm.nombre(), tiposArgs)
                    .map(m -> m.firma().tipoRetorno())
                    .orElse("numerus");
        }

        if (expr instanceof NodoExpr.LlamadaFuncion lf) {
            var def = tabla.buscarFuncion(lf.nombre());
            if (def.isPresent() && def.get().tipoRetorno() != null) {
                return def.get().tipoRetorno();
            }
            return "numerus";
        }

        return "numerus";
    }

    /** Los elementos sin valor inicial quedan en 0, igual que las variables simples. */
    private void rellenarConCeros(String nombre, String tipoPig, int desde, int hasta) {
        int faltan = hasta - desde;
        if (faltan <= 0) return;
        AccesoVariable base = new AccesoVariable(nombre, tipoAC(tipoPig));
        if (faltan <= 8) {
            for (int i = desde; i < hasta; i++) {
                gestor.emitir(new AsignacionArreglo(base, new Literal(i, "numerus"), new Literal(0, "numerus")));
            }
            return;
        }
        int lInicio = gestor.getContador().siguienteEtiqueta();
        int lFin = gestor.getContador().siguienteEtiqueta();
        AccesoTemporal k = new AccesoTemporal(gestor.getContador().siguienteTemporal("int"), "int");
        gestor.emitir(new AsignacionVariable(k, new Literal(desde, "numerus")));
        gestor.emitir(new DefinicionEtiqueta(lInicio));
        gestor.emitir(new Condicional1(k, ">=", new Literal(hasta, "numerus"), lFin));
        gestor.emitir(new AsignacionArreglo(base, k, new Literal(0, "numerus")));
        AccesoTemporal k2 = new AccesoTemporal(gestor.getContador().siguienteTemporal("int"), "int");
        gestor.emitir(new OperacionBinaria(k2, k, "+", new Literal(1, "numerus")));
        gestor.emitir(new AsignacionVariable(k, k2));
        gestor.emitir(new Salto(lInicio));
        gestor.emitir(new DefinicionEtiqueta(lFin));
    }

    @Override
    public void visitar(NodoSentencia.DeclaracionStruct d) {
        var estOpt = tabla.buscarEstructura(d.tipo());
        if (estOpt.isEmpty()) {
            throw new IllegalStateException("Estructura no encontrada: " + d.tipo());
        }
        var est = estOpt.get();
        int numCampos = est.atributos().size();

        int offset = tabla.asignarOffsetStruct(d.nombre(), numCampos);
        if (!tabla.declararVariable(d.nombre(), d.tipo(), 0, null, true, false, d.tipo(), offset)) {
            tabla.actualizarOffsetVariable(d.nombre(), offset);
        }

        List<String> nombresCampos = new ArrayList<>(est.atributos().keySet());
        for (int i = 0; i < d.inicializacion().size() && i < nombresCampos.size(); i++) {
            d.inicializacion().get(i).aceptar(this);
            AccesoMemoria valor = ultimoResultado;
            AccesoVariable base = new AccesoVariable(d.nombre(), d.tipo());
            gestor.emitir(new AsignacionAtributo(base, nombresCampos.get(i), false, valor));
        }

        for (int i = d.inicializacion().size(); i < nombresCampos.size(); i++) {
            AccesoVariable base = new AccesoVariable(d.nombre(), d.tipo());
            AccesoMemoria cero = new Literal(0, "numerus");
            gestor.emitir(new AsignacionAtributo(base, nombresCampos.get(i), false, cero));
        }
    }

    @Override
    public void visitar(NodoExpr.AccesoAtributo a) {
        a.objeto().aceptar(this);
        AccesoMemoria base = ultimoResultado;

        String tipoCampo = "int";
        if (a.objeto() instanceof NodoExpr.Identificador id) {
            var varOpt = tabla.buscarVariable(id.nombre());
            if (varOpt.isPresent()) {
                var v = varOpt.get();
                var estOpt = tabla.buscarEstructura(v.tipo());
                if (estOpt.isPresent()) {
                    String t = estOpt.get().atributos().get(a.atributo());
                    if (t != null) tipoCampo = tipoAC(t);
                }
            }
        }

        ultimoResultado = new AccesoAtributo1(base, a.atributo(), false, tipoCampo);
    }

    @Override
    public void visitar(NodoSentencia.Asignacion a) {
        a.valor().aceptar(this);
        AccesoMemoria valor = ultimoResultado;
        a.destino().aceptar(this);
        AccesoMemoria destino = ultimoResultado;

        if (destino instanceof AccesoArreglo arr) {
            gestor.emitir(new AsignacionArreglo(arr.getBase(), arr.getIndice(), valor));
        } else if (destino instanceof AccesoAtributo1 attr) {
            gestor.emitir(new AsignacionAtributo(attr.getBase(), attr.getCampo(), false, valor));
        } else {
            gestor.emitir(new AsignacionVariable(destino, valor));
        }
    }

    // OBJETOS

    @Override
    public void visitar(NodoExpr.InstanciaObjeto inst) {
        // 1. Evaluar argumentos.
        List<AccesoMemoria> args = new ArrayList<>();
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : inst.argumentos()) {
            arg.aceptar(this);
            args.add(ultimoResultado);
            tiposArgs.add(tipoPigLatinDe(arg));
        }

        // 2. Nombre del constructor, a partir de los parametros DECLARADOS.
        List<String> tiposParam = disposicion.buscarConstructor(inst.tipoClase(), tiposArgs)
                .orElseThrow(() -> new IllegalStateException(
                        "GeneradorCuartetasPig: constructor no encontrado en " + inst.tipoClase()
                                + " para argumentos " + tiposArgs));
        for (int i = 0; i < args.size(); i++) {
            args.set(i, adaptarArgumento(args.get(i), tiposArgs.get(i), tiposParam.get(i)));
        }
        String nombre = DisposicionObjetos.nombreConstructor(inst.tipoClase(), tiposParam);

        // 3. Temporal para el objeto.
        int idT = gestor.getContador().siguienteTemporal(inst.tipoClase());
        AccesoTemporal t = new AccesoTemporal(idT, inst.tipoClase());

        // 4. Emitir NewObjeto.
        gestor.emitir(new NewObjeto(t, inst.tipoClase(), nombre.toString(), args));
        ultimoResultado = t;
    }

    @Override
    public void visitar(NodoExpr.LlamadaMetodo llamada) {
        // 1. Evaluar receptor.
        llamada.objeto().aceptar(this);
        AccesoMemoria receptor = ultimoResultado;
        String tipoObj = tipoPigLatinDe(llamada.objeto());

        System.out.println("[DEBUG] metodo=" + llamada.nombre()
                + " receptor=" + llamada.objeto()
                + " tipoObj=" + tipoObj);
        List<String> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : llamada.argumentos()) {
            tiposArgs.add(tipoPigLatinDe(arg));
        }
        DisposicionObjetos.MetodoResuelto metodo = disposicion
                .buscarMetodo(tipoObj, llamada.nombre(), tiposArgs)
                .orElseThrow(() -> new IllegalStateException(
                        "GeneradorCuartetasPig: método no encontrado: " + llamada.nombre() + " en " + tipoObj));
        TablaSimbolosPig.Firma firma = metodo.firma();

        // 3. Evaluar argumentos.
        List<AccesoMemoria> args = new ArrayList<>();
        for (NodoExpr arg : llamada.argumentos()) {
            arg.aceptar(this);
            args.add(ultimoResultado);
        }

        // 4. Adaptar cada argumento al tipo declarado del parámetro (promoción num→decimal).
        for (int i = 0; i < args.size(); i++) {
            args.set(i, adaptarArgumento(args.get(i), tiposArgs.get(i), firma.parametros().get(i).tipo()));
        }

        // 5. Nombre ClaseDeclarante_metodo_<tipos de los parámetros declarados>.
        String nombreRiscV = DisposicionObjetos.nombreMetodo(metodo.claseDeclarante(), llamada.nombre(), firma);

        // 6. Temporal para el retorno.
        int idT = gestor.getContador().siguienteTemporal(
                firma.tipoRetorno() != null ? firma.tipoRetorno() : "void");
        AccesoTemporal t = new AccesoTemporal(idT,
                firma.tipoRetorno() != null ? firma.tipoRetorno() : "void");

        // 7. Emitir LlamadaMetodo1.
        gestor.emitir(new LlamadaMetodo1(t, receptor, nombreRiscV, args));
        ultimoResultado = firma.tipoRetorno() != null ? t : null;
    }

    private AccesoMemoria adaptarArgumento(AccesoMemoria arg, String tipoArg, String tipoParam) {
        if (!"numerus".equals(tipoArg) || !"decimalis".equals(tipoParam)) return arg;
        if (arg instanceof Literal l && l.getValor() instanceof Number n) {
            return new Literal(n.doubleValue(), "decimalis");
        }
        AccesoTemporal t = new AccesoTemporal(gestor.getContador().siguienteTemporal("decimalis"), "decimalis");
        gestor.emitir(new OperacionBinaria(t, arg, "*", new Literal(1000000, "numerus")));
        return t;
    }

    @Override
    public void visitar(NodoSentencia.LlamadaMetodoSentencia l) {
        l.llamada().aceptar(this);
    }
}