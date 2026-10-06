package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;

//cuarda las etiquetas de romper/continuar del ciclo activo
//se apilan en el GestorCodigoIntermedio mienstras se traducie un ciclo
public class ContextoCiclo {

    private final int etiquetaContinuar;
    private final int etiquetaRomper;

    public ContextoCiclo(int etiquetaContinuar, int etiquetaRomper) {
        this.etiquetaContinuar = etiquetaContinuar;
        this.etiquetaRomper = etiquetaRomper;
    }

    public int getEtiquetaContinuar() {
        return etiquetaContinuar;
    }

    public int getEtiquetaRomper() {
        return etiquetaRomper;
    }
}
