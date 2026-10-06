package com.example.contacto_3xtrat3r3str3.ui;

import com.example.contacto_3xtrat3r3str3.coloracion.PigHighlighter;
import com.example.contacto_3xtrat3r3str3.coloracion.YHighlighter;
import com.example.contacto_3xtrat3r3str3.coloracion.ZHighlighter;
import com.example.contacto_3xtrat3r3str3.ui.modelo.Lenguaje;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.time.Duration;

public class EditorCodigo extends HBox {

    private final CodeArea areaEdicion;

    private File archivoActual;
    private Lenguaje lenguaje = Lenguaje.DESCONOCIDO;
    private boolean modificado = false;
    private Runnable onModificado;

    public EditorCodigo() {
        this(Lenguaje.DESCONOCIDO);
    }

    public EditorCodigo(Lenguaje lenguajeInicial) {

        if (lenguajeInicial != null) {
            this.lenguaje = lenguajeInicial;
        }

        // Editor de código
        areaEdicion = new CodeArea();

        // Fuente y apariencia del editor
        areaEdicion.setStyle(
                "-fx-font-family: 'Consolas';" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-color: white;"
        );

        // Números de línea sincronizados con los párrafos
        areaEdicion.setParagraphGraphicFactory(
                LineNumberFactory.get(areaEdicion)
        );

        areaEdicion.setWrapText(false);
        HBox.setHgrow(areaEdicion, Priority.ALWAYS);

        // RESALTADO
        // Recalcular el highlighting cada 300ms tras la última pulsación.
        areaEdicion.multiPlainChanges()
                .successionEnds(Duration.ofMillis(300))
                .subscribe(ignore -> aplicarResaltado());
        getChildren().add(areaEdicion);
        areaEdicion.textProperty().addListener(
                (obs, viejo, nuevo) -> {
                    if (!modificado) {
                        modificado = true;
                        if (onModificado != null) {
                            onModificado.run();
                        }
                    }
                }
        );

        Platform.runLater(areaEdicion::requestFocus);
    }

    // RESALTADO
    private void aplicarResaltado() {
        String texto = areaEdicion.getText();
        if (texto == null || texto.isEmpty()) {
            return;
        }

        int caret = areaEdicion.getCaretPosition();
        int anchor = areaEdicion.getAnchor();

        switch (lenguaje) {
            case Y -> areaEdicion.setStyleSpans(0, YHighlighter.computeHighlighting(texto));
            case ZETARIANO -> areaEdicion.setStyleSpans(0, ZHighlighter.computeHighlighting(texto));
            case PIG_LATIN -> areaEdicion.setStyleSpans(0, PigHighlighter.computeHighlighting(texto));
            default -> { /* sin resaltado */ }
        }

        try {
            areaEdicion.selectRange(anchor, caret);
        } catch (Exception ignored) {
        }
    }

    // ---------- API ----------

    public void cargarContenido(String contenido, File archivo) {

        this.archivoActual = archivo;

        if (archivo != null) {
            Lenguaje detectado =
                    Lenguaje.porExtension(archivo.getName());

            if (detectado.esConocido()) {
                this.lenguaje = detectado;
            }
        }

        areaEdicion.replaceText(contenido != null ? contenido : "");
        modificado = false;
        Platform.runLater(this::aplicarResaltado);
    }

    public void marcarComoGuardado() {
        this.modificado = false;
    }

    public boolean estaModificado() {
        return modificado;
    }

    public File getArchivoActual() {
        return archivoActual;
    }

    public void setArchivoActual(File archivoActual) {

        this.archivoActual = archivoActual;

        if (archivoActual != null) {
            Lenguaje detectado =
                    Lenguaje.porExtension(archivoActual.getName());

            if (detectado.esConocido()) {
                this.lenguaje = detectado;
            }
        }
    }

    public Lenguaje getLenguaje() {
        return lenguaje;
    }

    public void setLenguaje(Lenguaje lenguaje) {
        if (lenguaje != null) {
            this.lenguaje = lenguaje;
        }
    }

    public void setOnModificado(Runnable callback) {
        this.onModificado = callback;
    }

    public CodeArea getAreaEdicion() {
        return areaEdicion;
    }

    public String getTexto() {
        return areaEdicion.getText();
    }

    public void setTexto(String texto) {
        areaEdicion.replaceText(texto != null ? texto : "");
        Platform.runLater(this::aplicarResaltado);
    }

    public void pedirFoco() {
        Platform.runLater(areaEdicion::requestFocus);
    }
}