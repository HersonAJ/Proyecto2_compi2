package com.example.stack_over_pig.ui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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

        // 3. Codigo C (renombrado a RISC-V si quieres)
        txtCuartetas = crearAreaTerminal();
        Tab tabCuartetas = new Tab("Codigo RISC-V", txtCuartetas);

        getTabs().addAll(tabConsola, tabErrores, tabCuartetas);

        // Atajo Ctrl+C para copiar errores seleccionados o todos
        configurarAtajoCopiar();
    }

    // ---------- Construcción de componentes ----------

    private TextArea crearAreaTerminal() {
        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(false);
        area.setFont(Font.font("Consolas", 13));

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

        Runnable aplicarEstilos = () -> {
            area.lookupAll(".content").forEach(n -> n.setStyle("-fx-background-color: #1e1e1e;"));
            area.lookupAll(".text").forEach(n -> n.setStyle("-fx-fill: #d4d4d4;"));
            area.lookupAll(".scroll-pane").forEach(n -> n.setStyle("-fx-background-color: #1e1e1e;"));
            area.lookupAll(".viewport").forEach(n -> n.setStyle("-fx-background-color: #1e1e1e;"));
        };

        area.skinProperty().addListener((obs, viejo, nuevo) -> aplicarEstilos.run());
        area.sceneProperty().addListener((obs, vieja, nueva) -> {
            if (nueva != null) {
                javafx.application.Platform.runLater(aplicarEstilos);
            }
        });

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

        // Menú contextual
        ContextMenu menu = new ContextMenu();

        MenuItem copiarSeleccionados = new MenuItem("Copiar seleccionados");
        copiarSeleccionados.setOnAction(e -> copiarErrores(true));
        copiarSeleccionados.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN));

        MenuItem copiarTodo = new MenuItem("Copiar todo");
        copiarTodo.setOnAction(e -> copiarErrores(false));

        menu.getItems().addAll(copiarSeleccionados, copiarTodo);
        tabla.setContextMenu(menu);

        return tabla;
    }

    private void configurarAtajoCopiar() {
        setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.C
                    && getSelectionModel().getSelectedItem() == getTabs().get(1)) {
                copiarErrores(true);
                event.consume();
            }
        });
    }

    /** Copia los errores al portapapeles. Si 'soloSeleccionados' es true y hay selección, copia solo esos. */
    private void copiarErrores(boolean soloSeleccionados) {
        ObservableList<FilaError> aCopiar;
        if (soloSeleccionados && !tablaErrores.getSelectionModel().getSelectedItems().isEmpty()) {
            aCopiar = tablaErrores.getSelectionModel().getSelectedItems();
        } else {
            aCopiar = modeloErrores;
        }

        if (aCopiar.isEmpty()) {
            imprimirConsola("No hay errores para copiar.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("tipo | linea | columna | mensaje\n");
        for (FilaError e : aCopiar) {
            sb.append(e.tipo()).append(" | ")
                    .append(e.linea()).append(" | ")
                    .append(e.columna()).append(" | ")
                    .append(e.mensaje()).append("\n");
        }

        ClipboardContent contenido = new ClipboardContent();
        contenido.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(contenido);

        imprimirConsola("Copiados " + aCopiar.size() + " error(es) al portapapeles.");
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