package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;

/**
 * Declara símbolos en la tabla para .pig.
 *
 * Validaciones:
 *   - Variable duplicada            -> declararVariable
 *   - Arreglo duplicado             -> declararArreglo
 *   - Estructura duplicada          -> declararStruct
 *   - Objeto duplicado              -> declararObjeto
 */
public class ValidadorDeclaracionesPig {

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorDeclaracionesPig(TablaSimbolosPig tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    /** Declara una variable simple. */
    public void declararVariable(NodoSentencia.DeclaracionVariable d) {
        boolean ok = tabla.declararVariable(d.nombre(), d.tipo(), 0, null, false, false, d.tipo());
        if (!ok) {
            reportarDuplicada(d.linea(), d.columna(), d.nombre());
        }
    }

    /** Declara un arreglo (detecta si el tipo es estructura o clase). */
    public void declararArreglo(NodoSentencia.DeclaracionArreglo d) {
        boolean esEstructura = tabla.buscarEstructura(d.tipo()).isPresent();
        boolean esObjeto = tabla.buscarClase(d.tipo()).isPresent();

        boolean ok = tabla.declararVariable(
                d.nombre(), d.tipo(), 1, d.tamano(),
                esEstructura, esObjeto, d.tipo()
        );

        if (!ok) {
            reportarDuplicada(d.linea(), d.columna(), d.nombre());
        }
    }

    /** Declara una variable de tipo estructura. */
    public void declararStruct(NodoSentencia.DeclaracionStruct d) {
        boolean ok = tabla.declararVariable(d.nombre(), d.tipo(), 0, null, true, false, d.tipo());
        if (!ok) {
            reportarDuplicada(d.linea(), d.columna(), d.nombre());
        }
    }

    /** Declara una variable de tipo objeto (creada con 'novus'). */
    public void declararObjeto(NodoSentencia.DeclaracionVariable d) {
        String tipo = d.tipo();
        // Si el tipo es null, intentar extraerlo del inicializador.
        if (tipo == null && d.inicializacion() instanceof NodoExpr.InstanciaObjeto inst) {
            tipo = inst.tipoClase();
        }
        boolean ok = tabla.declararVariable(
                d.nombre(), tipo, 0, null, false, true, tipo
        );

        if (!ok) {
            reportarDuplicada(d.linea(), d.columna(), d.nombre());
        }
    }

    /** Reporta error de declaración duplicada. */
    private void reportarDuplicada(int linea, int columna, String nombre) {
        errores.add(new ErrorSemantico(linea, columna, "Declaracion duplicada",
                "'" + nombre + "' ya fue declarado en este ambito"));
    }
}