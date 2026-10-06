package com.example.contacto_3xtrat3r3str3.ui;

import com.example.contacto_3xtrat3r3str3.piglatin.service.ResultadoCompilacionPig;
import com.example.contacto_3xtrat3r3str3.piglatin.service.ServicioCompilacionPig;
import com.example.contacto_3xtrat3r3str3.ui.modelo.Lenguaje;
import com.example.contacto_3xtrat3r3str3.y.errores.ErrorPosicional;
import com.example.contacto_3xtrat3r3str3.y.errores.ResultadoCompilacionY;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.y.service.ServicioCompilacionY;
import com.example.contacto_3xtrat3r3str3.zetariano.service.ResultadoCompilacionZ;
import com.example.contacto_3xtrat3r3str3.zetariano.service.ServicioCompilacionZ;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class VentanaPrincipal {

    private final BorderPane raiz;
    private final Stage stage;
    private final ArbolTrabajo arbol;
    private final PanelEditores panelEditores;
    private final MenuPrincipal menu;
    private final PanelSalida panelSalida;
    private final ServicioCompilacionY servicioY = new ServicioCompilacionY();
    private final ServicioCompilacionZ servicioZ = new ServicioCompilacionZ();
    private final ServicioCompilacionPig servicioPig = new ServicioCompilacionPig();

    // Última carpeta usada en el FileChooser de compilación
    private Path ultimaCarpetaSalidaC;

    public VentanaPrincipal(Stage stage) {
        this.stage = stage;
        raiz = new BorderPane();

        menu = new MenuPrincipal(stage);
        raiz.setTop(menu.getBarra());

        arbol = new ArbolTrabajo();
        arbol.setPrefWidth(260);
        arbol.setOnArchivoSeleccionado(this::abrirArchivoEnEditor);
        raiz.setLeft(arbol);

        panelEditores = new PanelEditores();
        panelSalida = new PanelSalida();

        SplitPane splitCentral = new SplitPane(panelEditores, panelSalida);
        splitCentral.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitCentral.setDividerPositions(0.75);
        SplitPane.setResizableWithParent(panelEditores, true);
        SplitPane.setResizableWithParent(panelSalida, true);
        raiz.setCenter(splitCentral);

        Region barraEstado = placeholder("");
        barraEstado.setPrefHeight(24);
        raiz.setBottom(barraEstado);

        // Conectar callbacks del menú
        menu.onNuevoArchivo     = this::accionNuevoArchivo;
        menu.onAbrirArchivo     = this::accionAbrirArchivo;
        menu.onAbrirCarpeta     = this::accionAbrirCarpeta;
        menu.onGuardar          = this::accionGuardar;
        menu.onGuardarComo      = this::accionGuardarComo;
        menu.onDescargarArchivo = this::accionDescargarArchivo;
        menu.onDescargarCarpeta = this::accionDescargarCarpeta;
        menu.onAnalizar         = this::accionAnalizar;
        menu.onCompilar         = this::accionCompilar;
        menu.onSalir            = () -> stage.close();

        // Atajos de teclado
        raiz.sceneProperty().addListener((obs, vieja, nueva) -> {
            if (nueva != null) {
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                        this::accionGuardar);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                        this::accionGuardarComo);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                        this::accionNuevoArchivo);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN),
                        this::accionAbrirArchivo);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN),
                        this::accionAbrirCarpeta);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F5),
                        this::accionAnalizar);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.F6),
                        this::accionCompilar);
                nueva.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN),
                        () -> stage.close());
            }
        });
    }

    public BorderPane getRaiz() { return raiz; }
    public PanelSalida getPanelSalida() { return panelSalida; }

    // Acciones de archivos

    private void accionAbrirArchivo() {
        GestorArchivos.dialogoAbrirArchivo(stage)
                .ifPresent(this::abrirArchivoEnEditor);
    }

    private void accionAbrirCarpeta() {
        GestorArchivos.dialogoAbrirCarpeta(stage)
                .ifPresent(arbol::abrirCarpeta);
    }

    private void abrirArchivoEnEditor(File archivo) {
        GestorArchivos.leerArchivo(archivo).ifPresent(contenido ->
                panelEditores.abrirArchivo(archivo, contenido));
    }

    private void accionGuardar() {
        Optional<EditorCodigo> editorOpt = panelEditores.editorActivo();
        if (editorOpt.isEmpty()) {
            panelSalida.imprimirConsola("No hay pestaña activa.");
            return;
        }
        EditorCodigo editor = editorOpt.get();
        File archivo = editor.getArchivoActual();
        if (archivo == null) {
            accionGuardarComo();
            return;
        }
        if (GestorArchivos.escribirArchivo(archivo, editor.getTexto())) {
            panelEditores.marcarGuardado(editor);
            panelSalida.imprimirConsola("Guardado: " + archivo.getAbsolutePath());
        } else {
            panelSalida.agregarError("UI",-1, -1,"No se pudo guardar el archivo.");
            panelSalida.enfocarErrores();
        }
        arbol.refrescar();
    }

    private void accionGuardarComo() {
        Optional<EditorCodigo> editorOpt = panelEditores.editorActivo();
        if (editorOpt.isEmpty()) {
            panelSalida.imprimirConsola("No hay pestaña activa.");
            return;
        }
        EditorCodigo editor = editorOpt.get();

        String sugerido = "nuevo.y";
        Tab tabActiva = panelEditores.getSelectionModel().getSelectedItem();
        if (tabActiva != null) {
            sugerido = tabActiva.getText().replaceFirst("^\\* ", "");
        } else if (editor.getArchivoActual() != null) {
            sugerido = editor.getArchivoActual().getName();
        }

        File carpetaProyecto = null;
        if (arbol.getRoot() != null) {
            File raizItem = arbol.getRoot().getValue();
            if (raizItem != null) {
                carpetaProyecto = raizItem.isDirectory() ? raizItem : raizItem.getParentFile();
            }
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar como...");
        fileChooser.setInitialFileName(sugerido);

        if (carpetaProyecto != null && carpetaProyecto.exists()) {
            fileChooser.setInitialDirectory(carpetaProyecto);
        }

        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Archivos Y (*.y)", "*.y"),
                new javafx.stage.FileChooser.ExtensionFilter("Archivos Zetariano (*.z)", "*.z"),
                new javafx.stage.FileChooser.ExtensionFilter("Archivos Pig Latin (*.pig)", "*.pig"),
                new javafx.stage.FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        File destino = fileChooser.showSaveDialog(stage);
        if (destino != null) {
            if (GestorArchivos.escribirArchivo(destino, editor.getTexto())) {
                editor.setArchivoActual(destino);
                panelEditores.actualizarArchivoGuardado(editor, destino);
                panelSalida.imprimirConsola("Guardado como: " + destino.getAbsolutePath());
                arbol.refrescar();
            } else {
                panelSalida.agregarError("UI",-1, -1,"No se pudo guardar el archivo.");
                panelSalida.enfocarErrores();
            }
        }
    }

    private void accionDescargarArchivo() {
        Optional<EditorCodigo> editorOpt = panelEditores.editorActivo();
        if (editorOpt.isEmpty()) return;
        EditorCodigo editor = editorOpt.get();
        String sugerido = editor.getArchivoActual() != null
                ? editor.getArchivoActual().getName()
                : "archivo.txt";
        GestorArchivos.dialogoDescargarArchivo(stage, sugerido).ifPresent(destino -> {
            File origen = editor.getArchivoActual();
            if (origen != null) {
                GestorArchivos.copiarArchivo(origen, destino);
            } else {
                GestorArchivos.escribirArchivo(destino, editor.getTexto());
            }
        });
    }

    private void accionDescargarCarpeta() {
        var itemRaiz = arbol.getRoot();
        if (itemRaiz == null) return;
        File origen = itemRaiz.getValue();
        if (origen == null || !origen.isDirectory()) return;
        GestorArchivos.dialogoDescargarCarpeta(stage).ifPresent(destino -> {
            File destinoReal = new File(destino, origen.getName());
            GestorArchivos.copiarCarpeta(origen, destinoReal);
        });
    }

    private void accionNuevoArchivo() {
        java.util.List<Lenguaje> lenguajesDisponibles =
                java.util.List.of(Lenguaje.Y, Lenguaje.ZETARIANO, Lenguaje.PIG_LATIN);
        javafx.scene.control.ChoiceDialog<Lenguaje> dialog =
                new javafx.scene.control.ChoiceDialog<>(Lenguaje.Y, lenguajesDisponibles);
        dialog.setTitle("Nuevo archivo");
        dialog.setHeaderText("Selecciona el lenguaje del nuevo archivo");
        dialog.setContentText("Lenguaje:");

        dialog.showAndWait().ifPresent(lenguajeSeleccionado -> {
            panelEditores.nuevoArchivoVacio(lenguajeSeleccionado);
        });
    }

    // ANALIZAR

    private void accionAnalizar() {
        Optional<EditorCodigo> editorOpt = panelEditores.editorActivo();
        if (editorOpt.isEmpty()) {
            panelSalida.agregarError("UI", -1, -1, "No hay pestaña activa.");
            panelSalida.enfocarErrores();
            return;
        }
        EditorCodigo editor = editorOpt.get();

        if (editor.getArchivoActual() == null) {
            panelSalida.agregarError("UI", -1, -1,
                    "El archivo no está guardado. Guárdalo (Ctrl+S) antes de analizar.");
            panelSalida.enfocarErrores();
            return;
        }

        Lenguaje lenguaje = editor.getLenguaje();
        if (!lenguaje.esConocido()) {
            panelSalida.agregarError("UI", -1, -1,
                    "No se pudo determinar el lenguaje del archivo.");
            panelSalida.enfocarErrores();
            return;
        }

        panelSalida.limpiarTodo();

        if (lenguaje == Lenguaje.Y) {
            analizarY(editor);
        } else if (lenguaje == Lenguaje.ZETARIANO) {
            analizarZ(editor);
        } else if (lenguaje == Lenguaje.PIG_LATIN) {
            analizarPig(editor);
        } else {
            panelSalida.agregarError("UI", -1, -1,
                    "Lenguaje no soportado: " + lenguaje.getNombreVisible());
            panelSalida.enfocarErrores();
        }
    }

    private void analizarY(EditorCodigo editor) {
        panelSalida.imprimirConsola("Analizando " + editor.getArchivoActual().getName() + " como Y?");

        ResultadoCompilacionY resultado = servicioY.analizar(editor.getTexto());

        for (ErrorPosicional e : resultado.getErroresLexicos()) {
            panelSalida.agregarError("Léxico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorPosicional e : resultado.getErroresSintacticos()) {
            panelSalida.agregarError("Sintáctico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorSemantico e : resultado.getErroresSemanticos()) {
            panelSalida.agregarError(e.categoria(), e.linea(), e.columna(), e.mensaje());
        }
        for (String e : resultado.getMensajesInternos()) {
            panelSalida.agregarError("Interno", -1, -1, e);
        }

        if (resultado.isExitoso()) {
            panelSalida.imprimirConsola("Análisis exitoso. Sin errores.");
        } else {
            panelSalida.imprimirConsola("Análisis finalizado con "
                    + panelSalida.totalErrores() + " error(es).");
            panelSalida.enfocarErrores();
        }
    }

    private void analizarZ(EditorCodigo editor) {
        panelSalida.imprimirConsola("Analizando " + editor.getArchivoActual().getName() + " como .z");

        ResultadoCompilacionZ resultado = servicioZ.analizar(editor.getTexto());

        for (ErrorPosicional e : resultado.getErroresLexicos()) {
            panelSalida.agregarError("Léxico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorPosicional e : resultado.getErroresSintacticos()) {
            panelSalida.agregarError("Sintáctico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorSemantico e : resultado.getErroresSemanticos()) {
            panelSalida.agregarError(e.categoria(), e.linea(), e.columna(), e.mensaje());
        }
        for (String e : resultado.getMensajesInternos()) {
            panelSalida.agregarError("Interno", -1, -1, e);
        }

        if (resultado.isExitoso()) {
            panelSalida.imprimirConsola("Análisis exitoso. Sin errores.");
        } else {
            panelSalida.imprimirConsola("Análisis finalizado con "
                    + panelSalida.totalErrores() + " error(es).");
            panelSalida.enfocarErrores();
        }
    }

    private void analizarPig(EditorCodigo editor) {
        panelSalida.imprimirConsola("Analizando " + editor.getArchivoActual().getName() + " como Pig Latin");

        File carpetaRaiz = obtenerCarpetaRaiz(editor);
        if (carpetaRaiz == null) {
            panelSalida.agregarError("UI", -1, -1,
                    "No se pudo determinar la carpeta raíz para las importaciones.");
            panelSalida.enfocarErrores();
            return;
        }

        ResultadoCompilacionPig resultado = servicioPig.analizar(
                editor.getTexto(),
                carpetaRaiz.toPath()
        );

        for (ErrorPosicional e : resultado.getErroresLexicos()) {
            panelSalida.agregarError("Léxico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorPosicional e : resultado.getErroresSintacticos()) {
            panelSalida.agregarError("Sintáctico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorSemantico e : resultado.getErroresSemanticos()) {
            panelSalida.agregarError(e.categoria(), e.linea(), e.columna(), e.mensaje());
        }
        for (String e : resultado.getMensajesInternos()) {
            panelSalida.agregarError("Interno", -1, -1, e);
        }

        if (resultado.isExitoso()) {
            panelSalida.imprimirConsola("Análisis exitoso. Sin errores.");
        } else {
            panelSalida.imprimirConsola("Análisis finalizado con "
                    + panelSalida.totalErrores() + " error(es).");
            panelSalida.enfocarErrores();
        }
    }

    // COMPILAR

    private void accionCompilar() {
        Optional<EditorCodigo> editorOpt = panelEditores.editorActivo();
        if (editorOpt.isEmpty()) {
            panelSalida.agregarError("UI", -1, -1, "No hay pestaña activa.");
            panelSalida.enfocarErrores();
            return;
        }
        EditorCodigo editor = editorOpt.get();

        if (editor.getArchivoActual() == null) {
            panelSalida.agregarError("UI", -1, -1,
                    "El archivo no está guardado. Guárdalo (Ctrl+S) antes de compilar.");
            panelSalida.enfocarErrores();
            return;
        }

        Lenguaje lenguaje = editor.getLenguaje();
        if (lenguaje != Lenguaje.PIG_LATIN) {
            panelSalida.imprimirConsola(
                    "La compilación a C solo está disponible para archivos .pig.");
            return;
        }

        panelSalida.limpiarTodo();

        File carpetaRaiz = obtenerCarpetaRaiz(editor);
        if (carpetaRaiz == null) {
            panelSalida.agregarError("UI", -1, -1,
                    "No se pudo determinar la carpeta raíz para las importaciones.");
            panelSalida.enfocarErrores();
            return;
        }

        // Elegir ruta destino del .c
        Path rutaDestinoC = pedirRutaDestinoC(editor);
        if (rutaDestinoC == null) {
            panelSalida.imprimirConsola("Compilación cancelada.");
            return;
        }

        panelSalida.imprimirConsola("Compilando " + editor.getArchivoActual().getName()
                + " hacia " + rutaDestinoC);

        ResultadoCompilacionPig resultado = servicioPig.compilar(
                editor.getTexto(),
                carpetaRaiz.toPath(),
                rutaDestinoC
        );

        for (ErrorPosicional e : resultado.getErroresLexicos()) {
            panelSalida.agregarError("Léxico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorPosicional e : resultado.getErroresSintacticos()) {
            panelSalida.agregarError("Sintáctico", e.getLinea(), e.getColumna(), e.getMensaje());
        }
        for (ErrorSemantico e : resultado.getErroresSemanticos()) {
            panelSalida.agregarError(e.categoria(), e.linea(), e.columna(), e.mensaje());
        }
        for (String e : resultado.getMensajesInternos()) {
            panelSalida.agregarError("Interno", -1, -1, e);
        }

        if (resultado.getCodigoCGenerado() != null) {
            panelSalida.mostrarCuartetas(resultado.getCodigoCGenerado());
        }

        if (resultado.isExitoso() && resultado.isCompilacionCExitosa()) {
            panelSalida.imprimirConsola("Compilación exitosa.");
            panelSalida.imprimirConsola("Ejecutable: " + resultado.getRutaEjecutable());
        } else if (resultado.isExitoso() && !resultado.isCompilacionCExitosa()) {
            panelSalida.imprimirConsola("El código C se generó pero gcc falló.");
            panelSalida.enfocarErrores();
        } else {
            panelSalida.imprimirConsola("Compilación finalizada con "
                    + panelSalida.totalErrores() + " error(es).");
            panelSalida.enfocarErrores();
        }
    }

    private Path pedirRutaDestinoC(EditorCodigo editor) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar código C generado");

        File archivoPig = editor.getArchivoActual();
        String nombreBase = archivoPig.getName().replaceFirst("\\.[^.]+$", "");
        fc.setInitialFileName(nombreBase + ".c");

        File carpetaInicial;
        if (ultimaCarpetaSalidaC != null && ultimaCarpetaSalidaC.toFile().exists()) {
            carpetaInicial = ultimaCarpetaSalidaC.toFile();
        } else {
            carpetaInicial = archivoPig.getParentFile();
        }
        if (carpetaInicial != null && carpetaInicial.exists()) {
            fc.setInitialDirectory(carpetaInicial);
        }

        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Archivos C (*.c)", "*.c")
        );

        File destino = fc.showSaveDialog(stage);
        if (destino == null) return null;

        if (!destino.getName().toLowerCase().endsWith(".c")) {
            destino = new File(destino.getParentFile(), destino.getName() + ".c");
        }

        ultimaCarpetaSalidaC = destino.getParentFile().toPath();
        return destino.toPath();
    }

    // Helpers

    private File obtenerCarpetaRaiz(EditorCodigo editor) {
        if (arbol.getRoot() != null && arbol.getRoot().getValue() != null) {
            File raiz = arbol.getRoot().getValue();
            if (raiz.isDirectory()) return raiz;
        }
        File archivoActual = editor.getArchivoActual();
        if (archivoActual != null) {
            return archivoActual.getParentFile();
        }
        return null;
    }

    private static Region placeholder(String texto) {
        Label l = new Label(texto);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setMaxHeight(Double.MAX_VALUE);
        l.setStyle("-fx-alignment: center; -fx-text-fill: #888; -fx-border-color: #ccc;");
        return l;
    }
}