package com.example.stack_over_pig.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        VentanaPrincipal ventana = new VentanaPrincipal(stage);

        Scene scene = new Scene(ventana.getRaiz(), 1280, 800);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/stack_over_pig/ui/estilos.css")
                        .toExternalForm()
        );

        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/stack_over_pig/ui/y-syntax.css")
                        .toExternalForm()
        );
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/stack_over_pig/ui/z-syntax.css")
                        .toExternalForm()
        );
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/example/stack_over_pig/ui/pig-syntax.css")
                        .toExternalForm()
        );

        stage.setTitle("Contacto 3xtrat3rr3str3D");
        stage.setScene(scene);
        stage.show();
    }
}