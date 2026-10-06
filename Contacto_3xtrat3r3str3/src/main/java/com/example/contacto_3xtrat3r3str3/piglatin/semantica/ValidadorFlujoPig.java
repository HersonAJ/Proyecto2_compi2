package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;

/**
 * Valida reglas de flujo de control en PigLatin.
 *
 * Validaciones:
 *   - 'perge' / 'interrumpe' fuera de un ciclo   -> validarInterrupcion
 *   - Código inalcanzable tras interrupción      -> validarCodigoInalcanzable
 */
public class ValidadorFlujoPig {

    private final List<ErrorSemantico> errores;
    private int nivelCiclo = 0;

    public ValidadorFlujoPig(List<ErrorSemantico> errores) {
        this.errores = errores;
    }

    // CICLOS
    public void entrarCiclo() { nivelCiclo++; }
    public void salirCiclo() { nivelCiclo--; }

    // INTERRUPCIONES
    /** Verifica que 'perge' o 'interrumpe' estén dentro de un ciclo. */
    public void validarInterrupcion(NodoSentencia.InterrupcionCiclo i) {
        if (nivelCiclo == 0) {
            String nombre = i.tipo();
            errores.add(new ErrorSemantico(i.linea(), i.columna(),
                    "Flujo inválido",
                    "'" + nombre + "' usado fuera de un ciclo"));
        }
    }

    // CODIGO INALCANZABLE
    /**
     * Reporta instrucciones inalcanzables después de un 'perge' o 'interrumpe' válido.
     * Si la interrupción está fuera de un ciclo, no se reporta código inalcanzable
     * porque esa interrupción ya generó su propio error.
     */
    public void validarCodigoInalcanzable(List<NodoSentencia> bloque) {
        if (bloque == null) return;

        boolean alcanzable = true;

        for (NodoSentencia s : bloque) {
            if (!alcanzable) {
                errores.add(new ErrorSemantico(s.linea(), s.columna(),
                        "Código inalcanzable",
                        "Esta instrucción nunca se ejecutará porque está después de 'perge' o 'interrumpe'"));
                break;
            }

            if (s instanceof NodoSentencia.InterrupcionCiclo && nivelCiclo > 0) {
                alcanzable = false;
            }
        }
    }
}