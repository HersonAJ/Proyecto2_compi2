package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Valida el uso de objetos importados de .z.
 *
 * Validaciones:
 *   - Constructor no compatible con los argumentos   -> validarInstanciacion
 *   - Metodo no compatible con los argumentos        -> validarLlamadaMetodo
 *   - Llamada a metodo sobre algo que no es objeto   -> validarLlamadaMetodo
 *   - Atributo de objeto inexistente                 -> validarAccesoAtributoObjeto
 */
public class ValidadorObjetosPig {

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;
    private final ValidadorTiposPig tipos;

    public ValidadorObjetosPig(TablaSimbolosPig tabla,
                               List<ErrorSemantico> errores,
                               ValidadorTiposPig tipos) {
        this.tabla = tabla;
        this.errores = errores;
        this.tipos = tipos;
    }

    // INSTANCIACION DE OBJETOS
    // Verifica que exista un constructor compatible con los argumentos
    public void validarInstanciacion(NodoExpr.InstanciaObjeto inst) {
        Optional<TablaSimbolosPig.DefinicionClase> claseOpt = tabla.buscarClase(inst.tipoClase());
        if (claseOpt.isEmpty()) return;

        TablaSimbolosPig.DefinicionClase clase = claseOpt.get();

        // Construir lista de tipos de argumentos.
        List<ValidadorTiposPig.TipoResuelto> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : inst.argumentos()) {
            tiposArgs.add(tipos.tipoDeExpresion(arg));
        }

        // Buscar un constructor que coincida.
        resolverSobrecarga(
                clase.constructores(), tiposArgs,
                inst.linea(), inst.columna(), clase.nombre(), "Constructor");
    }

    // LLAMADA A METODO

    /**
     * Valida 'miObjeto.getNombre()'.
     * Verifica que el objeto sea de una clase conocida y que exista un metodo compatible.
     * Devuelve el tipo de retorno del metodo.
     */
    public ValidadorTiposPig.TipoResuelto validarLlamadaMetodo(NodoExpr.LlamadaMetodo llamada) {
        // 1. Resolver el tipo del objeto.
        ValidadorTiposPig.TipoResuelto tipoObjeto = tipos.tipoDeExpresion(llamada.objeto());

        if (tipoObjeto == null) {
            tipoObjeto = resolverTipoDeObjeto(llamada.objeto());
            if (tipoObjeto == null) return null;
        }

        // 2. Buscar la clase.
        Optional<TablaSimbolosPig.DefinicionClase> claseOpt = tabla.buscarClase(tipoObjeto.base());
        if (claseOpt.isEmpty()) {
            errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(),
                    "Acceso inválido",
                    "No se puede llamar a '" + llamada.nombre()
                            + "' porque '" + tipoObjeto.base() + "' no es un objeto"));
            return null;
        }

        TablaSimbolosPig.DefinicionClase clase = claseOpt.get();

        // 3. Construir tipos de argumentos.
        List<ValidadorTiposPig.TipoResuelto> tiposArgs = new ArrayList<>();
        for (NodoExpr arg : llamada.argumentos()) {
            tiposArgs.add(tipos.tipoDeExpresion(arg));
        }

        // 4. Buscar un metodo que coincida.
        TablaSimbolosPig.Firma firma = resolverSobrecarga(
                clase.metodos(), tiposArgs,
                llamada.linea(), llamada.columna(), llamada.nombre(), "Método");

        if (firma == null) return null;

        // 5. Devolver el tipo de retorno.
        return firma.tipoRetorno() == null ? null : new ValidadorTiposPig.TipoResuelto(firma.tipoRetorno(), 0);
    }

    // ACCESO A ATRIBUTO DE OBJETO
    /**
     * Valida 'miObjeto.nombre'.
     * Devuelve el tipo del atributo.
     */
    public ValidadorTiposPig.TipoResuelto validarAccesoAtributoObjeto(NodoExpr.AccesoAtributo acceso) {
        ValidadorTiposPig.TipoResuelto tipoObjeto = tipos.tipoDeExpresion(acceso.objeto());

        if (tipoObjeto == null) {
            tipoObjeto = resolverTipoDeObjeto(acceso.objeto());
            if (tipoObjeto == null) return null;
        }

        Optional<TablaSimbolosPig.DefinicionClase> claseOpt = tabla.buscarClase(tipoObjeto.base());
        if (claseOpt.isEmpty()) {
            return null;
        }

        TablaSimbolosPig.DefinicionClase clase = claseOpt.get();
        TablaSimbolosPig.AtributoClase atributo = clase.atributos().get(acceso.atributo());

        if (atributo == null) {
            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                    "Atributo no encontrado",
                    "La clase '" + tipoObjeto.base()
                            + "' no tiene un atributo '" + acceso.atributo() + "'"));
            return null;
        }

        return new ValidadorTiposPig.TipoResuelto(atributo.tipo(), atributo.dimensiones());
    }

    // HELPERS
    /** Resuelve la sobrecarga que coincide con los tipos de argumentos. */
    private TablaSimbolosPig.Firma resolverSobrecarga(List<TablaSimbolosPig.Firma> firmas,
                                                      List<ValidadorTiposPig.TipoResuelto> tiposArgs,
                                                      int linea, int columna,
                                                      String nombre, String tipoElemento) {
        if (firmas.isEmpty()) {
            errores.add(new ErrorSemantico(linea, columna,
                    tipoElemento + " no declarado",
                    "'" + nombre + "' no existe en la clase importada"));
            return null;
        }

        if (tiposArgs.stream().anyMatch(java.util.Objects::isNull)) return null;

        // 1. Coincidencia exacta.
        for (TablaSimbolosPig.Firma f : firmas) {
            if (coincideExacto(f, tiposArgs)) return f;
        }

        // 2. Coincidencia con conversión implícita (int → double).
        List<TablaSimbolosPig.Firma> candidatas = new ArrayList<>();
        for (TablaSimbolosPig.Firma f : firmas) {
            if (coincideConConversion(f, tiposArgs)) candidatas.add(f);
        }

        if (candidatas.size() == 1) return candidatas.get(0);
        if (candidatas.size() > 1) {
            errores.add(new ErrorSemantico(linea, columna,
                    "Llamada ambigua",
                    "Más de una sobrecarga de '" + nombre + "' coincide con estos argumentos"));
            return null;
        }

        errores.add(new ErrorSemantico(linea, columna,
                "Llamada inválida",
                "Ninguna sobrecarga de '" + nombre + "' coincide con estos argumentos"));
        return null;
    }

    /** Compara firma y argumentos por igualdad exacta de tipos. */
    private boolean coincideExacto(TablaSimbolosPig.Firma f,
                                   List<ValidadorTiposPig.TipoResuelto> tiposArgs) {
        if (f.parametros().size() != tiposArgs.size()) return false;
        for (int i = 0; i < tiposArgs.size(); i++) {
            TablaSimbolosPig.Parametro p = f.parametros().get(i);
            if (!tipos.sonTiposEquivalentes(p.tipo(), tiposArgs.get(i).base())) return false;
            if (p.dimensiones() != tiposArgs.get(i).dimensiones()) return false;
        }
        return true;
    }

    /** Compara firma y argumentos permitiendo conversión implícita. */
    private boolean coincideConConversion(TablaSimbolosPig.Firma f,
                                          List<ValidadorTiposPig.TipoResuelto> tiposArgs) {
        if (f.parametros().size() != tiposArgs.size()) return false;
        for (int i = 0; i < tiposArgs.size(); i++) {
            TablaSimbolosPig.Parametro p = f.parametros().get(i);
            ValidadorTiposPig.TipoResuelto destino = new ValidadorTiposPig.TipoResuelto(p.tipo(), p.dimensiones());
            if (!tipos.esAsignable(destino, tiposArgs.get(i))) return false;
        }
        return true;
    }

    /** Resuelve el tipo de una variable que es un objeto. */
    private ValidadorTiposPig.TipoResuelto resolverTipoDeObjeto(NodoExpr expr) {
        if (expr instanceof NodoExpr.Identificador id) {
            Optional<TablaSimbolosPig.SimboloVariable> simbolo = tabla.buscarVariable(id.nombre());
            if (simbolo.isPresent()) {
                TablaSimbolosPig.SimboloVariable v = simbolo.get();
                if (v.esObjeto()) {
                    return new ValidadorTiposPig.TipoResuelto(v.tipo(), 0);
                }
            }
        }
        return null;
    }
}