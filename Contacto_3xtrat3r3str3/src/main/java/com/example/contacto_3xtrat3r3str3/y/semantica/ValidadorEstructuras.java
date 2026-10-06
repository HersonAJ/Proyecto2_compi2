package com.example.contacto_3xtrat3r3str3.y.semantica;

import com.example.contacto_3xtrat3r3str3.y.ast.NodoExpr;
import com.example.contacto_3xtrat3r3str3.y.ast.NodoSentencia;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;
import java.util.Optional;

/**
 * Valida la inicialización de estructuras en .y.
 *
 * Validaciones:
 *   - Cantidad de valores incorrecta   -> validarInicializacionEstructura
 *   - Tipo de valor incompatible       -> validarInicializacionEstructura
 */
public class ValidadorEstructuras {

    private final TablaSimbolos tabla;
    private final List<ErrorSemantico> errores;
    private final ValidadorTipos tipos;

    public ValidadorEstructuras(TablaSimbolos tabla,
                                List<ErrorSemantico> errores,
                                ValidadorTipos tipos) {
        this.tabla = tabla;
        this.errores = errores;
        this.tipos = tipos;
    }

    /**
     * Valida la inicialización de una estructura.
     * Se llama desde ValidadorSemantico en el case DECLARACION_ESTRUCTURA.
     */
    public void validarInicializacionEstructura(NodoSentencia.DeclaracionEstructura d) {
        // Sin inicialización, no hay nada que validar.
        if (d.inicializacion() == null || d.inicializacion().isEmpty()) {
            return;
        }

        // Buscar la estructura en la tabla.
        Optional<TablaSimbolos.DefinicionEstructura> defOpt =
                tabla.buscarEstructura(d.tipoEstructura());

        if (defOpt.isEmpty()) {
            return;
        }

        TablaSimbolos.DefinicionEstructura def = defOpt.get();
        List<String> tiposAtributos = List.copyOf(def.atributos().values());
        List<String> nombresAtributos = List.copyOf(def.atributos().keySet());

        // Verificar cantidad de valores.
        if (d.inicializacion().size() != tiposAtributos.size()) {
            errores.add(new ErrorSemantico(d.linea(), d.columna(),
                    "Cantidad incorrecta en inicialización de estructura",
                    "La estructura '" + d.tipoEstructura() + "' espera "
                            + tiposAtributos.size() + " valores, se encontraron "
                            + d.inicializacion().size()));
            return; // sin cantidad correcta
        }

        // Verificar tipos de cada valor.
        for (int i = 0; i < d.inicializacion().size(); i++) {
            NodoExpr valor = d.inicializacion().get(i);
            String tipoEsperado = tiposAtributos.get(i);
            String nombreAtributo = nombresAtributos.get(i);

            String tipoValor = tipos.tipoDeExpresion(valor);
            if (tipoValor == null) continue;

            if (!tipos.esAsignableA(tipoEsperado, tipoValor)) {
                errores.add(new ErrorSemantico(valor.linea(), valor.columna(),
                        "Tipo incompatible en inicialización de estructura",
                        "El atributo '" + nombreAtributo + "' de '" + d.tipoEstructura()
                                + "' espera '" + tipoEsperado + "', se encontró '" + tipoValor + "'"));
            }
        }
    }
}