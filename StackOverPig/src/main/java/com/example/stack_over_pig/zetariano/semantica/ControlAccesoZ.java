package com.example.stack_over_pig.zetariano.semantica;

import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;
import com.example.stack_over_pig.zetariano.nodo.Visibilidad;

import java.util.List;

/**
 * Decide si el codigo de la clase que se esta analizando puede usar un miembro
 * (atributo, metodo o constructor) y reporta el error cuando no.
 *
 * Reglas:
 *   public     -> cualquiera.
 *   private    -> solo dentro de la clase que lo declara.
 *   protected  -> mismo paquete, o una subclase de la clase que lo declara.
 *   (ninguno)  -> solo dentro del mismo paquete.
 *
 * Simplificacion: en 'protected' no se exige que el objeto por el que se accede sea del
 * tipo de la subclase (Java si lo exige cuando el paquete es distinto).
 */
public class ControlAccesoZ {

    private final TablaSimbolosZ tabla;
    private final List<ErrorSemantico> errores;

    public ControlAccesoZ(TablaSimbolosZ tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    /** true si la clase actual puede usar un miembro con esa visibilidad declarado en 'claseDeclarante'. */
    public boolean esAccesible(Visibilidad visibilidad, String claseDeclarante) {
        if (visibilidad == null || claseDeclarante == null) return true;
        String actual = tabla.getNombreClase();
        return switch (visibilidad) {
            case PUBLICO -> true;
            case PAQUETE -> true;      // sin paquetes: cualquier clase del proyecto
            case PROTEGIDO -> claseDeclarante.equals(actual) || tabla.esSubtipo(actual, claseDeclarante);
            case PRIVADO -> claseDeclarante.equals(actual);
        };
    }

    /**
     * Comprueba el acceso; si no es accesible registra el error y devuelve false.
     * 'tipoMiembro' es "El atributo", "El método" o "El constructor".
     */
    public boolean verificar(Visibilidad visibilidad, String claseDeclarante, String miembro,
                             String tipoMiembro, int linea, int columna) {
        if (esAccesible(visibilidad, claseDeclarante)) return true;
        errores.add(new ErrorSemantico(linea, columna, "Acceso no permitido",
                tipoMiembro + " '" + miembro + "' es " + descripcion(visibilidad)
                        + " en la clase '" + claseDeclarante
                        + "' y no se puede usar desde '" + tabla.getNombreClase() + "'"));
        return false;
    }
/*
    private boolean mismoPaquete(String clase) {
        return tabla.paqueteDe(clase).equals(tabla.getPaqueteActual());
    }
*/
    private String descripcion(Visibilidad visibilidad) {
        return switch (visibilidad) {
            case PUBLICO -> "public";
            case PRIVADO -> "private";
            case PROTEGIDO -> "protected";
            case PAQUETE -> "de paquete (sin modificador)";
        };
    }
}
