package com.example.contacto_3xtrat3r3str3.zetariano.service;


import com.example.contacto_3xtrat3r3str3.y.errores.ErrorPosicional;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.NodoPrograma;

import java.util.List;

public class ResultadoCompilacionZ {

    private final boolean exitoso;
    private final NodoPrograma programa;
    private final String codigoPreprocesado;
    private final TablaSimbolosZ tablaSimbolos;
    private final List<ErrorPosicional> erroresLexicos;
    private final List<ErrorPosicional> erroresSintacticos;
    private final List<ErrorSemantico> erroresSemanticos;
    private final List<String> mensajesInternos;

    public ResultadoCompilacionZ(boolean exitoso,
                                 NodoPrograma programa,
                                 String codigoPreprocesado,
                                 TablaSimbolosZ tablaSimbolos,
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

    public NodoPrograma getPrograma() {
        return programa;
    }

    public String getCodigoPreprocesado() {
        return codigoPreprocesado;
    }

    public TablaSimbolosZ getTablaSimbolos() {
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
