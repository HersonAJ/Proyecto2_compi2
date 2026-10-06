package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;
import java.util.Optional;

/**
 * Valida reglas específicas de asignaciones en PigLatin.
 *
 * Validaciones:
 *   - Destino no es asignable                 -> validarDestino
 *   - Asignación a función importada          -> validarIdentificadorComoDestino
 *   - Asignación a clase importada            -> validarIdentificadorComoDestino
 *   - Asignación a estructura importada       -> validarIdentificadorComoDestino
 *   - Asignación a arreglo completo           -> validarIdentificadorComoDestino
 */
public class ValidadorAsignacionesPig {

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorAsignacionesPig(TablaSimbolosPig tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    /** Valida que el destino de una asignación sea asignable. */
    public void validarDestino(NodoSentencia.Asignacion asignacion) {
        NodoExpr destino = asignacion.destino();
        // Caso 1: destino es un identificador simple (variable).
        if (destino instanceof NodoExpr.Identificador id) {
            validarIdentificadorComoDestino(id);
        }
        // Caso 2: destino es AccesoArray (arreglo[índice]) -> válido.
        // Caso 3: destino es AccesoAtributo (objeto.atributo) -> válido.
        // Caso 4: cualquier otra expresión (literal, binaria, etc.) -> inválido.
        if (!esDestinoValido(destino)) {
            errores.add(new ErrorSemantico(destino.linea(), destino.columna(),
                    "Destino inválido",
                    "El lado izquierdo de una asignación debe ser una variable, "
                            + "un elemento de arreglo o un atributo"));
        }
    }

    /** Verifica que un identificador simple pueda ser destino de asignación. */
    private void validarIdentificadorComoDestino(NodoExpr.Identificador id) {
        String nombre = id.nombre();

        // 1. Si es una función importada, no se puede asignar.
        if (tabla.buscarFuncion(nombre).isPresent()) {
            errores.add(new ErrorSemantico(id.linea(), id.columna(),
                    "Asignación inválida",
                    "'" + nombre + "' es una función y no puede ser destino de asignación"));
            return;
        }

        // 2. Si es una clase importada, no se puede asignar.
        if (tabla.buscarClase(nombre).isPresent()) {
            errores.add(new ErrorSemantico(id.linea(), id.columna(),
                    "Asignación inválida",
                    "'" + nombre + "' es una clase y no puede ser destino de asignación"));
            return;
        }

        // 3. Si es una estructura importada, no se puede asignar.
        if (tabla.buscarEstructura(nombre).isPresent()) {
            errores.add(new ErrorSemantico(id.linea(), id.columna(),
                    "Asignación inválida",
                    "'" + nombre + "' es una estructura y no puede ser destino de asignación"));
            return;
        }

        // 4. Si es una variable, revisar que no sea un arreglo.
        Optional<TablaSimbolosPig.SimboloVariable> simbolo = tabla.buscarVariable(nombre);
        if (simbolo.isEmpty()) {
            return;
        }

        TablaSimbolosPig.SimboloVariable v = simbolo.get();

        if (v.dimensiones() > 0) {
            errores.add(new ErrorSemantico(id.linea(), id.columna(),
                    "Asignación inválida",
                    "No se puede asignar a '" + nombre + "' porque es un arreglo. "
                            + "Use un índice: '" + nombre + "[indice] = ...'"));
        }
    }

    /** True si la expresión puede recibir una asignación (variable, arreglo o atributo). */
    private boolean esDestinoValido(NodoExpr expr) {
        if (expr == null) return false;
        return switch (expr.tipoNodo()) {
            case IDENTIFICADOR, ACCESO_ARRAY, ACCESO_ATRIBUTO -> true;
            default -> false;
        };
    }
}