package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;
import java.util.Optional;

/**
 * Valida tipos e infiere expresiones en .pig.
 *
 * Validaciones:
 *   - Índice de arreglo no numerus       -> tipoDeExpresion (ACCESO_ARRAY)
 *   - Indexar algo que no es arreglo     -> tipoDeExpresion (ACCESO_ARRAY)
 *   - Tipos incompatibles en binaria     -> tipoResultanteBinaria
 *   - Tipos incompatibles en ++/--       -> tipoDeExpresion (INCREMENTO_DECREMENTO)
 *   - Inicialización incompatible        -> validarInicializacion
 *   - Inicialización de arreglo inválida -> validarInicializacionArreglo
 *   - Asignación incompatible            -> validarAsignacion
 *   - Condición no booleana              -> validarCondicionBooleana
 */
public class ValidadorTiposPig {

    public record TipoResuelto(String base, int dimensiones) {
        public boolean esArreglo() { return dimensiones > 0; }
    }

    private static final String NUMERUS = "numerus";
    private static final String TEXTUM = "textum";
    private static final String DECIMALIS = "decimalis";
    private static final String LITTERA = "littera";
    private static final String BOOL = "bool";

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorTiposPig(TablaSimbolosPig tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    // INFERENCIA DE TIPOS
    /** Devuelve el tipo de una expresión. */
    public TipoResuelto tipoDeExpresion(NodoExpr expr) {
        if (expr == null) return null;

        return switch (expr.tipoNodo()) {
            case LITERAL_ENTERO -> new TipoResuelto(NUMERUS, 0);
            case UNARIA -> {
                NodoExpr.Unaria un = (NodoExpr.Unaria) expr;
                TipoResuelto operando = tipoDeExpresion(un.operando());
                if (operando != null && !esNumerico(operando.base())) {
                    errores.add(new ErrorSemantico(un.linea(), un.columna(),
                            "Tipo incompatible",
                            "'-' unario requiere tipo numérico, se encontró '" + operando.base() + "'"));
                    yield null;
                }
                yield operando;
            }
            case LITERAL_DECIMAL -> new TipoResuelto(DECIMALIS, 0);
            case LITERAL_TEXTO -> new TipoResuelto(TEXTUM, 0);
            case LITERAL_CARACTER -> new TipoResuelto(LITTERA, 0);
            case LITERAL_BOOL -> new TipoResuelto(BOOL, 0);

            case IDENTIFICADOR -> {
                NodoExpr.Identificador id = (NodoExpr.Identificador) expr;
                Optional<TablaSimbolosPig.SimboloVariable> simbolo = tabla.buscarVariable(id.nombre());
                yield simbolo.map(s -> new TipoResuelto(s.tipo(), s.dimensiones())).orElse(null);
            }

            case ACCESO_ARRAY -> {
                NodoExpr.AccesoArray a = (NodoExpr.AccesoArray) expr;
                TipoResuelto tipoIndice = tipoDeExpresion(a.indice());
                if (tipoIndice != null && !NUMERUS.equals(tipoIndice.base())) {
                    errores.add(new ErrorSemantico(a.linea(), a.columna(),
                            "Tipo incompatible",
                            "El índice de un arreglo debe ser 'numerus', se encontró '" + tipoIndice.base() + "'"));
                }

                TipoResuelto tipoArreglo = tipoDeExpresion(a.arreglo());
                if (tipoArreglo == null) yield null;
                if (tipoArreglo.dimensiones() == 0) {
                    errores.add(new ErrorSemantico(a.linea(), a.columna(),
                            "Tipo incompatible",
                            "No se puede indexar un valor que no es arreglo"));
                    yield null;
                }
                yield new TipoResuelto(tipoArreglo.base(), tipoArreglo.dimensiones() - 1);
            }

            case ACCESO_ATRIBUTO -> {
                yield null;
            }
            case BINARIA -> {
                NodoExpr.Binaria b = (NodoExpr.Binaria) expr;
                TipoResuelto izq = tipoDeExpresion(b.izquierda());
                TipoResuelto der = tipoDeExpresion(b.derecha());
                yield tipoResultanteBinaria(b.operador(), izq, der, b.linea(), b.columna());
            }
            case INCREMENTO_DECREMENTO -> {
                NodoExpr.IncrementoDecremento inc = (NodoExpr.IncrementoDecremento) expr;
                TipoResuelto operando = tipoDeExpresion(inc.operando());
                if (operando != null && !esNumerico(operando.base())) {
                    errores.add(new ErrorSemantico(inc.linea(), inc.columna(),
                            "Tipo incompatible",
                            "'" + inc.operador() + "' requiere tipo numérico"));
                    yield null;
                }
                yield operando;
            }
            case LLAMADA_FUNCION -> {
                NodoExpr.LlamadaFuncion l = (NodoExpr.LlamadaFuncion) expr;
                Optional<TablaSimbolosPig.DefinicionFuncion> def = tabla.buscarFuncion(l.nombre());
                if (def.isEmpty()) yield null;
                yield new TipoResuelto(def.get().tipoRetorno(), 0);
            }
            case LLAMADA_METODO -> {
                yield null;
            }
            case INSTANCIA_OBJETO -> {
                NodoExpr.InstanciaObjeto inst = (NodoExpr.InstanciaObjeto) expr;
                yield new TipoResuelto(inst.tipoClase(), 0);
            }
            case LISTA_LITERAL -> {
                yield null;
            }
        };
    }

    /** Calcula el tipo resultado de una operación binaria y reporta incompatibles. */
    private TipoResuelto tipoResultanteBinaria(String operador, TipoResuelto izq,
                                               TipoResuelto der, int linea, int columna) {
        if (izq == null || der == null) return null;

        return switch (operador) {
            case "+" -> {
                if (TEXTUM.equals(izq.base()) || TEXTUM.equals(der.base())) {
                    yield new TipoResuelto(TEXTUM, 0);
                }
                if (esNumerico(izq.base()) && esNumerico(der.base())) {
                    yield promocionNumerica(izq, der);
                }
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "-", "*", "/" -> {
                if (esNumerico(izq.base()) && esNumerico(der.base())) {
                    yield promocionNumerica(izq, der);
                }
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "<", ">", "<=", ">=" -> {
                if (esNumerico(izq.base()) && esNumerico(der.base())) {
                    yield new TipoResuelto(BOOL, 0);
                }
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "==", "!=" -> {
                boolean compatibles = izq.base().equals(der.base())
                        || (esNumerico(izq.base()) && esNumerico(der.base()));
                if (compatibles) yield new TipoResuelto(BOOL, 0);
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "&&", "||" -> {
                if (BOOL.equals(izq.base()) && BOOL.equals(der.base())) {
                    yield new TipoResuelto(BOOL, 0);
                }
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            default -> null;
        };
    }

    /** Promoción numérica: decimalis gana sobre numerus. */
    private TipoResuelto promocionNumerica(TipoResuelto izq, TipoResuelto der) {
        return (DECIMALIS.equals(izq.base()) || DECIMALIS.equals(der.base()))
                ? new TipoResuelto(DECIMALIS, 0)
                : new TipoResuelto(NUMERUS, 0);
    }

    /** Reporta tipos incompatibles en una operación binaria. */
    private void reportarIncompatible(String operador, TipoResuelto izq, TipoResuelto der,
                                      int linea, int columna) {
        errores.add(new ErrorSemantico(linea, columna,
                "Tipo incompatible",
                "El operador '" + operador + "' no admite '" + izq.base() + "' y '" + der.base() + "'"));
    }

    // COMPATIBILIDAD
    /** True si un valor de tipo 'origen' puede asignarse a un destino. */
    public boolean esAsignable(TipoResuelto destino, TipoResuelto origen) {
        if (destino.dimensiones() != origen.dimensiones()) return false;
        if (destino.dimensiones() > 0) {
            return sonTiposEquivalentes(destino.base(), origen.base());
        }
        if (sonTiposEquivalentes(destino.base(), origen.base())) return true;
        return DECIMALIS.equals(destino.base()) && NUMERUS.equals(origen.base());
    }

    /** True si dos tipos son equivalentes entre lenguajes (numerus ↔ int ↔ entero). */
    public boolean sonTiposEquivalentes(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        return mapearTipo(a).equals(mapearTipo(b));
    }

    /** Normaliza un tipo a su forma canónica interna. */
    public String mapearTipo(String tipo) {
        if (tipo == null) return null;
        return switch (tipo) {
            case "numerus", "entero", "int" -> "NUMERICO_ENTERO";
            case "decimalis", "flotante", "double" -> "NUMERICO_DECIMAL";
            case "textum", "cadena", "String" -> "TEXTO";
            case "littera", "caracter", "char" -> "CARACTER";
            case "bool", "boolean" -> "BOOLEANO";
            default -> tipo; // estructuras, clases, etc.
        };
    }

    // VALIDACIONES
    /** Valida que una inicialización sea compatible con el tipo declarado. */
    public void validarInicializacion(NodoSentencia.DeclaracionVariable d) {
        if (d.inicializacion() == null) return;

        TipoResuelto tipoDestino = new TipoResuelto(d.tipo(), 0);
        TipoResuelto tipoValor = tipoDeExpresion(d.inicializacion());
        if (tipoValor == null) return;

        if (!esAsignable(tipoDestino, tipoValor)) {
            errores.add(new ErrorSemantico(d.inicializacion().linea(), d.inicializacion().columna(),
                    "Tipo incompatible",
                    "No se puede asignar '" + tipoValor.base() + "' a '" + d.tipo() + "'"));
        }
    }

    /** Valida que los valores de un arreglo coincidan con su tipo base. */
    public void validarInicializacionArreglo(NodoSentencia.DeclaracionArreglo d) {
        if (d.inicializacion() == null || d.inicializacion().isEmpty()) return;

        TipoResuelto tipoElemento = new TipoResuelto(d.tipo(), 0);
        for (NodoExpr e : d.inicializacion()) {
            TipoResuelto tipoValor = tipoDeExpresion(e);
            if (tipoValor == null) continue;
            if (!esAsignable(tipoElemento, tipoValor)) {
                errores.add(new ErrorSemantico(e.linea(), e.columna(),
                        "Tipo incompatible",
                        "No se puede colocar '" + tipoValor.base() + "' en un arreglo de '" + d.tipo() + "'"));
            }
        }
    }

    /** Valida que el valor de una asignación sea compatible con el destino. */
    public void validarAsignacion(NodoExpr destino, NodoExpr valor) {
        TipoResuelto tipoDestino = tipoDeExpresion(destino);
        TipoResuelto tipoValor = tipoDeExpresion(valor);
        if (tipoDestino == null || tipoValor == null) return;

        if (!esAsignable(tipoDestino, tipoValor)) {
            errores.add(new ErrorSemantico(valor.linea(), valor.columna(),
                    "Tipo incompatible",
                    "No se puede asignar '" + tipoValor.base() + "' a '" + tipoDestino.base() + "'"));
        }
    }

    /** Valida que una condición sea de tipo bool. */
    public void validarCondicionBooleana(NodoExpr condicion) {
        TipoResuelto tipo = tipoDeExpresion(condicion);
        if (tipo != null && !BOOL.equals(tipo.base())) {
            errores.add(new ErrorSemantico(condicion.linea(), condicion.columna(),
                    "Flujo inválido",
                    "La condición debe ser 'bool', se encontró '" + tipo.base() + "'"));
        }
    }

    // HELPERS
    /** True si el tipo es numérico en cualquier nomenclatura. */
    private boolean esNumerico(String tipo) {
        return NUMERUS.equals(tipo) || DECIMALIS.equals(tipo)
                || "entero".equals(tipo) || "flotante".equals(tipo)
                || "int".equals(tipo) || "double".equals(tipo);
    }
}