package com.example.contacto_3xtrat3r3str3.y.semantica;

import com.example.contacto_3xtrat3r3str3.y.ast.*;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.ArrayList;
import java.util.List;

/**
 * Declara símbolos en la tabla para .y.
 *
 * Validaciones:
 *   - Estructura duplicada o con atributos repetidos  -> declararEstructura
 *   - Parámetro duplicado en una función              -> registrarFirmaFuncion
 *   - Función duplicada                               -> registrarFirmaFuncion
 *   - Variable / arreglo / matriz / estructura duplicada -> declararVariable y similares
 */
public class ValidadorDeclaraciones {

    private final TablaSimbolos tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorDeclaraciones(TablaSimbolos tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    /** Registra una estructura global en la tabla. */
    public void declararEstructura(NodoEstructura.Estructura estructura) {
        List<TablaSimbolos.AtributoEstructura> atributos = new ArrayList<>();
        for (NodoAtributo a : estructura.atributos()) {
            NodoAtributo.Atributo atributo = (NodoAtributo.Atributo) a;
            String tipo = atributo.tipoPrimitivo() != null ? atributo.tipoPrimitivo() : atributo.tipoEstructura();
            atributos.add(new TablaSimbolos.AtributoEstructura(atributo.nombre(), tipo));
        }

        if (!tabla.declararEstructura(estructura.nombre(), atributos)) {
            errores.add(new ErrorSemantico(estructura.linea(), estructura.columna(),
                    "Declaración duplicada",
                    "La estructura '" + estructura.nombre() + "' ya existe, o tiene atributos repetidos"));
        }
    }

    /** Registra la firma de una función (nombre, parámetros, tipo retorno). */
    public void registrarFirmaFuncion(NodoFuncion.Funcion funcion) {
        List<TablaSimbolos.Parametro> parametros = new ArrayList<>();
        for (NodoParametro p : funcion.parametros()) {
            NodoParametro.Parametro param = (NodoParametro.Parametro) p;
            String tipo = param.tipoPrimitivo() != null ? param.tipoPrimitivo() : param.tipoEstructura();

            boolean yaExiste = parametros.stream().anyMatch(x -> x.nombre().equals(param.nombre()));
            if (yaExiste) {
                errores.add(new ErrorSemantico(param.linea(), param.columna(),
                        "Declaración duplicada",
                        "El parámetro '" + param.nombre() + "' ya fue declarado en '" + funcion.nombre() + "'"));
                continue;
            }
            parametros.add(new TablaSimbolos.Parametro(
                    param.nombre(), tipo, param.esArreglo(), param.esEstructura(), param.tipoEstructura()));
        }

        if (!tabla.declararFuncion(funcion.nombre(), parametros, funcion.tipoRetorno())) {
            errores.add(new ErrorSemantico(funcion.linea(), funcion.columna(),
                    "Declaración duplicada",
                    "La función '" + funcion.nombre() + "' ya fue declarada"));
        }
    }

    /** Declara los parámetros de una función en el scope actual. */
    public void declararParametrosEnScope(NodoFuncion.Funcion funcion) {
        for (NodoParametro p : funcion.parametros()) {
            NodoParametro.Parametro param = (NodoParametro.Parametro) p;
            String tipo = param.tipoPrimitivo() != null ? param.tipoPrimitivo() : param.tipoEstructura();
            tabla.declararVariable(param.nombre(), tipo, param.esArreglo(), param.esArreglo() ? 1 : 0,
                    param.esEstructura(), param.tipoEstructura(), List.of());
        }
    }

    /** Declara una variable simple. */
    public void declararVariable(NodoSentencia.DeclaracionVariable d) {
        if (!tabla.declararVariable(d.nombre(), d.tipo())) {
            reportarDuplicada(d.linea(), d.columna(), d.nombre());
        }
    }

    /** Declara un arreglo 1D. */
    public void declararArreglo(NodoSentencia.DeclaracionArreglo d) {
        boolean ok = tabla.declararVariable(d.nombre(), d.tipo(), true, 1, false, null, List.of(d.tamano()));
        if (!ok) reportarDuplicada(d.linea(), d.columna(), d.nombre());
    }

    /** Declara una matriz 2D. */
    public void declararMatriz(NodoSentencia.DeclaracionMatriz d) {
        boolean ok = tabla.declararVariable(d.nombre(), d.tipo(), true, 2, false, null,
                List.of(d.filas(), d.columnas()));
        if (!ok) reportarDuplicada(d.linea(), d.columna(), d.nombre());
    }

    /** Declara una variable de tipo estructura. */
    public void declararVariableEstructura(NodoSentencia.DeclaracionEstructura d) {
        boolean ok = tabla.declararVariable(d.nombre(), null, false, 0, true, d.tipoEstructura(), List.of());
        if (!ok) reportarDuplicada(d.linea(), d.columna(), d.nombre());
    }

    /** Declara la variable de un ciclo 'para'. */
    public void declararVariableCiclo(String nombre, String tipo, int linea, int columna) {
        if (!tabla.declararVariable(nombre, tipo)) {
            reportarDuplicada(linea, columna, nombre);
        }
    }

    /** Reporta error de declaración duplicada. */
    private void reportarDuplicada(int linea, int columna, String nombre) {
        errores.add(new ErrorSemantico(linea, columna,
                "Declaración duplicada",
                "'" + nombre + "' ya fue declarado en este ámbito"));
    }
}