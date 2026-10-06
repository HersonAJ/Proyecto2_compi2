package com.example.contacto_3xtrat3r3str3.y.semantica;

import com.example.contacto_3xtrat3r3str3.y.ast.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;

/**
 * Valida reglas de flujo de control en .y.
 *
 * Validaciones:
 *   - 'romper' fuera de un ciclo                -> validarRomper
 *   - 'continuar' fuera de un ciclo             -> validarContinuar
 *   - Código inalcanzable tras romper/continuar -> validarCodigoInalcanzable
 *   - Función con retorno no garantizado        -> validarRetornoGarantizado
 */
public class ValidadorFlujo {

    private final List<ErrorSemantico> errores;
    private int nivelCiclo = 0;

    public ValidadorFlujo(List<ErrorSemantico> errores) {
        this.errores = errores;
    }

    // CICLOS
    public void entrarCiclo() {
        nivelCiclo++;
    }

    public void salirCiclo() {
        nivelCiclo--;
    }

    /** Verifica que 'romper' esté dentro de un ciclo. */
    public void validarRomper(NodoSentencia.Romper r) {
        if (nivelCiclo == 0) {
            errores.add(new ErrorSemantico(r.linea(), r.columna(),
                    "Corrupción de flujo",
                    "'romper' usado fuera de un ciclo"));
        }
    }

    /** Verifica que 'continuar' esté dentro de un ciclo. */
    public void validarContinuar(NodoSentencia.Continuar c) {
        if (nivelCiclo == 0) {
            errores.add(new ErrorSemantico(c.linea(), c.columna(),
                    "Corrupción de flujo",
                    "'continuar' usado fuera de un ciclo"));
        }
    }

    // CODIGO INALCANZABLE
    /**
     * Reporta instrucciones inalcanzables (después de un romper/continuar válido).
     * Si el romper/continuar está fuera de un ciclo, no se reporta código
     * inalcanzable porque ese romper/continuar ya genera su propio error.
     */
    public void validarCodigoInalcanzable(List<NodoSentencia> bloque) {
        if (bloque == null) return;

        boolean alcanzable = true;

        for (NodoSentencia s : bloque) {
            if (!alcanzable) {
                errores.add(new ErrorSemantico(s.linea(), s.columna(),
                        "Código inalcanzable",
                        "Esta instrucción nunca se ejecutará porque está después de 'romper' o 'continuar'"));
                break;
            }

            // Solo un romper/continuar DENTRO de un ciclo corta el flujo.
            if ((s instanceof NodoSentencia.Romper || s instanceof NodoSentencia.Continuar)
                    && nivelCiclo > 0) {
                alcanzable = false;
            }
        }
    }

    // RETORNO GARANTIZADO
    /**
     * Verifica que una función con tipo de retorno declarado tenga
     * un 'retornar' garantizado en todos los caminos posibles.
     */
    public void validarRetornoGarantizado(String tipoRetorno,
                                          List<NodoSentencia> cuerpo,
                                          int lineaFuncion,
                                          int columnaFuncion) {
        if (tipoRetorno == null) return;

        if (cuerpo == null || cuerpo.isEmpty()) {
            errores.add(new ErrorSemantico(lineaFuncion, columnaFuncion,
                    "Falta de retorno",
                    "La función declara retorno de tipo '" + tipoRetorno + "' pero no tiene instrucciones"));
            return;
        }

        boolean garantiza = bloqueGarantizaRetorno(cuerpo);

        if (!garantiza) {
            errores.add(new ErrorSemantico(lineaFuncion, columnaFuncion,
                    "Falta de retorno",
                    "La función declara retorno de tipo '" + tipoRetorno
                            + "' pero no todos los caminos terminan con 'retornar'"));
        }
    }

    /** Devuelve true si un bloque garantiza que siempre se retorna. */
    private boolean bloqueGarantizaRetorno(List<NodoSentencia> bloque) {
        if (bloque == null || bloque.isEmpty()) return false;
        for (NodoSentencia s : bloque) {
            if (instruccionGarantizaRetorno(s)) {
                return true;
            }
        }
        return false;
    }

    /** Devuelve true si una instrucción garantiza que se retorna en todos los caminos. */
    private boolean instruccionGarantizaRetorno(NodoSentencia s) {
        // Caso 1: retorno directo.
        if (s instanceof NodoSentencia.Retorno) {
            return true;
        }

        // Caso 2: condicional con rama 'contrario' donde todas las ramas retornan.
        if (s instanceof NodoSentencia.Condicional c) {
            boolean siRetorna = bloqueGarantizaRetorno(c.cuerpoSi());

            // 'contrario' es el equivalente al else. Si no hay, no se garantiza.
            if (c.cuerpoContrario() == null) {
                return false;
            }
            boolean contrarioRetorna = bloqueGarantizaRetorno(c.cuerpoContrario());

            // Si hay 'sino' intermedio, también debe retornar.
            boolean sinoRetorna = true;
            if (c.cuerpoSino() != null) {
                sinoRetorna = bloqueGarantizaRetorno(c.cuerpoSino());
            }

            return siRetorna && contrarioRetorna && sinoRetorna;
        }
        // Cualquier otra instrucción no garantiza retorno.
        return false;
    }
}