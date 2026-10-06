package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoExpr;
import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;
import java.util.Optional;

/**
 * Valida el uso de estructuras importadas de .y.
 *
 * Validaciones:
 *   - Cantidad incorrecta en inicialización    -> validarInicializacion
 *   - Tipo incompatible en inicialización      -> validarInicializacion
 *   - Acceso a atributo en algo que no es struct -> validarAccesoAtributo
 *   - Atributo inexistente                     -> validarAccesoAtributo
 */
public class ValidadorEstructurasPig {

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;
    private final ValidadorTiposPig tipos;

    public ValidadorEstructurasPig(TablaSimbolosPig tabla,
                                   List<ErrorSemantico> errores,
                                   ValidadorTiposPig tipos) {
        this.tabla = tabla;
        this.errores = errores;
        this.tipos = tipos;
    }

    // INICIALIZACION DE ESTRUCTURA
    /**
     * Valida la inicialización: 'esto miDir : Direccion {"Calle Real", 42}'
     * Verifica cantidad y tipo de cada valor contra los atributos.
     */
    public void validarInicializacion(NodoSentencia.DeclaracionStruct d) {
        if (d.inicializacion() == null || d.inicializacion().isEmpty()) {
            return;
        }

        Optional<TablaSimbolosPig.DefinicionEstructura> defOpt =
                tabla.buscarEstructura(d.tipo());

        if (defOpt.isEmpty()) return;

        TablaSimbolosPig.DefinicionEstructura def = defOpt.get();
        List<String> tiposAtributos = List.copyOf(def.atributos().values());
        List<String> nombresAtributos = List.copyOf(def.atributos().keySet());

        // Verificar cantidad.
        if (d.inicializacion().size() != tiposAtributos.size()) {
            errores.add(new ErrorSemantico(d.linea(), d.columna(),
                    "Cantidad incorrecta en inicialización de estructura",
                    "La estructura '" + d.tipo() + "' espera " + tiposAtributos.size()
                            + " valores, se encontraron " + d.inicializacion().size()));
            return;
        }

        // Verificar tipo de cada valor.
        for (int i = 0; i < d.inicializacion().size(); i++) {
            NodoExpr valor = d.inicializacion().get(i);
            String tipoEsperado = tiposAtributos.get(i);
            String nombreAtributo = nombresAtributos.get(i);

            ValidadorTiposPig.TipoResuelto tipoValor = tipos.tipoDeExpresion(valor);
            if (tipoValor == null) continue;

            ValidadorTiposPig.TipoResuelto esperado = new ValidadorTiposPig.TipoResuelto(tipoEsperado, 0);
            if (!tipos.esAsignable(esperado, tipoValor)) {
                errores.add(new ErrorSemantico(valor.linea(), valor.columna(),
                        "Tipo incompatible en inicialización de estructura",
                        "El atributo '" + nombreAtributo + "' de '" + d.tipo()
                                + "' espera '" + tipoEsperado + "', se encontró '" + tipoValor.base() + "'"));
            }
        }
    }

    // ACCESO A ATRIBUTO
    /**
     * Verifica que el objeto sea estructura y que el atributo exista.
     * Devuelve el tipo del atributo.
     */
    public ValidadorTiposPig.TipoResuelto validarAccesoAtributo(NodoExpr.AccesoAtributo acceso) {
        // Resolver el tipo del objeto base.
        ValidadorTiposPig.TipoResuelto tipoObjeto = tipos.tipoDeExpresion(acceso.objeto());

        if (tipoObjeto == null) {
            tipoObjeto = resolverTipoDeEstructura(acceso.objeto());
            if (tipoObjeto == null) return null;
        }

        // Buscar la estructura.
        Optional<TablaSimbolosPig.DefinicionEstructura> defOpt =
                tabla.buscarEstructura(tipoObjeto.base());

        if (defOpt.isEmpty()) {
            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                    "Acceso inválido",
                    "No se puede acceder a '" + acceso.atributo()
                            + "' porque '" + tipoObjeto.base() + "' no es una estructura"));
            return null;
        }

        // Buscar el atributo.
        TablaSimbolosPig.DefinicionEstructura def = defOpt.get();
        String tipoAtributo = def.atributos().get(acceso.atributo());

        if (tipoAtributo == null) {
            errores.add(new ErrorSemantico(acceso.linea(), acceso.columna(),
                    "Atributo no encontrado",
                    "La estructura '" + tipoObjeto.base()
                            + "' no tiene un atributo '" + acceso.atributo() + "'"));
            return null;
        }

        return new ValidadorTiposPig.TipoResuelto(tipoAtributo, 0);
    }

    // HELPERS
    private ValidadorTiposPig.TipoResuelto resolverTipoDeEstructura(NodoExpr expr) {
        if (expr instanceof NodoExpr.Identificador id) {
            Optional<TablaSimbolosPig.SimboloVariable> simbolo = tabla.buscarVariable(id.nombre());
            if (simbolo.isPresent()) {
                TablaSimbolosPig.SimboloVariable v = simbolo.get();
                if (v.esEstructura()) {
                    return new ValidadorTiposPig.TipoResuelto(v.tipo(), 0);
                }
            }
        }
        return null;
    }
}