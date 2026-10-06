package com.example.contacto_3xtrat3r3str3.zetariano.semantica;

import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.NodoSentencia;

import java.util.List;
import java.util.Optional;

/**
 * Validación de tipos e inferencia de expresiones en .z.
 *
 * Validaciones:
 *   - Índice de arreglo no entero          -> tipoDeExpresion (ACCESO_ARRAY)
 *   - Índice fuera de rango                -> tipoDeExpresion (ACCESO_ARRAY)
 *   - Indexar algo que no es arreglo       -> tipoDeExpresion (ACCESO_ARRAY)
 *   - Acceso inválido a atributo/metodo    -> tipoDeExpresion (ACCESO_ATRIBUTO / LLAMADA_METODO)
 *   - Atributo no declarado                -> tipoDeExpresion (ACCESO_ATRIBUTO)
 *   - Tipos incompatibles en binaria       -> tipoResultanteBinaria
 *   - División/módulo por cero             -> tipoResultanteBinaria
 *   - Tipos incompatibles en unaria        -> tipoDeExpresion (UNARIA)
 *   - Operador ++/-- sobre no lvalue       -> tipoDeExpresion (INCREMENTO_DECREMENTO)
 *   - Condición de ternario no booleana    -> tipoDeExpresion (TERNARIA)
 *   - Ramas del ternario incompatibles     -> tipoDeExpresion (TERNARIA)
 *   - Argumentos incompatibles en llamada  -> resolverSobrecarga
 *   - Llamada ambigua por sobrecarga       -> resolverSobrecarga
 *   - Inicialización incompatible          -> validarInicializacion
 *   - Asignación incompatible              -> validarAsignacion
 *   - Destino de asignación inválido       -> validarAsignacion
 *   - Condición no booleana                -> validarCondicionBooleana
 *   - Retorno incompatible                 -> validarRetorno
 *   - Tipo no permitido en switch          -> validarTipoSwitch
 */
public class ValidadorTiposZ {

    public record TipoResuelto(String base, int dimensiones) {
        public boolean esArreglo() { return dimensiones > 0; }
    }

    private static final String INT = "int";
    private static final String DOUBLE = "double";
    private static final String CHAR = "char";
    private static final String BOOLEAN = "boolean";
    private static final String STRING = "String";
    private static final String NULO = "null";

    private final TablaSimbolosZ tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorTiposZ(TablaSimbolosZ tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    private boolean esPrimitivo(String tipo) { //True si el tipo es un primitivo (sin contar String)
        return INT.equals(tipo) || DOUBLE.equals(tipo) || CHAR.equals(tipo) || BOOLEAN.equals(tipo);
    }

    private boolean esNumerico(String tipo) {
        return INT.equals(tipo) || DOUBLE.equals(tipo);
    }

    // CALCULO DE TIPO DE UNA EXPRESION
    /** Devuelve el tipo de una expresión (base + dimensiones). */
    public TipoResuelto tipoDeExpresion(NodoExpr expr) {
        if (expr == null) return null;

        return switch (expr.tipoNodo()) {
            case LITERAL_ENTERO -> new TipoResuelto(INT, 0);
            case LITERAL_DECIMAL -> new TipoResuelto(DOUBLE, 0);
            case LITERAL_CADENA -> new TipoResuelto(STRING, 0);
            case LITERAL_CARACTER -> new TipoResuelto(CHAR, 0);
            case LITERAL_BOOL -> new TipoResuelto(BOOLEAN, 0);
            case LITERAL_NULO -> new TipoResuelto(NULO, 0);
            case LISTA_LITERAL -> null;
            case IDENTIFICADOR -> {
                NodoExpr.Identificador id = (NodoExpr.Identificador) expr;
                Optional<TablaSimbolosZ.SimboloVariable> simbolo = tabla.buscarVariable(id.nombre());
                yield simbolo.map(s -> new TipoResuelto(s.tipo(), s.dimensiones())).orElse(null);
            }

            case ACCESO_ARRAY -> {
                NodoExpr.AccesoArray acceso = (NodoExpr.AccesoArray) expr;
                TipoResuelto tipoIndice = tipoDeExpresion(acceso.indice());
                if (tipoIndice != null && !INT.equals(tipoIndice.base())) {
                    errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                            "Tipo incompatible",
                            "El índice de un arreglo debe ser 'int', se encontró '" + tipoIndice.base() + "'"));
                }

                TipoResuelto tipoArreglo = tipoDeExpresion(acceso.arreglo());
                if (tipoArreglo == null) yield null;
                if (tipoArreglo.dimensiones() == 0) {
                    errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                            "Tipo incompatible", "No se puede indexar un valor que no es arreglo"));
                    yield null;
                }

                if (acceso.arreglo() instanceof NodoExpr.Identificador id && acceso.indice() instanceof NodoExpr.LiteralEntero indiceLit) {
                    Optional<TablaSimbolosZ.SimboloVariable> simbolo = tabla.buscarVariable(id.nombre());
                    if (simbolo.isPresent() && simbolo.get().tamanoConocido() != null) {
                        int tamano = simbolo.get().tamanoConocido();
                        int indice = indiceLit.valor();
                        if (indice < 0 || indice >= tamano) {
                            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                                    "Indice fuera de rango",
                                    "El índice " + indice + " está fuera del rango válido [0, " + (tamano - 1) + "] para '" + id.nombre() + "'"));
                        }
                    }
                }

                yield new TipoResuelto(tipoArreglo.base(), tipoArreglo.dimensiones() - 1);
            }

            case ACCESO_ATRIBUTO -> {
                NodoExpr.AccesoAtributo acceso = (NodoExpr.AccesoAtributo) expr;
                TipoResuelto tipoObjeto = tipoDeExpresion(acceso.objeto());
                if (tipoObjeto == null) yield null;

                if (!tipoObjeto.base().equals(tabla.getNombreClase())) {
                    errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                            "Acceso invalido",
                            "No se puede acceder a '" + acceso.atributo() + "' porque '" + tipoObjeto.base() + "' no es un objeto"));
                    yield null;
                }

                Optional<TablaSimbolosZ.SimboloAtributo> atributo = tabla.buscarAtributo(acceso.atributo());
                if (atributo.isEmpty()) {
                    errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                            "Atributo no declarado",
                            "'" + acceso.atributo() + "' no existe en la clase '" + tipoObjeto.base() + "'"));
                    yield null;
                }
                yield new TipoResuelto(atributo.get().tipo(), atributo.get().dimensiones());
            }

            case BINARIA -> {
                NodoExpr.Binaria bin = (NodoExpr.Binaria) expr;
                TipoResuelto izq = tipoDeExpresion(bin.izquierda());
                TipoResuelto der = tipoDeExpresion(bin.derecha());
                yield tipoResultanteBinaria(bin.operador(), izq, der, bin.izquierda(), bin.derecha(), bin.linea(), bin.columna());
            }

            case UNARIA -> {
                NodoExpr.Unaria un = (NodoExpr.Unaria) expr;
                TipoResuelto operando = tipoDeExpresion(un.operando());
                if (operando == null) yield null;

                if ("!".equals(un.operador())) {
                    if (!BOOLEAN.equals(operando.base())) {
                        errores.add(new ErrorSemantico(un.linea(), un.columna(),
                                "Tipo incompatible", "'!' requiere 'boolean', se encontró '" + operando.base() + "'"));
                        yield null;
                    }
                    yield operando;
                } else { // "-"
                    if (!esNumerico(operando.base())) {
                        errores.add(new ErrorSemantico(un.linea(), un.columna(),
                                "Tipo incompatible", "'-' unario requiere tipo numérico, se encontró '" + operando.base() + "'"));
                        yield null;
                    }
                    yield operando;
                }
            }

            case INCREMENTO_DECREMENTO -> {
                NodoExpr.IncrementoDecremento inc = (NodoExpr.IncrementoDecremento) expr;
                if (!esLValue(inc.operando())) {
                    errores.add(new ErrorSemantico(inc.linea(), inc.columna(),
                            "Operando inválido",
                            "El operador '" + inc.operador() + "' solo se puede aplicar sobre una variable, "
                                    + "un elemento de arreglo o un atributo"));
                    yield null;
                }

                TipoResuelto operando = tipoDeExpresion(inc.operando());
                if (operando != null && !esNumerico(operando.base())) {
                    errores.add(new ErrorSemantico(inc.linea(), inc.columna(),
                            "Tipo incompatible", "'" + inc.operador() + "' requiere tipo numérico"));
                    yield null;
                }
                yield operando;
            }

            case TERNARIA -> {
                NodoExpr.Ternaria t = (NodoExpr.Ternaria) expr;
                TipoResuelto cond = tipoDeExpresion(t.condicion());
                if (cond != null && !BOOLEAN.equals(cond.base())) {
                    errores.add(new ErrorSemantico(t.linea(), t.columna(),
                            "Tipo incompatible", "La condición del ternario debe ser 'boolean'"));
                }
                TipoResuelto siVerdadero = tipoDeExpresion(t.siVerdadero());
                TipoResuelto siFalso = tipoDeExpresion(t.siFalso());
                if (siVerdadero == null || siFalso == null) yield null;

                if (esAsignable(siVerdadero, siFalso)) yield siVerdadero;
                if (esAsignable(siFalso, siVerdadero)) yield siFalso;
                errores.add(new ErrorSemantico(t.linea(), t.columna(),
                        "Tipo incompatible",
                        "Las dos ramas del ternario tienen tipos incompatibles: '"
                                + siVerdadero.base() + "' y '" + siFalso.base() + "'"));
                yield null;
            }

            case LLAMADA_FUNCION -> {
                NodoExpr.LlamadaFuncion llamada = (NodoExpr.LlamadaFuncion) expr;
                List<TipoResuelto> tiposArgs = llamada.argumentos().stream().map(this::tipoDeExpresion).toList();
                TablaSimbolosZ.Firma firma = resolverSobrecarga(tabla.getMetodos(llamada.nombre()), tiposArgs,
                        llamada.linea(), llamada.columna(), llamada.nombre(), "Metodo");
                yield firma == null || firma.tipoRetorno() == null ? null : new TipoResuelto(firma.tipoRetorno(), 0);
            }

            case LLAMADA_METODO -> {
                NodoExpr.LlamadaMetodo llamada = (NodoExpr.LlamadaMetodo) expr;
                TipoResuelto tipoObjeto = tipoDeExpresion(llamada.objeto());
                List<TipoResuelto> tiposArgs = llamada.argumentos().stream().map(this::tipoDeExpresion).toList();
                if (tipoObjeto == null) yield null;

                if (!tipoObjeto.base().equals(tabla.getNombreClase())) {
                    errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(),
                            "Acceso invalido",
                            "No se puede llamar a '" + llamada.nombre() + "' porque '" + tipoObjeto.base() + "' no es un objeto"));
                    yield null;
                }

                TablaSimbolosZ.Firma firma = resolverSobrecarga(tabla.getMetodos(llamada.nombre()), tiposArgs,
                        llamada.linea(), llamada.columna(), llamada.nombre(), "Metodo");
                yield firma == null || firma.tipoRetorno() == null ? null : new TipoResuelto(firma.tipoRetorno(), 0);
            }

            case INSTANCIA_OBJETO -> {
                NodoExpr.InstanciaObjeto instancia = (NodoExpr.InstanciaObjeto) expr;
                if (!instancia.tipoClase().equals(tabla.getNombreClase())) yield null;

                List<TipoResuelto> tiposArgs = instancia.argumentos().stream().map(this::tipoDeExpresion).toList();
                resolverSobrecarga(tabla.getConstructores(instancia.tipoClase()), tiposArgs,
                        instancia.linea(), instancia.columna(), instancia.tipoClase(), "Constructor");
                yield new TipoResuelto(instancia.tipoClase(), 0);
            }

            case ARREGLO_NUEVO -> {
                NodoExpr.ArregloNuevo arreglo = (NodoExpr.ArregloNuevo) expr;
                for (NodoExpr dim : arreglo.dimensiones()) {
                    if (dim == null) continue;
                    TipoResuelto tipoDim = tipoDeExpresion(dim);
                    if (tipoDim != null && !INT.equals(tipoDim.base())) {
                        errores.add(new ErrorSemantico(arreglo.linea(), arreglo.columna(),
                                "Tipo incompatible", "El tamaño de un arreglo debe ser 'int'"));
                    }
                }
                yield new TipoResuelto(arreglo.tipoBase(), arreglo.dimensiones().size());
            }
        };
    }

    /** Calcula el tipo resultado de una operación binaria, reportando incompatibilidades. */
    private TipoResuelto tipoResultanteBinaria(String operador, TipoResuelto izq, TipoResuelto der,
                                               NodoExpr izqExpr, NodoExpr derExpr,
                                               int linea, int columna) {
        if (izq == null || der == null) return null;

        return switch (operador) {
            case "+" -> {
                if (STRING.equals(izq.base()) || STRING.equals(der.base())) yield new TipoResuelto(STRING, 0);
                if (esNumerico(izq.base()) && esNumerico(der.base())) yield promocionNumerica(izq, der);
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "-", "*", "/", "%" -> {
                if (esNumerico(izq.base()) && esNumerico(der.base())) {
                    // Validar división/módulo entre cero literal.
                    if (("/".equals(operador) || "%".equals(operador)) && esCeroLiteral(derExpr)) {
                        errores.add(new ErrorSemantico(linea, columna,
                                "División entre cero",
                                "No se puede dividir ni calcular módulo por cero"));
                        yield null;
                    }
                    yield promocionNumerica(izq, der);
                }
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "<", ">", "<=", ">=" -> {
                if (esNumerico(izq.base()) && esNumerico(der.base())) yield new TipoResuelto(BOOLEAN, 0);
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "==", "!=" -> {
                boolean compatibles = izq.base().equals(der.base())
                        || (esNumerico(izq.base()) && esNumerico(der.base()))
                        || NULO.equals(izq.base()) || NULO.equals(der.base());
                if (compatibles) yield new TipoResuelto(BOOLEAN, 0);
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            case "&&", "||" -> {
                if (BOOLEAN.equals(izq.base()) && BOOLEAN.equals(der.base())) yield new TipoResuelto(BOOLEAN, 0);
                reportarIncompatible(operador, izq, der, linea, columna);
                yield null;
            }
            default -> null;
        };
    }

    private TipoResuelto promocionNumerica(TipoResuelto izq, TipoResuelto der) { /** Promoción numérica: double gana sobre int. */
        return (DOUBLE.equals(izq.base()) || DOUBLE.equals(der.base()))
                ? new TipoResuelto(DOUBLE, 0) : new TipoResuelto(INT, 0);
    }

    /** Reporta tipos incompatibles en una operación binaria. */
    private void reportarIncompatible(String operador, TipoResuelto izq, TipoResuelto der, int linea, int columna) {
        errores.add(new ErrorSemantico(linea, columna,
                "Tipo incompatible",
                "El operador '" + operador + "' no admite '" + izq.base() + "' y '" + der.base() + "'"));
    }

    // COMPATIBILIDAD Y ASIGNACION
    /** True si un valor de tipo 'origen' se puede asignar a uno de tipo 'destino'. */
    private boolean esAsignable(TipoResuelto destino, TipoResuelto origen) {
        if (NULO.equals(origen.base())) {
            return !esPrimitivo(destino.base());
        }
        if (destino.dimensiones() != origen.dimensiones()) return false;
        if (destino.dimensiones() > 0) {
            return destino.base().equals(origen.base());
        }
        if (destino.base().equals(origen.base())) return true;
        return DOUBLE.equals(destino.base()) && INT.equals(origen.base());
    }

    /** Valida que una inicialización sea compatible con el tipo declarado. */
    public void validarInicializacion(String tipoDeclarado, int dimensionesDeclaradas, NodoExpr inicializacion) {
        if (inicializacion == null) return;

        if (inicializacion.tipoNodo() == com.example.contacto_3xtrat3r3str3.zetariano.nodo.TipoNodoExpr.LISTA_LITERAL) {
            NodoExpr.ListaLiteral lista = (NodoExpr.ListaLiteral) inicializacion;
            TipoResuelto tipoElemento = new TipoResuelto(tipoDeclarado, Math.max(0, dimensionesDeclaradas - 1));
            for (NodoExpr elem : lista.elementos()) {
                TipoResuelto tipoValor = tipoDeExpresion(elem);
                if (tipoValor != null && !esAsignable(tipoElemento, tipoValor)) {
                    errores.add(new ErrorSemantico(elem.linea(), elem.columna(),
                            "Tipo incompatible",
                            "No se puede colocar un valor '" + tipoValor.base() + "' en un arreglo de '" + tipoDeclarado + "'"));
                }
            }
            return;
        }

        TipoResuelto tipoValor = tipoDeExpresion(inicializacion);
        if (tipoValor == null) return;
        TipoResuelto tipoDestino = new TipoResuelto(tipoDeclarado, dimensionesDeclaradas);
        if (!esAsignable(tipoDestino, tipoValor)) {
            errores.add(new ErrorSemantico(inicializacion.linea(), inicializacion.columna(),
                    "Tipo incompatible",
                    "No se puede asignar '" + tipoValor.base() + "' a una variable de tipo '" + tipoDeclarado + "'"));
        }
    }

    /** Valida una asignación simple o compuesta. */
    public void validarAsignacion(String operador, NodoExpr destino, NodoExpr valor) {

        if (!esLValue(destino)) {
            errores.add(new ErrorSemantico(destino.linea(), destino.columna(), "Destino inválido",
                    "El lado izquierdo de una asignación debe ser una variable, "
                            + "un elemento de arreglo o un atributo"));
            return;
        }

        TipoResuelto tipoDestino = tipoDeExpresion(destino);
        TipoResuelto tipoValor = tipoDeExpresion(valor);
        if (tipoDestino == null || tipoValor == null) return;

        // Asignación simple.
        if ("=".equals(operador)) {
            if (!esAsignable(tipoDestino, tipoValor)) {
                errores.add(new ErrorSemantico(valor.linea(), valor.columna(),
                        "Tipo incompatible",
                        "No se puede asignar '" + tipoValor.base() + "' a algo de tipo '" + tipoDestino.base() + "'"));
            }
            return;
        }

        // Asignación compuesta: simular la operación binaria.
        String operadorBinario = switch (operador) {
            case "+=" -> "+";
            case "-=" -> "-";
            case "*=" -> "*";
            default -> null;
        };

        if (operadorBinario == null) return;

        TipoResuelto tipoResultado = tipoResultanteBinaria(
                operadorBinario, tipoDestino, tipoValor, destino, valor, destino.linea(), destino.columna());

        if (tipoResultado == null) return;

        if (!esAsignable(tipoDestino, tipoResultado)) {
            errores.add(new ErrorSemantico(valor.linea(), valor.columna(),
                    "Tipo incompatible",
                    "El resultado de '" + operador + "' ('" + tipoResultado.base()
                            + "') no se puede asignar al destino de tipo '" + tipoDestino.base() + "'"));
        }
    }

    /** Valida que una condición sea de tipo boolean. */
    public void validarCondicionBooleana(NodoExpr condicion) {
        TipoResuelto tipo = tipoDeExpresion(condicion);
        if (tipo != null && !BOOLEAN.equals(tipo.base())) {
            errores.add(new ErrorSemantico(condicion.linea(), condicion.columna(),
                    "Flujo inválido",
                    "La condición debe ser 'boolean', se encontró '" + tipo.base() + "'"));
        }
    }

    /** Valida que un 'return' sea compatible con el tipo de retorno del método. */
    public void validarRetorno(NodoSentencia.Retorno retorno, String tipoRetornoEsperado) {
        if (tipoRetornoEsperado == null) {
            if (retorno.valor() != null) {
                errores.add(new ErrorSemantico(retorno.linea(), retorno.columna(),
                        "Tipo incompatible", "El método es 'void', no debe retornar un valor"));
            }
            return;
        }
        if (retorno.valor() == null) {
            errores.add(new ErrorSemantico(retorno.linea(), retorno.columna(),
                    "Tipo incompatible", "Se esperaba retornar un valor de tipo '" + tipoRetornoEsperado + "'"));
            return;
        }
        TipoResuelto tipoValor = tipoDeExpresion(retorno.valor());
        TipoResuelto tipoEsperado = new TipoResuelto(tipoRetornoEsperado, 0);
        if (tipoValor != null && !esAsignable(tipoEsperado, tipoValor)) {
            errores.add(new ErrorSemantico(retorno.linea(), retorno.columna(),
                    "Tipo incompatible",
                    "Se esperaba retornar '" + tipoRetornoEsperado + "', se encontró '" + tipoValor.base() + "'"));
        }
    }

    // RESOLUCION DE SOBRECARGA

    /** Resuelve la firma que coincide con los tipos de argumentos dados. */
    private TablaSimbolosZ.Firma resolverSobrecarga(List<TablaSimbolosZ.Firma> firmas, List<TipoResuelto> tiposArgs,
                                                    int linea, int columna, String nombre, String tipoElemento) {
        if (firmas.isEmpty()) {
            errores.add(new ErrorSemantico(linea, columna,
                    tipoElemento + " no declarado", "'" + nombre + "' no existe"));
            return null;
        }
        if (tiposArgs.stream().anyMatch(java.util.Objects::isNull)) return null;

        // 1. Coincidencia exacta.
        for (TablaSimbolosZ.Firma f : firmas) {
            if (coincideExacto(f, tiposArgs)) return f;
        }
        // 2. Coincidencia con promoción int -> double.
        List<TablaSimbolosZ.Firma> candidatas = firmas.stream().filter(f -> coincideConPromocion(f, tiposArgs)).toList();
        if (candidatas.size() == 1) return candidatas.get(0);
        if (candidatas.size() > 1) {
            errores.add(new ErrorSemantico(linea, columna,
                    "Llamada ambigua", "Mas de una sobrecarga de '" + nombre + "' coincide con estos argumentos"));
            return null;
        }

        errores.add(new ErrorSemantico(linea, columna,
                "Llamada invalida", "Ninguna sobrecarga de '" + nombre + "' coincide con estos argumentos"));
        return null;
    }

    /** Compara firma y argumentos por igualdad exacta de tipos. */
    private boolean coincideExacto(TablaSimbolosZ.Firma f, List<TipoResuelto> tiposArgs) {
        if (f.parametros().size() != tiposArgs.size()) return false;
        for (int i = 0; i < tiposArgs.size(); i++) {
            TablaSimbolosZ.Parametro p = f.parametros().get(i);
            if (!p.tipo().equals(tiposArgs.get(i).base()) || p.dimensiones() != tiposArgs.get(i).dimensiones()) {
                return false;
            }
        }
        return true;
    }

    /** Compara firma y argumentos permitiendo promoción. */
    private boolean coincideConPromocion(TablaSimbolosZ.Firma f, List<TipoResuelto> tiposArgs) {
        if (f.parametros().size() != tiposArgs.size()) return false;
        for (int i = 0; i < tiposArgs.size(); i++) {
            TablaSimbolosZ.Parametro p = f.parametros().get(i);
            TipoResuelto destino = new TipoResuelto(p.tipo(), p.dimensiones());
            if (!esAsignable(destino, tiposArgs.get(i))) return false;
        }
        return true;
    }

    // HELPERS
    /** True si la expresión puede ser destino de ++/-- o de una asignación. */
    private boolean esLValue(NodoExpr expr) {
        if (expr == null) return false;
        return switch (expr.tipoNodo()) {
            case IDENTIFICADOR, ACCESO_ARRAY, ACCESO_ATRIBUTO -> true;
            default -> false;
        };
    }

    /** Valida que la expresión de un switch sea int, char, String o boolean. */
    public void validarTipoSwitch(NodoExpr expresion) {
        TipoResuelto tipo = tipoDeExpresion(expresion);
        if (tipo == null) return;

        String base = tipo.base();
        boolean valido = INT.equals(base)
                || CHAR.equals(base)
                || STRING.equals(base)
                || BOOLEAN.equals(base);

        if (!valido) {
            errores.add(new ErrorSemantico(expresion.linea(), expresion.columna(),
                    "Tipo incompatible en switch",
                    "La expresión del switch debe ser 'int', 'char', 'String' o 'boolean', se encontró '"
                            + base + "'"));
        }
    }

    private boolean esCeroLiteral(NodoExpr expr) {
        return expr instanceof NodoExpr.LiteralEntero lit && lit.valor() == 0;
    }
}