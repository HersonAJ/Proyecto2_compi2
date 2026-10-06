package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas;
/*
//contador de temporales t0, t1, etc y etiquetas L0, L1, etc
public class ContadorIds {

    private int contadorTemporales;
    private int contadorEtiquetas;

    public ContadorIds() {
        this.contadorTemporales = 0;
        this.contadorEtiquetas = 0;
    }

    public int siguienteTemporal() {
        return contadorTemporales++;
    }

    public int siguienteEtiqueta() {
        return contadorEtiquetas++;
    }

    public int getTotalTemporales() {
        return contadorTemporales;
    }

    public int getTotalEtiquetas() {
        return contadorEtiquetas;
    }
}
*/

import java.util.ArrayList;
import java.util.List;

/**
 * Contador de temporales (t0, t1, ...) y etiquetas (L0, L1, ...).
 * Además, registra el tipo C de cada temporal para que el generador
 * pueda declararlo correctamente (int, float, char*, etc.).
 */
public class ContadorIds {

    private int contadorTemporales;
    private int contadorEtiquetas;
    private final List<String> tiposTemporales; // tiposTemporales[i] = tipo C de t_i

    public ContadorIds() {
        this.contadorTemporales = 0;
        this.contadorEtiquetas = 0;
        this.tiposTemporales = new ArrayList<>();
    }

    // ===== Temporales =====

    /**
     * Crea un nuevo temporal sin registrar tipo todavía.
     * Se usa cuando no importa el tipo (p. ej. cuando sabemos que será int).
     * Devuelve el número del temporal.
     */
    public int siguienteTemporal() {
        int id = contadorTemporales++;
        tiposTemporales.add("int"); // por defecto int
        return id;
    }

    /**
     * Crea un nuevo temporal registrando ya su tipo C.
     */
    public int siguienteTemporal(String tipoC) {
        int id = contadorTemporales++;
        tiposTemporales.add(tipoC);
        return id;
    }

    /**
     * Registra/corrige el tipo de un temporal ya creado.
     */
    public void registrarTipoTemporal(int idTemporal, String tipoC) {
        if (idTemporal >= 0 && idTemporal < tiposTemporales.size()) {
            tiposTemporales.set(idTemporal, tipoC);
        }
    }

    public String getTipoTemporal(int idTemporal) {
        return tiposTemporales.get(idTemporal);
    }

    public int getTotalTemporales() {
        return contadorTemporales;
    }

    public List<String> getTiposTemporales() {
        return tiposTemporales;
    }

    // ===== Etiquetas =====

    public int siguienteEtiqueta() {
        return contadorEtiquetas++;
    }

    public int getTotalEtiquetas() {
        return contadorEtiquetas;
    }
}