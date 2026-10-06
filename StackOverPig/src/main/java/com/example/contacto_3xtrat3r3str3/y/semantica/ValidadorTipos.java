package com.example.contacto_3xtrat3r3str3.y.semantica;

import com.example.contacto_3xtrat3r3str3.y.ast.NodoExpr;
import com.example.contacto_3xtrat3r3str3.y.ast.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;
import java.util.Optional;

/**
 * Validación de tipos e inferencia de expresiones en .y.
 *
 * Validaciones:
 *   - Índice de arreglo no entero              -> tipoDeAccesoArray
 *   - Índice fuera de rango                    -> validarRangoIndice
 *   - Acceso a atributo inválido               -> tipoDeAccesoAtributo
 *   - Tipos incompatibles en operación lógica  -> tipoDeBinaria
 *   - Tipos incompatibles en comparación       -> tipoDeBinaria
 *   - Tipos incompatibles en operación numérica-> tipoDeBinaria
 *   - Tipos incompatibles en negación          -> tipoDeUnaria
 *   - Inicialización incompatible              -> validarInicializacion
 *   - Condición no booleana                    -> validarCondicionBooleana
 *   - Retorno incompatible                     -> validarRetorno
 *   - Argumentos de llamada incompatibles      -> validarLlamada
 *   - Asignación incompatible                  -> validarAsignacion
 */
public class ValidadorTipos {

    private final TablaSimbolos tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorTipos(TablaSimbolos tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    // INFERENCIA DE TIPOS
    /** Devuelve el tipo de una expresión, o null si no se puede inferir. */
    public String tipoDeExpresion(NodoExpr expresion) {
        if (expresion == null) return null;

        return switch (expresion.tipoNodo()) {
            case LITERAL_ENTERO ->  "entero";
            case LITERAL_FLOTANTE -> "flotante";
            case LITERAL_CADENA -> "cadena";
            case LITERAL_CARACTER -> "caracter";
            case LITERAL_BOOL -> "bool";

            case IDENTIFICADOR -> tipoDeIdentificador((NodoExpr.Identificador) expresion);
            case ACCESO_ARRAY -> tipoDeAccesoArray((NodoExpr.AccesoArray) expresion);
            case ACCESO_ATRIBUTO -> tipoDeAccesoAtributo((NodoExpr.AccesoAtributo) expresion);

            case BINARIA -> tipoDeBinaria((NodoExpr.Binaria) expresion);
            case UNARIA -> tipoDeUnaria((NodoExpr.Unaria) expresion);

            case LLAMADA_FUNCION -> tipoDeLlamada((NodoExpr.LlamadaFuncion) expresion);
            case LEER -> "cadena";
        };
    }

    /** Tipo de un identificador: su tipo o su tipo de estructura. */
    private String tipoDeIdentificador(NodoExpr.Identificador id) {
        Optional<TablaSimbolos.SimboloVariable> simbolo = tabla.buscarVariable(id.nombre());
        if (simbolo.isEmpty()) {
            return  null;
        }
        TablaSimbolos.SimboloVariable variable = simbolo.get();
        if (variable.esEstructura()) {
            return  variable.tipoEstructura();
        }
        return variable.tipo();
    }

    /** Tipo de un acceso a arreglo: valida índice y rango, devuelve el tipo base. */
    private String tipoDeAccesoArray(NodoExpr.AccesoArray acceso) {
        // 1. El índice debe ser de tipo entero.
        String tipoIndice = tipoDeExpresion(acceso.indice());
        if (tipoIndice != null && !tipoIndice.equals("entero")) {
            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                    "Tipo incompatible en índice",
                    "El índice de un arreglo debe ser 'entero', se encontró '" + tipoIndice + "'"));
        }

        // 2. Validar índice fuera de rango (solo si el índice es literal entero).
        validarRangoIndice(acceso);

        // 3. El tipo del arreglo es el tipo base del arreglo.
        String tipoArreglo = tipoDeExpresion(acceso.arreglo());
        return tipoArreglo;
    }

    /**
     * Valida que un índice literal entero esté dentro del rango declarado.
     * Solo aplica cuando el índice es literal, el arreglo es identificador
     * simple y tiene tamaño conocido.
     */
    private void validarRangoIndice(NodoExpr.AccesoArray acceso) {
        // El índice debe ser un literal entero.
        if (!(acceso.indice() instanceof NodoExpr.LiteralEntero lit)) {
            return;
        }
        if (!(acceso.arreglo() instanceof NodoExpr.Identificador id)) {
            return;
        }

        // Buscar la variable en la tabla.
        Optional<TablaSimbolos.SimboloVariable> simboloOpt = tabla.buscarVariable(id.nombre());
        if (simboloOpt.isEmpty()) {
            return;
        }

        TablaSimbolos.SimboloVariable simbolo = simboloOpt.get();
        if (!simbolo.esArreglo() || simbolo.tamanos().isEmpty()) {
            return;
        }

        int indice = lit.valor();

        // Validar índice negativo.
        if (indice < 0) {
            errores.add(new ErrorSemantico(acceso.indice().linea(), acceso.indice().columna(),
                    "Índice fuera de rango",
                    "El índice no puede ser negativo: " + indice));
            return;
        }

        int tamano = simbolo.tamanos().get(0);

        if (indice >= tamano) {
            errores.add(new ErrorSemantico(acceso.indice().linea(), acceso.indice().columna(),
                    "Índice fuera de rango",
                    "El índice " + indice + " excede el tamaño del arreglo '" + id.nombre()
                            + "' (tamaño " + tamano + ")"));
        }
    }

    /** Tipo de un acceso a atributo: valida que la estructura y el campo existan. */
    private String tipoDeAccesoAtributo(NodoExpr.AccesoAtributo acceso) {
        String tipoObjeto = tipoDeExpresion(acceso.objeto());
        if (tipoObjeto == null) {
            return null;
        }

        Optional<TablaSimbolos.DefinicionEstructura> def = tabla.buscarEstructura(tipoObjeto);
        if (def.isEmpty()) {
            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                    "Acceso a atributo inválido",
                    "'" + tipoObjeto + "' no es una estructura"));
            return null;
        }

        String tipoAtrubuto = def.get().atributos().get(acceso.atributo());
        if (tipoAtrubuto == null) {
            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(), "Atributo no encontrado",
                    "La estructura '" + tipoObjeto + "' no tiene un atributo '" + acceso.atributo() + "'"));
            return null;
        }
        return tipoAtrubuto;
    }

    /** Tipo resultado de una operación binaria, validando compatibilidad. */
    private String tipoDeBinaria(NodoExpr.Binaria bin) {
        String tipoIzq = tipoDeExpresion(bin.izquierda());
        String tipoDer = tipoDeExpresion(bin.derecha());

        if (tipoIzq == null || tipoDer == null) {
            return null;
        }

        String operador = bin.operador();

        // Operadores lógicos: ambos deben ser bool.
        if (operador.equals("&&") || operador.equals("||")) {
            if (!tipoIzq.equals("bool") || !tipoDer.equals("bool")) {
                errores.add(new ErrorSemantico(bin.linea(), bin.columna(), "Tipo incompatible en operacion logica",
                        "El operador '" + operador + "' requiere 'bool', se encontro '" + tipoIzq + "' y '" + tipoDer + "'"));
                return null;
            }
            return "bool";
        }

        // Operadores relacionales y de igualdad: resultado bool.
        if (operador.equals("==") || operador.equals("!=") || operador.equals("<") || operador.equals(">")
                || operador.equals("<=") || operador.equals(">=")) {

            if (!sonCompatibles(tipoIzq, tipoDer)) {
                errores.add(new ErrorSemantico(bin.linea(), bin.columna(), "Tipo incompatible en comparacion",
                        "No se puede comparar '" + tipoIzq + "' con '" + tipoDer +"'"));
                return null;
            }
            return "bool";
        }

        // Suma con cadena: resultado cadena por concatenación.
        if(operador.equals("+")) {
            if (tipoIzq.equals("cadena") || tipoDer.equals("cadena")) {
                return "cadena";
            }
        }

        // Operaciones aritméticas: ambos deben ser numéricos.
        if (!esNumerico(tipoIzq) || !esNumerico(tipoDer)) {
            errores.add(new ErrorSemantico(bin.linea(), bin.columna(), "Tipo incompatible en operacion numerica",
                    "El operador '" + operador +"' requiere tipos numericos, se encontro '" + tipoIzq+ "' y '" + tipoDer + "'"));
            return null;
        }

        // Promoción numérica: flotante gana.
        if (tipoIzq.equals("flotante") || tipoDer.equals("flotante")) {
            return "flotante";
        }
        return "entero";
    }

    /** Tipo resultado de una operación unaria. */
    private String tipoDeUnaria(NodoExpr.Unaria unaria) {
        String tipoOperando = tipoDeExpresion(unaria.operando());
        if (tipoOperando == null) return null;

        if (unaria.operador().equals("!")) {
            if (!tipoOperando.equals("bool")) {
                errores.add(new ErrorSemantico(unaria.linea(), unaria.columna(), "Tipo incompatible de negacion",
                        "El operador '!' requiere 'bool', se encontro '" + tipoOperando + "'"));
                return null;
            }
            return "bool";
        }

        if (unaria.operador().equals("-")) {
            if (!esNumerico(tipoOperando)) {
                errores.add(new ErrorSemantico(unaria.linea(), unaria.columna(), "Tipo incompatible en negacion aritmetica",
                        "El operador '-' requiere tipo numerico, se encontro '" + tipoOperando + "'"));
                return null;
            }
            return tipoOperando;
        }

        // ++ y -- como expresiones
        if (unaria.operador().equals("++") || unaria.operador().equals("--")) {
            if (!esNumerico(tipoOperando)) {
                errores.add(new ErrorSemantico(unaria.linea(), unaria.columna(), "Tipo incompatible en incremento/decremento" ,
                        "El operador '" + unaria.operador() + "' requiere tipo numerico, se encontro" + tipoOperando + "'"));
                return null;
            }
            return tipoOperando;
        }
        return null;
    }

    /** Tipo de retorno de una llamada a función. */
    private String tipoDeLlamada(NodoExpr.LlamadaFuncion llamada) {
        Optional<TablaSimbolos.DefinicionFuncion> definicion = tabla.buscarFuncion(llamada.nombre());
        if (definicion.isEmpty()) {
            return  null;
        }
        return definicion.get().tipoRetorno();
    }

    // HELPERS
    private boolean esNumerico(String tipo) {
        return tipo != null && (tipo.equals("entero") || tipo.equals("flotante"));
    }

    /** True si los dos tipos son compatibles para una comparación. */
    private boolean sonCompatibles(String tipoA, String tipoB) {
        if (esNumerico(tipoA) && esNumerico(tipoB)) return true;
        if (tipoA.equals(tipoB)) return true;
        return false;
    }

    // VALIDACIONES
    /** Valida que una inicialización sea compatible con el tipo declarado. */
    public void validarInicializacion(String tipoDeclarado, NodoExpr inicializacion) {
        if (inicializacion == null) return;

        String tipoExpr = tipoDeExpresion(inicializacion);
        if (tipoExpr == null) return;

        if (!esAsignableA(tipoDeclarado, tipoExpr)) {
            errores.add(new ErrorSemantico(inicializacion.linea(), inicializacion.columna(), "Tipo incompatible en inicializacion",
                    "No se puede asignar '" + tipoExpr + "' a una variable de tipo '" + tipoDeclarado + "'"));
        }
    }

    /** Valida que una condición sea de tipo bool. */
    public void validarCondicionBooleana(NodoExpr condicion) {
        if (condicion == null) return;

        String tipo = tipoDeExpresion(condicion);
        if (tipo == null) return;

        if (!tipo.equals("bool")) {
            errores.add(new ErrorSemantico(condicion.linea(), condicion.columna(), "Tipo incompatible en condicion",
                    "La condicion debe ser 'bool', se encontro '" + tipo + "'"));
        }
    }

    /** Valida que un 'retornar' sea compatible con el tipo de retorno declarado. */
    public void validarRetorno(NodoSentencia.Retorno retorno, String tipoRetornoEsperado) {
        // Función sin retorno que no debe devolver valor.
        if (tipoRetornoEsperado == null) {
            if (retorno.valor() != null) {
                errores.add(new ErrorSemantico(retorno.linea(), retorno.columna(), "Retorno incompatible",
                        "La funcion no declara tipo de retorno, pero se esta retornando un valor"));
            }
            return;
        }

        String tipoValor = tipoDeExpresion(retorno.valor());
        if (tipoValor == null) return;

        if (!esAsignableA(tipoRetornoEsperado, tipoValor)) {
            errores.add(new ErrorSemantico(retorno.linea(), retorno.columna(), "Tipo incompatible de retorno",
                    "Se esperaba '" + tipoRetornoEsperado + "', se encontro '" + tipoValor + "'"));
        }
    }

    /** Valida que los argumentos de una llamada coincidan con los parámetros. */
    public void validarLlamada(NodoExpr.LlamadaFuncion llamada, TablaSimbolos.DefinicionFuncion definicion) {
        if (llamada == null || definicion == null) return;

        List<NodoExpr> argumentos = llamada.argumentos();
        List<TablaSimbolos.Parametro> params = definicion.parametros();

        // Verificar número de argumentos.
        if (argumentos.size() != params.size()) {
            errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(), "Argumentos incorrectos",
                    "La funcion '" + definicion.nombre() + "' espera  " + params.size() +
                            " argumentos, se encontraron " + argumentos.size()));
            return;
        }

        // Verificar tipo de cada argumento.
        for (int i = 0; i < argumentos.size(); i++) {
            NodoExpr arg = argumentos.get(i);
            TablaSimbolos.Parametro parametro = params.get(i);

            String tipoArg = tipoDeExpresion(arg);
            if (tipoArg == null) continue;

            String tipoParam = parametro.tipo();
            if (tipoParam == null) {
                tipoParam = parametro.tipoEstructura();
            }

            if (!esAsignableA(tipoParam, tipoArg)) {
                errores.add(new ErrorSemantico(arg.linea(), arg.columna(), "Argumento incompatible",
                        "El argumento " + (i + 1) + " de '" + definicion.nombre() +
                                "' espera '" + tipoParam + "', se encontro '" + tipoArg + "'"));
            }
        }
    }

    /**
     * True si un valor de tipo 'tipoOrigen' se puede asignar a uno de tipo 'tipoDestino'.
     * Reglas:
     *   - Mismo tipo → válido.
     *   - Destino 'cadena' → acepta cualquier origen (concatenación).
     *   - Destino 'flotante' ← origen 'entero' → válido.
     */
    public boolean esAsignableA(String tipoDestino, String tipoOrigen) {
        if (tipoDestino == null || tipoOrigen == null) return false;

        if (tipoDestino.equals(tipoOrigen)) return true;
        if (tipoDestino.equals("cadena")) return true;
        if (tipoDestino.equals("flotante") && tipoOrigen.equals("entero")) return true;

        return false;
    }

    /** Valida una asignación y reporta error si los tipos no son compatibles. */
    public void validarAsignacion(String tipoDestino, String tipoValor, int linea, int columna) {
        if (tipoDestino == null || tipoValor == null) return;

        if (!esAsignableA(tipoDestino, tipoValor)) {
            errores.add(new ErrorSemantico(linea, columna,
                    "Tipo incompatible en asignación",
                    "No se puede asignar '" + tipoValor + "' a una variable de tipo '" + tipoDestino + "'"));
        }
    }
}