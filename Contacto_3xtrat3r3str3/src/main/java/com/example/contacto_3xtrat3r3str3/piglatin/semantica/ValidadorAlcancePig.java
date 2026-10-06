package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;

/**
 * Validador de alcance para .pig.
 *
 * Validaciones:
 *   - Identificador no declarado       -> resolverNombre
 *   - Función no declarada             -> resolverFuncion
 *   - Clase no declarada               -> resolverClase
 *   - Tipo no declarado                -> resolverTipo
 */
public class ValidadorAlcancePig {

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorAlcancePig(TablaSimbolosPig tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    // RESOLUCION DE EXPRESIONES
    /** Recorre una expresión resolviendo sus identificadores y tipos. */
    public void resolverExpresion(NodoExpr expr) {
        if (expr == null) return;

        switch (expr.tipoNodo()) {
            case IDENTIFICADOR -> resolverIdentificador((NodoExpr.Identificador) expr);
            case ACCESO_ARRAY -> {
                NodoExpr.AccesoArray a = (NodoExpr.AccesoArray) expr;
                resolverExpresion(a.arreglo());
                resolverExpresion(a.indice());
            }
            case ACCESO_ATRIBUTO -> {
                NodoExpr.AccesoAtributo a = (NodoExpr.AccesoAtributo) expr;
                resolverExpresion(a.objeto());
            }
            case BINARIA -> {
                NodoExpr.Binaria b = (NodoExpr.Binaria) expr;
                resolverExpresion(b.izquierda());
                resolverExpresion(b.derecha());
            }
            case INCREMENTO_DECREMENTO -> {
                NodoExpr.IncrementoDecremento inc = (NodoExpr.IncrementoDecremento) expr;
                resolverExpresion(inc.operando());
            }
            case LLAMADA_FUNCION -> {
                NodoExpr.LlamadaFuncion l = (NodoExpr.LlamadaFuncion) expr;
                resolverFuncion(l.nombre(), l.linea(), l.columna());
                for (NodoExpr arg : l.argumentos()) resolverExpresion(arg);
            }
            case LLAMADA_METODO -> {
                NodoExpr.LlamadaMetodo l = (NodoExpr.LlamadaMetodo) expr;
                resolverExpresion(l.objeto());
                for (NodoExpr arg : l.argumentos()) resolverExpresion(arg);
            }
            case INSTANCIA_OBJETO -> {
                NodoExpr.InstanciaObjeto inst = (NodoExpr.InstanciaObjeto) expr;
                resolverClase(inst.tipoClase(), inst.linea(), inst.columna());
                for (NodoExpr arg : inst.argumentos()) resolverExpresion(arg);
            }
            case LISTA_LITERAL -> {
                for (NodoExpr e : ((NodoExpr.ListaLiteral) expr).elementos()) {
                    resolverExpresion(e);
                }
            }
            case LITERAL_ENTERO, LITERAL_DECIMAL, LITERAL_TEXTO,
                 LITERAL_CARACTER, LITERAL_BOOL -> {
            }
            case UNARIA -> resolverExpresion(((NodoExpr.Unaria) expr).operando());
        }
    }

    // RESOLUCIONES ESPECIFICAS
    /** Resuelve un identificador buscando su nombre en la tabla. */
    public void resolverIdentificador(NodoExpr.Identificador id) {
        resolverNombre(id.nombre(), id.linea(), id.columna());
    }

    /** Reporta error si el nombre no es variable, función, clase ni estructura. */
    public void resolverNombre(String nombre, int linea, int columna) {
        if (tabla.buscarVariable(nombre).isEmpty()) {
            // Si es una función o clase importada, no es error.
            if (tabla.buscarFuncion(nombre).isPresent()) return;
            if (tabla.buscarClase(nombre).isPresent()) return;
            if (tabla.buscarEstructura(nombre).isPresent()) return;

            errores.add(new ErrorSemantico(linea, columna,
                    "Identificador no declarado",
                    "'" + nombre + "' no existe en el ámbito actual"));
        }
    }

    /** Verifica que la función importada exista en la tabla. */
    public void resolverFuncion(String nombre, int linea, int columna) {
        if (tabla.buscarFuncion(nombre).isEmpty()) {
            errores.add(new ErrorSemantico(linea, columna,
                    "Función no declarada",
                    "La función '" + nombre + "' no existe. ¿Falta importar un archivo .y?"));
        }
    }

    /** Verifica que la clase importada exista en la tabla. */
    public void resolverClase(String nombre, int linea, int columna) {
        if (tabla.buscarClase(nombre).isEmpty()) {
            errores.add(new ErrorSemantico(linea, columna,
                    "Clase no declarada",
                    "La clase '" + nombre + "' no existe. ¿Falta importar un archivo .z?"));
        }
    }

    /** Verifica que un tipo sea primitivo, estructura o clase importada. */
    public void resolverTipo(String tipo, int linea, int columna) {
        if (tipo == null) return;

        // Tipos primitivos de PigLatin
        if (esTipoPrimitivo(tipo)) return;

        // Estructuras o clases importadas
        if (tabla.buscarEstructura(tipo).isPresent()) return;
        if (tabla.buscarClase(tipo).isPresent()) return;

        errores.add(new ErrorSemantico(linea, columna,
                "Tipo no declarado",
                "'" + tipo + "' no corresponde a ningún tipo primitivo ni a una estructura/clase importada"));
    }

    // HELPERS
    /** True si el tipo es primitivo de .pig. */
    private boolean esTipoPrimitivo(String tipo) {
        return "numerus".equals(tipo)
                || "textum".equals(tipo)
                || "decimalis".equals(tipo)
                || "littera".equals(tipo)
                || "bool".equals(tipo);
    }
}