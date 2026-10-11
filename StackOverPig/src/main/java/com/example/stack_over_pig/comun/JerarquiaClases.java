package com.example.stack_over_pig.comun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Responde preguntas sobre la herencia de clases (cadena de ancestros, subtipo, ciclos)
 * sin saber nada de como se guardan las clases: quien la use le da una funcion que dice
 * cual es la superclase de una clase (o null si no hereda de ninguna).
 *
 * Zetariano y Pig Latin lo reutilizan, cada uno con su propia tabla de simbolos.
 */
public final class JerarquiaClases {

    private final Function<String, String> superclaseDe;

    public JerarquiaClases(Function<String, String> superclaseDe) {
        this.superclaseDe = superclaseDe;
    }

    /**
     * La clase y sus ancestros, de la mas derivada a la raiz.
     * Si la herencia es circular se detiene al volver a una clase ya vista, asi que nunca se cuelga.
     */
    public List<String> cadena(String clase) {
        List<String> resultado = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        String actual = clase;
        while (actual != null && vistas.add(actual)) {
            resultado.add(actual);
            actual = superclaseDe.apply(actual);
        }
        return resultado;
    }

    /** true si 'sub' es la misma clase que 'sup' o hereda de ella (directa o indirectamente). */
    public boolean esSubtipo(String sub, String sup) {
        if (sub == null || sup == null) return false;
        return cadena(sub).contains(sup);
    }

    /** true si al subir por las superclases de 'clase' se vuelve a pasar por una clase ya vista. */
    public boolean tieneCiclo(String clase) {
        Set<String> vistas = new HashSet<>();
        String actual = clase;
        while (actual != null) {
            if (!vistas.add(actual)) return true;
            actual = superclaseDe.apply(actual);
        }
        return false;
    }
}