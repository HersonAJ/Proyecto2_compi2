/*package com.example.contacto_3xtrat3r3str3.y.errores;

import com.example.contacto_3xtrat3r3str3.y.ast.NodoPrograma;

import java.util.List;

public class ResultadoCompilacionY {

    private final boolean exitoso;
    private final NodoPrograma programa;
    private final String codigoPreprocesado;
    private final List<ErrorPosicional> erroresLexicos;
    private final List<ErrorPosicional> erroresSintacticos;
    private final List<String> mensajesInternos;

    public ResultadoCompilacionY(boolean exitoso,
                                 NodoPrograma programa,
                                 String codigoPreprocesado,
                                 List<ErrorPosicional> erroresLexicos,
                                 List<ErrorPosicional> erroresSintacticos,
                                 List<String> mensajesInternos) {
        this.exitoso = exitoso;
        this.programa = programa;
        this.codigoPreprocesado = codigoPreprocesado;
        this.erroresLexicos = erroresLexicos;
        this.erroresSintacticos = erroresSintacticos;
        this.mensajesInternos = mensajesInternos;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public NodoPrograma getPrograma() {
        return programa;
    }

    public String getCodigoPreprocesado() {
        return codigoPreprocesado;
    }

    public List<ErrorPosicional> getErroresLexicos() {
        return erroresLexicos;
    }

    public List<ErrorPosicional> getErroresSintacticos() {
        return erroresSintacticos;
    }

    public List<String> getMensajesInternos() {
        return mensajesInternos;
    }

    public boolean hayErrores() {
        return !erroresLexicos.isEmpty()
                || !erroresSintacticos.isEmpty()
                || !mensajesInternos.isEmpty();
    }
}
*/

package com.example.contacto_3xtrat3r3str3.y.errores;


import com.example.contacto_3xtrat3r3str3.y.ast.NodoPrograma;
import com.example.contacto_3xtrat3r3str3.y.semantica.TablaSimbolos;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;

import java.util.List;

/**
 * DTO que empaqueta el resultado completo de una compilación de Y?.
 */
public class ResultadoCompilacionY {

    private final boolean exitoso;
    private final NodoPrograma.Programa programa;
    private final String codigoPreprocesado;
    private final TablaSimbolos tablaSimbolos;
    private final List<ErrorPosicional> erroresLexicos;
    private final List<ErrorPosicional> erroresSintacticos;
    private final List<ErrorSemantico> erroresSemanticos;
    private final List<String> mensajesInternos;

    public ResultadoCompilacionY(boolean exitoso,
                                 NodoPrograma.Programa programa,
                                 String codigoPreprocesado,
                                 TablaSimbolos tablaSimbolos,
                                 List<ErrorPosicional> erroresLexicos,
                                 List<ErrorPosicional> erroresSintacticos,
                                 List<ErrorSemantico> erroresSemanticos,
                                 List<String> mensajesInternos) {
        this.exitoso = exitoso;
        this.programa = programa;
        this.codigoPreprocesado = codigoPreprocesado;
        this.tablaSimbolos = tablaSimbolos;
        this.erroresLexicos = erroresLexicos;
        this.erroresSintacticos = erroresSintacticos;
        this.erroresSemanticos = erroresSemanticos;
        this.mensajesInternos = mensajesInternos;
    }

    public boolean isExitoso() {
        return exitoso;
    }

    public NodoPrograma.Programa getPrograma() {   // ← tipo concreto
        return programa;
    }

    public String getCodigoPreprocesado() {
        return codigoPreprocesado;
    }

    public TablaSimbolos getTablaSimbolos() {
        return tablaSimbolos;
    }

    public List<ErrorPosicional> getErroresLexicos() {
        return erroresLexicos;
    }

    public List<ErrorPosicional> getErroresSintacticos() {
        return erroresSintacticos;
    }

    public List<ErrorSemantico> getErroresSemanticos() {
        return erroresSemanticos;
    }

    public List<String> getMensajesInternos() {
        return mensajesInternos;
    }

    public boolean hayErrores() {
        return !erroresLexicos.isEmpty()
                || !erroresSintacticos.isEmpty()
                || !erroresSemanticos.isEmpty()
                || !mensajesInternos.isEmpty();
    }
}