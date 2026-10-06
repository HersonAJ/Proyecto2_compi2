package com.example.contacto_3xtrat3r3str3.ui;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;

import java.io.File;
import java.util.function.Consumer;

public class ArbolTrabajo extends TreeView<File> {

    private Consumer<File> onArchivoSeleccionado;

    public ArbolTrabajo() {
        setShowRoot(true);
        setCellFactory(tv -> new CeldaArchivoProyecto());

        // Tecla Enter para abrir archivo seleccionado
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                dispararSeleccion();
            }
        });
    }

    public void setOnArchivoSeleccionado(Consumer<File> callback) {
        this.onArchivoSeleccionado = callback;
    }

    public void abrirCarpeta(File carpeta) {
        if (carpeta == null || !carpeta.isDirectory()) return;
        TreeItem<File> raiz = construirItem(carpeta);
        raiz.setExpanded(true);
        setRoot(raiz);
    }

    // Método vital para actualizar el árbol cuando creas o guardas un archivo nuevo
    public void refrescar() {
        TreeItem<File> raizActual = getRoot();
        if (raizActual != null && raizActual.getValue() != null) {
            abrirCarpeta(raizActual.getValue());
        }
    }

    private TreeItem<File> construirItem(File archivo) {
        TreeItem<File> item = new TreeItem<>(archivo);
        if (archivo.isDirectory()) {
            File[] hijos = archivo.listFiles();
            if (hijos != null) {
                for (File hijo : hijos) {
                    // Ocultar carpetas de sistema o control de versiones pesadas
                    if (!hijo.isHidden() && !hijo.getName().equals(".git")) {
                        item.getChildren().add(construirItem(hijo));
                    }
                }
            }
        }
        return item;
    }

    private void dispararSeleccion() {
        TreeItem<File> item = getSelectionModel().getSelectedItem();
        if (item == null) return;
        File f = item.getValue();
        // Solo actúa si es un archivo de código válido, ignorando las carpetas
        if (f != null && f.isFile() && onArchivoSeleccionado != null) {
            onArchivoSeleccionado.accept(f);
        }
    }

    // Celda personalizada para manejar iconos o nombres limpios
    private class CeldaArchivoProyecto extends TreeCell<File> {
        public CeldaArchivoProyecto() {
            // Doble clic nativo en la celda del archivo
            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !isEmpty()) {
                    File archivo = getItem();
                    if (archivo != null && archivo.isFile() && onArchivoSeleccionado != null) {
                        onArchivoSeleccionado.accept(archivo);
                    }
                }
            });
        }

        @Override
        protected void updateItem(File archivo, boolean vacio) {
            super.updateItem(archivo, vacio);
            if (vacio || archivo == null) {
                setText(null);
                setGraphic(null);
            } else {
                String nombre = archivo.getName();
                // Solución para unidades de disco en Windows (ej: "C:\")
                if (nombre.isEmpty()) {
                    nombre = archivo.getAbsolutePath();
                }
                setText(nombre);

                // Aquí puedes cambiar visualmente el icono dependiendo de si es carpeta o archivo
                if (archivo.isDirectory()) {
                    setStyle("-fx-text-fill: #2C7BE5;"); // Color distintivo para carpetas
                } else {
                    setStyle("-fx-text-fill: #2E3A45;"); // Color para archivos
                }
                setGraphic(null);
            }
        }
    }
}