package com.example.stack_over_pig.piglatin.service;

import com.example.stack_over_pig.piglatin.nodo.NodoPrograma;
import com.example.stack_over_pig.y.errores.ErrorPosicional;
import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;

import java.util.List;

public class ResultadoCompilacionPig {

    private final boolean exitoso;
    private final NodoPrograma programa;
    private final List<ErrorPosicional> erroresLexicos;
    private final List<ErrorPosicional> erroresSintacticos;
    private final List<ErrorSemantico> erroresSemanticos;
    private final List<String> mensajesInternos;

    // RISC-V generado y salida de su ejecución en Venus
    private final String codigoRiscV;
    private final String salidaEjecucionRiscV;

    public ResultadoCompilacionPig(boolean exitoso,
                                   NodoPrograma programa,
                                   List<ErrorPosicional> erroresLexicos,
                                   List<ErrorPosicional> erroresSintacticos,
                                   List<ErrorSemantico> erroresSemanticos,
                                   List<String> mensajesInternos,
                                   String codigoRiscV,
                                   String salidaEjecucionRiscV) {
        this.exitoso = exitoso;
        this.programa = programa;
        this.erroresLexicos = erroresLexicos;
        this.erroresSintacticos = erroresSintacticos;
        this.erroresSemanticos = erroresSemanticos;
        this.mensajesInternos = mensajesInternos;
        this.codigoRiscV = codigoRiscV;
        this.salidaEjecucionRiscV = salidaEjecucionRiscV;
    }

    public boolean isExitoso() { return exitoso; }
    public NodoPrograma getPrograma() { return programa; }
    public List<ErrorPosicional> getErroresLexicos() { return erroresLexicos; }
    public List<ErrorPosicional> getErroresSintacticos() { return erroresSintacticos; }
    public List<ErrorSemantico> getErroresSemanticos() { return erroresSemanticos; }
    public List<String> getMensajesInternos() { return mensajesInternos; }

    public String getCodigoRiscV() { return codigoRiscV; }
    public String getSalidaEjecucionRiscV() { return salidaEjecucionRiscV; }

    public boolean hayErrores() {
        return !erroresLexicos.isEmpty()
                || !erroresSintacticos.isEmpty()
                || !erroresSemanticos.isEmpty()
                || !mensajesInternos.isEmpty();
    }
}