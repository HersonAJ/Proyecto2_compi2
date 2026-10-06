package com.example.contacto_3xtrat3r3str3.y.semantica;

import com.example.contacto_3xtrat3r3str3.y.ast.NodoExpr;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;

/**
 * Validador de alcance para .y.
 * Verifica que los identificadores usados existan en la tabla de simbolos.
 *
 * Validaciones:
 *   - Identificador no declarado       -> resolverNombre / resolverIdentificador
 *   - Funcion no declarada             -> resolverLlamadaFuncion
 *   - Estructura no declarada          -> resolverTipoEstructura
 */
public class ValidadorAlcance {

    private final TablaSimbolos tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorAlcance(TablaSimbolos tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    /**
     * Recorre cualquier expresion buscando identificadores y llamadas a funcion,
     * y reporta si alguno no existe.
     */
    public void resolverExpresion(NodoExpr expr) {
        if (expr == null) return;

        switch (expr.tipoNodo()) {
            case IDENTIFICADOR -> resolverIdentificador((NodoExpr.Identificador) expr);

            case ACCESO_ARRAY -> {
                NodoExpr.AccesoArray acceso = (NodoExpr.AccesoArray) expr;
                resolverExpresion(acceso.arreglo());
                resolverExpresion(acceso.indice());
            }

            case ACCESO_ATRIBUTO -> {
                NodoExpr.AccesoAtributo acceso = (NodoExpr.AccesoAtributo) expr;
                resolverExpresion(acceso.objeto());
            }

            case BINARIA -> {
                NodoExpr.Binaria bin = (NodoExpr.Binaria) expr;
                resolverExpresion(bin.izquierda());
                resolverExpresion(bin.derecha());
            }

            case UNARIA -> resolverExpresion(((NodoExpr.Unaria) expr).operando());

            case LLAMADA_FUNCION -> {
                NodoExpr.LlamadaFuncion llamada = (NodoExpr.LlamadaFuncion) expr;
                resolverLlamadaFuncion(llamada);
                for (NodoExpr arg : llamada.argumentos()) {
                    resolverExpresion(arg);
                }
            }

            // Los literales no tienen nada que resolver.
            case LITERAL_ENTERO, LITERAL_FLOTANTE, LITERAL_CADENA, LITERAL_CARACTER, LITERAL_BOOL -> { }
        }
    }

    /** Resuelve un identificador buscando su nombre en la tabla. */
    public void resolverIdentificador(NodoExpr.Identificador id) {
        resolverNombre(id.nombre(), id.linea(), id.columna());
    }

    /**
     * Resuelve un nombre suelto (sin nodo completo) buscando en la tabla.
     * Si no existe como variable ni como funcion, reporta error.
     */
    public void resolverNombre(String nombre, int linea, int columna) {
        if (tabla.buscarVariable(nombre).isEmpty()) {
            if (tabla.buscarFuncion(nombre).isPresent()) {
                return;
            }
            errores.add(new ErrorSemantico(linea, columna,
                    "Identificador no declarado",
                    "'" + nombre + "' no existe en el ámbito actual"));
        }
    }

    /** Verifica que la funcion llamada exista en la tabla. */
    public void resolverLlamadaFuncion(NodoExpr.LlamadaFuncion llamada) {
        if (tabla.buscarFuncion(llamada.nombre()).isEmpty()) {
            errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(),
                    "Función no declarada",
                    "La función '" + llamada.nombre() + "' no existe"));
        }
    }

    /** Verifica que una estructura exista en la tabla. */
    public void resolverTipoEstructura(String nombreTipo, int linea, int columna) {
        if (nombreTipo != null && tabla.buscarEstructura(nombreTipo).isEmpty()) {
            errores.add(new ErrorSemantico(linea, columna,
                    "Estructura no declarada",
                    "El tipo '" + nombreTipo + "' no corresponde a ninguna estructura declarada"));
        }
    }
}