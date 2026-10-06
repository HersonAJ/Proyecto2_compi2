package com.example.contacto_3xtrat3r3str3.y.semantica;

import com.example.contacto_3xtrat3r3str3.y.ast.*;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orquesta todas las validaciones semánticas del lenguaje .y.
 *
 * Orden de ejecución:
 *   1. Declarar estructuras y firmas de funciones   -> analizar
 *   2. Procesar cuerpo de cada función              -> procesarFuncion
 *   3. Procesar cada sentencia del cuerpo           -> procesarSentencia
 */
public class ValidadorSemantico {

    private final TablaSimbolos tabla = new TablaSimbolos();
    private final List<ErrorSemantico> errores = new ArrayList<>();

    private final ValidadorDeclaraciones declaraciones = new ValidadorDeclaraciones(tabla, errores);
    private final ValidadorAlcance alcance = new ValidadorAlcance(tabla, errores);
    private final ValidadorTipos tipos = new ValidadorTipos(tabla, errores);
    private final ValidadorEstructuras estructuras = new ValidadorEstructuras(tabla, errores, tipos);
    private final ValidadorFlujo flujo = new ValidadorFlujo(errores);
    private final ValidadorAsignaciones asignaciones = new ValidadorAsignaciones(tabla, errores);

    private NodoFuncion.Funcion funcionActual;

    /** Punto de entrada: recorre el programa completo. */
    public List<ErrorSemantico> analizar(NodoPrograma.Programa programa) {
        // Fase 1: declarar estructuras y firmas de funciones.
        for (NodoEstructura e : programa.estructuras()) {
            declaraciones.declararEstructura((NodoEstructura.Estructura) e);
        }
        for (NodoFuncion f : programa.funciones()) {
            declaraciones.registrarFirmaFuncion((NodoFuncion.Funcion) f);
        }
        // Fase 2: procesar el cuerpo de cada función.
        for (NodoFuncion f : programa.funciones()) {
            procesarFuncion((NodoFuncion.Funcion) f);
        }
        return errores;
    }

    /** Procesa el cuerpo de una función dentro de su propio scope. */
    private void procesarFuncion(NodoFuncion.Funcion funcion) {
        funcionActual = funcion;
        tabla.entrarScope(funcion.nombre());
        declaraciones.declararParametrosEnScope(funcion);
        procesarBloque(funcion.cuerpo());
        flujo.validarRetornoGarantizado(
                funcion.tipoRetorno(),
                funcion.cuerpo(),
                funcion.linea(),
                funcion.columna()
        );

        tabla.salirScope();
        funcionActual = null;
    }

    /** Procesa un bloque: primero código inalcanzable, luego cada sentencia. */
    private void procesarBloque(List<NodoSentencia> bloque) {
        flujo.validarCodigoInalcanzable(bloque);
        for (NodoSentencia s : bloque) procesarSentencia(s);
    }

    /** Procesa cada tipo de sentencia con sus validaciones correspondientes. */
    private void procesarSentencia(NodoSentencia s) {
        switch (s.tipoNodo()) {

            case DECLARACION_VARIABLE -> {
                NodoSentencia.DeclaracionVariable d = (NodoSentencia.DeclaracionVariable) s;
                // 1. Validar que el inicializador sea compatible con el tipo declarado.
                tipos.validarInicializacion(d.tipo(), d.inicializacion());
                // 2. Resolver identificadores dentro del inicializador.
                alcance.resolverExpresion(d.inicializacion());
                validarLlamadasEnExpresion(d.inicializacion());
                // 3. Declarar la variable.
                declaraciones.declararVariable(d);
            }

            case DECLARACION_ARREGLO -> {
                NodoSentencia.DeclaracionArreglo d = (NodoSentencia.DeclaracionArreglo) s;
                // 1. Validar cada inicializador contra el tipo base.
                for (var expr : d.inicializacion()) {
                    tipos.validarInicializacion(d.tipo(), expr);
                    alcance.resolverExpresion(expr);
                    validarLlamadasEnExpresion(expr);
                }
                // 2. Declarar el arreglo.
                declaraciones.declararArreglo(d);
            }

            case DECLARACION_MATRIZ -> {
                declaraciones.declararMatriz((NodoSentencia.DeclaracionMatriz) s);
            }

            case DECLARACION_ESTRUCTURA -> {
                NodoSentencia.DeclaracionEstructura d = (NodoSentencia.DeclaracionEstructura) s;
                // 1. Resolver que la estructura existe.
                alcance.resolverTipoEstructura(d.tipoEstructura(), d.linea(), d.columna());
                // 2. Resolver y validar el inicializador si lo hay.
                for (var expr : d.inicializacion()) {
                    alcance.resolverExpresion(expr);
                    tipos.tipoDeExpresion(expr);
                    validarLlamadasEnExpresion(expr);
                }
                // 3. Declarar la variable.
                estructuras.validarInicializacionEstructura(d);
                declaraciones.declararVariableEstructura(d);
            }

            case ASIGNACION -> {
                NodoSentencia.Asignacion a = (NodoSentencia.Asignacion) s;
                alcance.resolverExpresion(a.destino());
                alcance.resolverExpresion(a.valor());
                asignaciones.validarDestino(a);
                validarAsignacion(a);
            }

            case INCREMENTO_DECREMENTO -> {
                NodoSentencia.IncrementoDecremento i = (NodoSentencia.IncrementoDecremento) s;
                alcance.resolverNombre(i.nombre(), i.linea(), i.columna());
                validarIncrementoDecremento(i);
            }

            case CONDICIONAL -> procesarCondicional((NodoSentencia.Condicional) s);
            case ELEGIR -> procesarElegir((NodoSentencia.Elegir) s);
            case CICLO_PARA -> procesarCicloPara((NodoSentencia.CicloPara) s);
            case CICLO_MIENTRAS -> procesarCicloMientras((NodoSentencia.CicloMientras) s);
            case CICLO_HACER_MIENTRAS -> procesarCicloHacerMientras((NodoSentencia.CicloHacerMientras) s);

            case RETORNO -> {
                NodoSentencia.Retorno r = (NodoSentencia.Retorno) s;
                alcance.resolverExpresion(r.valor());
                tipos.tipoDeExpresion(r.valor());
                // Validar contra el tipo de retorno de la función actual.
                String tipoEsperado = funcionActual != null ? funcionActual.tipoRetorno() : null;
                tipos.validarRetorno(r, tipoEsperado);
            }

            case IMPRIMIR -> {
                NodoExpr valor = ((NodoSentencia.Imprimir) s).expresion();
                alcance.resolverExpresion(valor);
                validarLlamadasEnExpresion(valor);
                tipos.tipoDeExpresion(valor);
            }

            case LEER -> { }

            case ROMPER -> flujo.validarRomper((NodoSentencia.Romper) s);
            case CONTINUAR -> flujo.validarContinuar((NodoSentencia.Continuar) s);

            case DECLARACION_ESTRUCTURA_LOCAL -> {
                NodoSentencia.DeclaracionEstructuraLocal d = (NodoSentencia.DeclaracionEstructuraLocal) s;
                declaraciones.declararEstructura(d.estructura());
            }
        }
    }

    // VALIDACIONES ESPECÍFICAS
    /** Valida que los tipos del destino y del valor de una asignación sean compatibles. */
    private void validarAsignacion(NodoSentencia.Asignacion asignacion) {
        String tipoDestino = tipos.tipoDeExpresion(asignacion.destino());
        String tipoValor = tipos.tipoDeExpresion(asignacion.valor());

        if (tipoDestino == null || tipoValor == null) return;
        tipos.validarAsignacion(tipoDestino, tipoValor, asignacion.linea(), asignacion.columna());
    }

    /** Valida que el operando de ++/-- sea numérico. */
    private void validarIncrementoDecremento(NodoSentencia.IncrementoDecremento inc) {
        Optional<TablaSimbolos.SimboloVariable> simbolo = tabla.buscarVariable(inc.nombre());
        if (simbolo.isEmpty()) return;

        String tipo = simbolo.get().tipo();
        if (tipo == null) return;

        if (!tipo.equals("entero") && !tipo.equals("flotante")) {
            errores.add(new ErrorSemantico(inc.linea(), inc.columna(),
                    "Tipo incompatible en incremento/decremento",
                    "El operador '" + inc.operador() + "' requiere tipo numérico, se encontró '" + tipo + "'"));
        }
    }

    /** Valida recursivamente las llamadas dentro de una expresión. */
    private void validarLlamadasEnExpresion(NodoExpr expr) {
        if (expr == null) return;

        if (expr instanceof NodoExpr.LlamadaFuncion llamada) {
            Optional<TablaSimbolos.DefinicionFuncion> def = tabla.buscarFuncion(llamada.nombre());
            def.ifPresent(d -> tipos.validarLlamada(llamada, d));
            for (NodoExpr arg : llamada.argumentos()) {
                validarLlamadasEnExpresion(arg);
            }
        } else if (expr instanceof NodoExpr.Binaria bin) {
            validarLlamadasEnExpresion(bin.izquierda());
            validarLlamadasEnExpresion(bin.derecha());
        } else if (expr instanceof NodoExpr.Unaria un) {
            validarLlamadasEnExpresion(un.operando());
        } else if (expr instanceof NodoExpr.AccesoArray arr) {
            validarLlamadasEnExpresion(arr.arreglo());
            validarLlamadasEnExpresion(arr.indice());
        } else if (expr instanceof NodoExpr.AccesoAtributo atr) {
            validarLlamadasEnExpresion(atr.objeto());
        }
    }

    // ESTRUCTURAS DE CONTROL
    /** Procesa un 'si/sino/contrario'. */
    private void procesarCondicional(NodoSentencia.Condicional c) {
        alcance.resolverExpresion(c.condicion());
        tipos.tipoDeExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
        validarLlamadasEnExpresion(c.condicion());

        tabla.entrarScope("si");
        procesarBloque(c.cuerpoSi());
        tabla.salirScope();

        if (c.cuerpoSino() != null) {
            alcance.resolverExpresion(c.condicionSino());
            tipos.tipoDeExpresion(c.condicionSino());
            tipos.validarCondicionBooleana(c.condicionSino());
            validarLlamadasEnExpresion(c.condicionSino());

            tabla.entrarScope("sino");
            procesarBloque(c.cuerpoSino());
            tabla.salirScope();
        }
        if (c.cuerpoContrario() != null) {
            tabla.entrarScope("contrario");
            procesarBloque(c.cuerpoContrario());
            tabla.salirScope();
        }
    }

    /** Procesa un 'elegir' con sus casos y 'siempre'. */
    private void procesarElegir(NodoSentencia.Elegir e) {
        alcance.resolverExpresion(e.expresion());
        validarLlamadasEnExpresion(e.expresion());

        for (NodoSentencia.CasoElegir caso : e.casos()) {
            alcance.resolverExpresion(caso.valor());
            validarLlamadasEnExpresion(caso.valor());

            tabla.entrarScope("caso");
            procesarBloque(caso.cuerpo());
            tabla.salirScope();
        }
        if (e.siempre() != null) {
            tabla.entrarScope("siempre");
            procesarBloque(e.siempre().cuerpo());
            tabla.salirScope();
        }
    }

    /** Procesa un ciclo 'para'. */
    private void procesarCicloPara(NodoSentencia.CicloPara c) {
        tabla.entrarScope("para");
        declaraciones.declararVariableCiclo(c.nombreVariable(), c.tipoInicializacion(), c.linea(), c.columna());

        alcance.resolverExpresion(c.valorInicial());
        tipos.tipoDeExpresion(c.valorInicial());
        validarLlamadasEnExpresion(c.valorInicial());

        alcance.resolverExpresion(c.condicion());
        tipos.tipoDeExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
        validarLlamadasEnExpresion(c.condicion());

        flujo.entrarCiclo();
        procesarBloque(c.cuerpo());
        flujo.salirCiclo();
        tabla.salirScope();
    }

    /** Procesa un ciclo 'mientras'. */
    private void procesarCicloMientras(NodoSentencia.CicloMientras c) {
        alcance.resolverExpresion(c.condicion());
        tipos.tipoDeExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
        validarLlamadasEnExpresion(c.condicion());

        tabla.entrarScope("mientras");
        flujo.entrarCiclo();
        procesarBloque(c.cuerpo());
        flujo.salirCiclo();
        tabla.salirScope();
    }

    /** Procesa un ciclo 'hacer-mientras'. */
    private void procesarCicloHacerMientras(NodoSentencia.CicloHacerMientras c) {
        tabla.entrarScope("hacer-mientras");
        flujo.entrarCiclo();
        procesarBloque(c.cuerpo());
        flujo.salirCiclo();
        tabla.salirScope();

        alcance.resolverExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
        validarLlamadasEnExpresion(c.condicion());
    }

    public List<ErrorSemantico> getErrores() {
        return errores;
    }

    public TablaSimbolos getTabla() {
        return tabla;
    }
}