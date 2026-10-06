package com.example.contacto_3xtrat3r3str3.ui;

import com.example.contacto_3xtrat3r3str3.ui.modelo.Lenguaje;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PanelEditores extends TabPane {

    private final Map<File, Tab> tabsPorArchivo = new HashMap<>();

    public PanelEditores() {
        setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
    }

    public EditorCodigo abrirArchivo(File archivo, String contenido) {
        Tab existente = tabsPorArchivo.get(archivo);
        if (existente != null) {
            getSelectionModel().select(existente);
            EditorCodigo ed = (EditorCodigo) existente.getUserData();
            ed.pedirFoco();
            return ed;
        }

        EditorCodigo editor = new EditorCodigo();
        editor.cargarContenido(contenido, archivo);

        Tab tab = new Tab(archivo.getName(), editor);
        tab.setUserData(editor);
        editor.setOnModificado(() -> marcarModificado(tab));

        tab.setOnCloseRequest(e -> tabsPorArchivo.remove(archivo));

        tabsPorArchivo.put(archivo, tab);
        getTabs().add(tab);
        getSelectionModel().select(tab);
        editor.pedirFoco();
        return editor;
    }

    private void marcarModificado(Tab tab) {
        if (tab == null) return;
        if (!tab.getText().startsWith("* ")) {
            tab.setText("* " + tab.getText());
        }
    }

    public Optional<EditorCodigo> editorActivo() {
        Tab t = getSelectionModel().getSelectedItem();
        if (t == null) return Optional.empty();
        Object data = t.getUserData();
        if (data instanceof EditorCodigo) {
            return Optional.of((EditorCodigo) data);
        }
        return Optional.empty();
    }

    public void marcarGuardado(EditorCodigo editor) {
        editor.marcarComoGuardado();
        for (Tab t : getTabs()) {
            if (t.getUserData() == editor) {
                t.setText(t.getText().replaceFirst("^\\* ", ""));
                break;
            }
        }
    }

    public void cerrarArchivo(File archivo) {
        Tab t = tabsPorArchivo.remove(archivo);
        if (t != null) getTabs().remove(t);
    }

    public EditorCodigo nuevoArchivoVacio(Lenguaje lenguaje) {
        EditorCodigo nuevoEditor = new EditorCodigo(lenguaje);

        String ext = (lenguaje != null && lenguaje.esConocido())
                ? lenguaje.getExtension()
                : ".y";
        String nombreSugerido = "nuevo" + ext;

        Tab nuevaPestana = new Tab(nombreSugerido, nuevoEditor);
        nuevaPestana.setUserData(nuevoEditor);
        nuevoEditor.setOnModificado(() -> marcarModificado(nuevaPestana));

        getTabs().add(nuevaPestana);
        getSelectionModel().select(nuevaPestana);
        nuevoEditor.pedirFoco();
        return nuevoEditor;
    }

    public void actualizarArchivoGuardado(EditorCodigo editor, File nuevoArchivo) {
        editor.marcarComoGuardado();
        editor.setArchivoActual(nuevoArchivo);
        for (Tab t : getTabs()) {
            if (t.getUserData() == editor) {
                t.setText(nuevoArchivo.getName());
                tabsPorArchivo.put(nuevoArchivo, t);
                break;
            }
        }
    }
}