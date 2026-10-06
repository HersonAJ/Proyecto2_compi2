package com.example.contacto_3xtrat3r3str3.zetariano.semantica;

import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Declara símbolos en la tabla para .z.
 *
 * Validaciones:
 *   - Atributo duplicado                    -> declararAtributos
 *   - Constructor con nombre incorrecto     -> declararConstructores
 *   - Constructor duplicado (misma firma)   -> declararConstructores
 *   - Metodo duplicado (misma firma)        -> declararMetodos
 *   - Parámetro duplicado                   -> construirParametros
 *   - Variable local duplicada              -> declararVariable
 */
public class ValidadorDeclaracionesZ {

    private final TablaSimbolosZ tabla;
    private final List<ErrorSemantico> errores;

    public ValidadorDeclaracionesZ(TablaSimbolosZ tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
    }

    /** Declara el nombre de la clase en la tabla. */
    public void declararClase(NodoClase clase) {
        tabla.declararClase(clase.nombre());
    }

    /** Declara todos los atributos de la clase. */
    public void declararAtributos(NodoClase clase) {
        for (NodoAtributoZ a: clase.atributos()) {
            if (!tabla.declararAtributo(a.nombre(), a.tipo(), a.dimensiones())) {
                errores.add(new ErrorSemantico(a.linea(), a.columna(), "Declaracion duplicada",
                        "El atributo '" + a.nombre() + "' ya fue declarado en la clase"));
            }
        }
    }

    /** Declara los constructores validando nombre y firma. */
    public void declararConstructores(NodoClase clase) {
        for (NodoConstructor c : clase.constructores()) {
            // El nombre del constructor debe coincidir con el nombre de la clase.
            if (!c.nombre().equals(clase.nombre())) {
                errores.add(new ErrorSemantico(c.linea(), c.columna(), "Constructor invalido",
                        "'" + c.nombre() + "' no coincide con el nombre de la clase '" + clase.nombre() + "'"));
                continue;
            }

            List<TablaSimbolosZ.Parametro> parametros = construirParametros(c.parametros(), c.nombre());
            if (!tabla.declararConstructor(c.nombre(), parametros)) {
                errores.add(new ErrorSemantico(c.linea(), c.columna(), "Declaracion duplicada",
                        "Ya existe un constructor de '" + c.nombre() + "' con esa misma firma"));
            }
        }
    }

    /** Declara los métodos de la clase. */
    public void declararMetodos(NodoClase clase) {
        for (NodoMetodo m : clase.metodos()) {
            List<TablaSimbolosZ.Parametro> parametros = construirParametros(m.parametros(), m.nombre());
            if (!tabla.declararMetodo(m.nombre(), parametros, m.tipoRetorno())) {
                errores.add(new ErrorSemantico(m.linea(), m.columna(), "Declaracion duplicada",
                        "Ya existe un metodo '" +  m.nombre() + "' con esa misma firma"));
            }
        }
    }

    /** Convierte los nodos de parámetros en la representación de la tabla. */
    private List<TablaSimbolosZ.Parametro> construirParametros(List<NodoParametroZ> nodos, String nombreDueño) {
        List<TablaSimbolosZ.Parametro> parametros = new ArrayList<>();
        for (NodoParametroZ p : nodos) {
            boolean yaExiste = parametros.stream().anyMatch(x -> x.nombre().equals(p.nombre()));
            if (yaExiste) {
                errores.add(new ErrorSemantico(p.linea(), p.columna(), "Declaracion duplicada",
                        "El parametro '" + p.nombre() + "' ya fue declarado en '" + nombreDueño + "'"));
                continue;
            }
            parametros.add(new TablaSimbolosZ.Parametro(p.nombre(), p.tipo(), 0));
        }
        return parametros;
    }

    /** Registra los parámetros como variables en el scope local. */
    public void declararParametrosEnScope(List<NodoParametroZ> parametros) {
        for (NodoParametroZ p : parametros) {
            tabla.declararVariable(p.nombre(), p.tipo());
        }
    }

    /** Declara una variable local calculando su tamaño si es arreglo. */
    public void declararVariable(NodoSentencia.DeclaracionVariable d) {
        Integer tamanoConocido = calcularTamanoConocido(d.inicializacion());
        if (!tabla.declararVariable(d.nombre(), d.tipo(), d.dimensiones(), tamanoConocido)) {
            errores.add(new ErrorSemantico(d.linea(), d.columna(), "Declaracion duplicada",
                    "'" + d.nombre() + "' ya fue declarado en este ambito"));
        }
    }

    /** Calcula el tamaño conocido de un arreglo a partir de su inicializador. */
    private Integer calcularTamanoConocido(NodoExpr inicializacion) {
        if (inicializacion == null) return null;

        if (inicializacion instanceof NodoExpr.ListaLiteral lista) {
            return lista.elementos().size(); // '{10, 20, 30}' -> tamaño 3
        }
        if (inicializacion instanceof NodoExpr.ArregloNuevo arreglo && !arreglo.dimensiones().isEmpty()) {
            NodoExpr primeraDimension = arreglo.dimensiones().get(0);
            if (primeraDimension instanceof NodoExpr.LiteralEntero lit) {
                return lit.valor(); // 'new int[5]' -> tamaño 5
            }
        }
        return null;
    }
}