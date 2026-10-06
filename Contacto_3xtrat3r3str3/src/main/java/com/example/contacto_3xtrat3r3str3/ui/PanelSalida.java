package com.example.contacto_3xtrat3r3str3.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;

public class PanelSalida extends TabPane {

    // ---------- Fila de error unificada ----------
    public record FilaError(String tipo, int linea, int columna, String mensaje) {}

    private final TextArea txtConsola;
    private final TextArea txtCuartetas;
    private final TableView<FilaError> tablaErrores;
    private final ObservableList<FilaError> modeloErrores = FXCollections.observableArrayList();

    public PanelSalida() {
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

        // 1. Consola
        txtConsola = crearAreaTerminal();
        Tab tabConsola = new Tab("Consola", txtConsola);

        // 2. Errores (tabla)
        tablaErrores = crearTablaErrores();
        Tab tabErrores = new Tab("Errores", tablaErrores);

        // 3. Codigo C
        txtCuartetas = crearAreaTerminal();
        Tab tabCuartetas = new Tab("Codigo C", txtCuartetas);

        getTabs().addAll(tabConsola, tabErrores, tabCuartetas);
    }

    // ---------- Construcción de componentes ----------

    private TextArea crearAreaTerminal() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setFont(Font.font("Consolas", 13));

        // Estilo principal del TextArea
        area.setStyle(
                "-fx-control-inner-background: #1e1e1e;" +
                        "-fx-background-color: #1e1e1e;" +
                        "-fx-text-fill: #d4d4d4;" +
                        "-fx-prompt-text-fill: #888888;" +
                        "-fx-highlight-fill: #264f78;" +
                        "-fx-highlight-text-fill: #ffffff;" +
                        "-fx-border-color: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );

        // Aplicar los colores a las capas internas
        Runnable aplicarEstilos = () -> {

            area.lookupAll(".content").forEach(n -> n.setStyle("-fx-background-color: #1e1e1e;"));
            area.lookupAll(".text").forEach(n -> n.setStyle("-fx-fill: #d4d4d4;"));
            area.lookupAll(".scroll-pane").forEach(n -> n.setStyle("-fx-background-color: #1e1e1e;"));
            area.lookupAll(".viewport").forEach(n -> n.setStyle("-fx-background-color: #1e1e1e;"));
        };

        area.skinProperty().addListener((obs, viejo, nuevo) -> aplicarEstilos.run());
        area.sceneProperty().addListener(
                (obs, vieja, nueva) -> {
                    if (nueva != null) {
                        javafx.application.Platform.runLater(
                                aplicarEstilos
                        );
                    }
                }
        );

        return area;
    }

    @SuppressWarnings("unchecked")
    private TableView<FilaError> crearTablaErrores() {
        TableView<FilaError> tabla = new TableView<>(modeloErrores);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setStyle(
                "-fx-control-inner-background: #1e1e1e;" +
                        "-fx-background-color: #1e1e1e;" +
                        "-fx-table-cell-border-color: #333333;" +
                        "-fx-text-fill: #d4d4d4;" +
                        "-fx-border-color: transparent;"
        );

        TableColumn<FilaError, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tipo()));
        colTipo.setPrefWidth(110);

        TableColumn<FilaError, Number> colLinea = new TableColumn<>("Línea");
        colLinea.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().linea()));
        colLinea.setPrefWidth(70);

        TableColumn<FilaError, Number> colColumna = new TableColumn<>("Columna");
        colColumna.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().columna()));
        colColumna.setPrefWidth(80);

        TableColumn<FilaError, String> colMensaje = new TableColumn<>("Mensaje");
        colMensaje.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mensaje()));
        colMensaje.setPrefWidth(500);

        tabla.getColumns().addAll(colTipo, colLinea, colColumna, colMensaje);
        return tabla;
    }

    // ---------- API pública ----------

    public void limpiarTodo() {
        txtConsola.clear();
        txtCuartetas.clear();
        modeloErrores.clear();
    }

    public void limpiarConsola() { txtConsola.clear(); }
    public void limpiarErrores() { modeloErrores.clear(); }
    public void limpiarCuartetas() { txtCuartetas.clear(); }

    public void imprimirConsola(String mensaje) {
        txtConsola.appendText(mensaje + "\n");
    }

    public void agregarError(String tipo, int linea, int columna, String mensaje) {
        modeloErrores.add(new FilaError(tipo, linea, columna, mensaje));
    }

    public void agregarErrores(Iterable<FilaError> errores) {
        for (FilaError e : errores) {
            modeloErrores.add(e);
        }
    }

    public void mostrarCuartetas(String cuartetas) {
        txtCuartetas.setText(cuartetas);
    }

    public void enfocarErrores() {
        if (!modeloErrores.isEmpty()) {
            getSelectionModel().select(1);
        }
    }

    public int totalErrores() {
        return modeloErrores.size();
    }
}