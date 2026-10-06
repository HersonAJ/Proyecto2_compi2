package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoPrograma;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta todos los validadores semánticos de PigLatin.
 *
 * Orden de ejecución:
 *   1. Cargar importaciones               -> ValidadorImportacionesPig
 *   2. Procesar variables globales        -> procesarSentenciaGlobal
 *   3. Procesar cuerpo del MAIOR          -> procesarBloque
 *   4. Procesar cada sentencia            -> procesarSentencia
 */
public class ValidadorSemanticoPig {

    private final TablaSimbolosPig tabla = new TablaSimbolosPig();
    private final List<ErrorSemantico> errores = new ArrayList<>();

    private final Path carpetaRaiz;

    private ValidadorImportacionesPig importaciones;
    private final ValidadorDeclaracionesPig declaraciones;
    private final ValidadorAlcancePig alcance;
    private final ValidadorTiposPig tipos;
    private final ValidadorFlujoPig flujo;
    private final ValidadorAsignacionesPig asignaciones;
    private final ValidadorEstructurasPig estructuras;
    private final ValidadorObjetosPig objetos;

    public ValidadorSemanticoPig(Path carpetaRaiz) {
        this.carpetaRaiz = carpetaRaiz;

        this.declaraciones = new ValidadorDeclaracionesPig(tabla, errores);
        this.alcance = new ValidadorAlcancePig(tabla, errores);
        this.tipos = new ValidadorTiposPig(tabla, errores);
        this.flujo = new ValidadorFlujoPig(errores);
        this.asignaciones = new ValidadorAsignacionesPig(tabla, errores);
        this.estructuras = new ValidadorEstructurasPig(tabla, errores, tipos);
        this.objetos = new ValidadorObjetosPig(tabla, errores, tipos);
    }

    // PUNTO DE ENTRADA
    /** Punto de entrada: recorre el programa completo. */
    public List<ErrorSemantico> analizar(NodoPrograma programa) {
        // 1. IMPORTACIONES
        importaciones = new ValidadorImportacionesPig(tabla, errores, carpetaRaiz);
        boolean importacionesOk = importaciones.procesarImportaciones(programa.importaciones());
        if (!importacionesOk) {
            // Si una importación falla, no tiene sentido continuar.
            return errores;
        }
        // 2. VARIABLES GLOBALES
        for (NodoSentencia s : programa.variablesGlobales()) {
            procesarSentenciaGlobal(s);
        }
        // 3. CUERPO DEL MAIOR
        procesarBloque(programa.cuerpoMain());
        return errores;
    }

    // VARIABLES GLOBALES
    /** Procesa una declaración global (variable, arreglo o struct). */
    private void procesarSentenciaGlobal(NodoSentencia s) {
        switch (s.tipoNodo()) {
            case DECLARACION_VARIABLE -> {
                NodoSentencia.DeclaracionVariable d = (NodoSentencia.DeclaracionVariable) s;

                if (d.tipo() != null) {
                    alcance.resolverTipo(d.tipo(), d.linea(), d.columna());
                }

                alcance.resolverExpresion(d.inicializacion());
                tipos.validarInicializacion(d);

                if (esInstanciaDeObjeto(d)) {
                    declaraciones.declararObjeto(d);
                } else {
                    declaraciones.declararVariable(d);
                }
            }
            case DECLARACION_ARREGLO -> {
                NodoSentencia.DeclaracionArreglo d = (NodoSentencia.DeclaracionArreglo) s;
                alcance.resolverTipo(d.tipo(), d.linea(), d.columna());
                for (NodoExpr e : d.inicializacion()) alcance.resolverExpresion(e);
                tipos.validarInicializacionArreglo(d);
                declaraciones.declararArreglo(d);
            }
            case DECLARACION_STRUCT -> {
                NodoSentencia.DeclaracionStruct d = (NodoSentencia.DeclaracionStruct) s;
                alcance.resolverTipo(d.tipo(), d.linea(), d.columna());
                for (NodoExpr e : d.inicializacion()) alcance.resolverExpresion(e);
                estructuras.validarInicializacion(d);
                declaraciones.declararStruct(d);
            }
            default -> {
            }
        }
    }

    // BLOQUES Y SENTENCIAS
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
                if (d.tipo() != null) {
                    alcance.resolverTipo(d.tipo(), d.linea(), d.columna());
                }
                alcance.resolverExpresion(d.inicializacion());
                tipos.validarInicializacion(d);
                if (esInstanciaDeObjeto(d)) {
                    declaraciones.declararObjeto(d);
                } else {
                    declaraciones.declararVariable(d);
                }
            }
            case DECLARACION_ARREGLO -> {
                NodoSentencia.DeclaracionArreglo d = (NodoSentencia.DeclaracionArreglo) s;
                alcance.resolverTipo(d.tipo(), d.linea(), d.columna());
                for (NodoExpr e : d.inicializacion()) alcance.resolverExpresion(e);
                tipos.validarInicializacionArreglo(d);
                declaraciones.declararArreglo(d);
            }
            case DECLARACION_STRUCT -> {
                NodoSentencia.DeclaracionStruct d = (NodoSentencia.DeclaracionStruct) s;
                alcance.resolverTipo(d.tipo(), d.linea(), d.columna());
                for (NodoExpr e : d.inicializacion()) alcance.resolverExpresion(e);
                estructuras.validarInicializacion(d);
                declaraciones.declararStruct(d);
            }
            case ASIGNACION -> {
                NodoSentencia.Asignacion a = (NodoSentencia.Asignacion) s;
                alcance.resolverExpresion(a.destino());
                alcance.resolverExpresion(a.valor());
                asignaciones.validarDestino(a);
                tipos.validarAsignacion(a.destino(), a.valor());
            }
            case INCREMENTO_DECREMENTO -> {
                NodoSentencia.IncrementoDecremento inc = (NodoSentencia.IncrementoDecremento) s;
                alcance.resolverExpresion(inc.operando());
                tipos.tipoDeExpresion(inc.operando());
            }
            case CONDICIONAL -> procesarCondicional((NodoSentencia.Condicional) s);
            case CICLO_DUM -> procesarCicloDum((NodoSentencia.CicloDum) s);
            case CICLO_FACERE -> procesarCicloFacere((NodoSentencia.CicloFacere) s);
            case CICLO_PER -> procesarCicloPer((NodoSentencia.CicloPer) s);
            case LECTURA -> {
                NodoSentencia.Lectura l = (NodoSentencia.Lectura) s;
                if (l.variable() != null) {
                    alcance.resolverNombre(l.variable(), l.linea(), l.columna());
                }
            }
            case ESCRITURA -> {
                NodoSentencia.Escritura e = (NodoSentencia.Escritura) s;
                for (NodoExpr v : e.valores()) {
                    alcance.resolverExpresion(v);
                    tipos.tipoDeExpresion(v);
                }
            }
            case INTERRUPCION_CICLO -> {
                flujo.validarInterrupcion((NodoSentencia.InterrupcionCiclo) s);
            }
            case LLAMADA_FUNCION_SENTENCIA -> {
                NodoSentencia.LlamadaFuncionSentencia l = (NodoSentencia.LlamadaFuncionSentencia) s;
                alcance.resolverExpresion(l.llamada());
                tipos.tipoDeExpresion(l.llamada());
            }
            case LLAMADA_METODO_SENTENCIA -> {
                NodoSentencia.LlamadaMetodoSentencia l = (NodoSentencia.LlamadaMetodoSentencia) s;
                alcance.resolverExpresion(l.llamada());
                objetos.validarLlamadaMetodo(l.llamada());
            }
        }
    }

    // CONDICIONAL
    /** Procesa un 'si / aliter / aliter-final'. */
    private void procesarCondicional(NodoSentencia.Condicional c) {
        alcance.resolverExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
        tabla.entrarScope("si");
        procesarBloque(c.cuerpoSi());
        tabla.salirScope();

        for (NodoSentencia.RamaAliter r : c.ramasAliter()) {
            alcance.resolverExpresion(r.condicion());
            tipos.validarCondicionBooleana(r.condicion());

            tabla.entrarScope("aliter");
            procesarBloque(r.cuerpo());
            tabla.salirScope();
        }
        if (c.cuerpoAliter() != null) {
            tabla.entrarScope("aliter-final");
            procesarBloque(c.cuerpoAliter());
            tabla.salirScope();
        }
    }

    // CICLOS
    /** Procesa un ciclo 'dum'. */
    private void procesarCicloDum(NodoSentencia.CicloDum c) {
        alcance.resolverExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());

        tabla.entrarScope("dum");
        flujo.entrarCiclo();
        procesarBloque(c.cuerpo());
        flujo.salirCiclo();
        tabla.salirScope();
    }

    /** Procesa un ciclo 'facere ... dum'. */
    private void procesarCicloFacere(NodoSentencia.CicloFacere c) {
        tabla.entrarScope("facere");
        flujo.entrarCiclo();
        procesarBloque(c.cuerpo());
        flujo.salirCiclo();
        tabla.salirScope();
        alcance.resolverExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
    }

    /** Procesa un ciclo 'per'. */
    private void procesarCicloPer(NodoSentencia.CicloPer c) {
        tabla.entrarScope("per");

        if (c.inicializacion() != null) {
            procesarSentencia(c.inicializacion());
        }
        alcance.resolverExpresion(c.condicion());
        tipos.validarCondicionBooleana(c.condicion());
        flujo.entrarCiclo();
        procesarBloque(c.cuerpo());
        if (c.actualizacion() != null) {
            procesarSentencia(c.actualizacion());
        }
        flujo.salirCiclo();

        tabla.salirScope();
    }

    // HELPERS
    /** True si la declaración inicializa con 'novus'. */
    private boolean esInstanciaDeObjeto(NodoSentencia.DeclaracionVariable d) {
        return d.inicializacion() instanceof NodoExpr.InstanciaObjeto;
    }

    public List<ErrorSemantico> getErrores() { return errores; }

    public TablaSimbolosPig getTabla() { return tabla; }

    public ValidadorImportacionesPig getImportaciones() { return importaciones;}
}