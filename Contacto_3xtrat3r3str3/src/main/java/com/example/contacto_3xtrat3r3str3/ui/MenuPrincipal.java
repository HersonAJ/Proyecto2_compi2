package com.example.contacto_3xtrat3r3str3.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MenuPrincipal {

    private final HBox barra;

    public Runnable onNuevoArchivo;
    public Runnable onAbrirArchivo;
    public Runnable onAbrirCarpeta;
    public Runnable onGuardar;
    public Runnable onGuardarComo;
    public Runnable onDescargarArchivo;
    public Runnable onDescargarCarpeta;
    public Runnable onSalir;
    public Runnable onAnalizar;
    public Runnable onCompilar;

    public MenuPrincipal(Stage stage) {
        barra = new HBox();
        barra.setStyle("-fx-background-color: #ffffff; -fx-padding: 0;");

        Menu archivo = new Menu("Archivo");
        archivo.getItems().addAll(
                item("Nuevo archivo",         () -> ejecutar(onNuevoArchivo)),
                item("Abrir archivo...",      () -> ejecutar(onAbrirArchivo)),
                item("Abrir carpeta...",      () -> ejecutar(onAbrirCarpeta)),
                new SeparatorMenuItem(),
                item("Guardar",               () -> ejecutar(onGuardar)),
                item("Guardar como...",       () -> ejecutar(onGuardarComo)),
                new SeparatorMenuItem(),
                item("Descargar archivo...",  () -> ejecutar(onDescargarArchivo)),
                item("Descargar carpeta...",  () -> ejecutar(onDescargarCarpeta)),
                new SeparatorMenuItem(),
                item("Salir",                 () -> {
                    if (onSalir != null) onSalir.run();
                    else stage.close();
                })
        );

        MenuBar menuBar = new MenuBar(archivo);
        menuBar.setStyle("-fx-background-color: transparent;");

        // -------- Botones --------
        Button btnAnalizar = new Button("Analizar");
        btnAnalizar.setOnAction(e -> ejecutar(onAnalizar));
        btnAnalizar.setStyle(estiloBoton("#1976D2", "#1565C0"));

        Button btnCompilar = new Button("Compilar");
        btnCompilar.setOnAction(e -> ejecutar(onCompilar));
        btnCompilar.setStyle(estiloBoton("#388E3C", "#2E7D32"));

        barra.getChildren().addAll(menuBar, btnAnalizar, btnCompilar);
    }

    public HBox getBarra() { return barra; }

    private void ejecutar(Runnable r) { if (r != null) r.run(); }

    private static MenuItem item(String texto, Runnable accion) {
        MenuItem mi = new MenuItem(texto);
        if (accion != null) mi.setOnAction(e -> accion.run());
        return mi;
    }

    private static String estiloBoton(String colorBase, String colorHover) {
        return "-fx-background-color: " + colorBase + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 0;" +
                "-fx-border-radius: 0;" +
                "-fx-padding: 6 16 6 16;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: transparent;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;";
    }
}