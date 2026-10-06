package com.example.contacto_3xtrat3r3str3.ui;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GestorArchivos {

    private GestorArchivos() {}

    // ---------- Lectura / escritura ----------

    public static Optional<String> leerArchivo(File archivo) {
        if (archivo == null || !archivo.isFile()) return Optional.empty();
        try {
            String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            return Optional.of(contenido);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean escribirArchivo(File archivo, String contenido) {
        if (archivo == null) return false;
        try {
            Files.writeString(archivo.toPath(), contenido, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Listado recursivo ----------

    public static List<File> listarCarpeta(File carpeta) {
        List<File> resultado = new ArrayList<>();
        if (carpeta == null || !carpeta.isDirectory()) return resultado;
        listarRecursivo(carpeta, resultado);
        return resultado;
    }

    private static void listarRecursivo(File carpeta, List<File> acumulador) {
        File[] hijos = carpeta.listFiles();
        if (hijos == null) return;
        for (File hijo : hijos) {
            acumulador.add(hijo);
            if (hijo.isDirectory()) {
                listarRecursivo(hijo, acumulador);
            }
        }
    }

    // ---------- Copia (descarga) ----------

    public static boolean copiarArchivo(File origen, File destino) {
        if (origen == null || destino == null) return false;
        try {
            Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean copiarCarpeta(File origen, File destino) {
        if (origen == null || destino == null || !origen.isDirectory()) return false;
        try {
            Files.walk(origen.toPath())
                    .forEach(orig -> {
                        try {
                            var rel = origen.toPath().relativize(orig);
                            var dest = destino.toPath().resolve(rel);
                            if (Files.isDirectory(orig)) {
                                Files.createDirectories(dest);
                            } else {
                                Files.createDirectories(dest.getParent());
                                Files.copy(orig, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ---------- Diálogos ----------

    public static Optional<File> dialogoAbrirArchivo(Stage owner) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Abrir archivo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"),
                new FileChooser.ExtensionFilter("Y?", "*.y"),
                new FileChooser.ExtensionFilter("Zetariano", "*.z"),
                new FileChooser.ExtensionFilter("Pig Latin", "*.pig")
        );
        File f = fc.showOpenDialog(owner);
        return Optional.ofNullable(f);
    }

    public static Optional<File> dialogoAbrirCarpeta(Stage owner) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Abrir carpeta");
        File f = dc.showDialog(owner);
        return Optional.ofNullable(f);
    }

    public static Optional<File> dialogoGuardarComo(Stage owner, String nombreSugerido) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar como");
        if (nombreSugerido != null) fc.setInitialFileName(nombreSugerido);
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Y?", "*.y"),
                new FileChooser.ExtensionFilter("Zetariano", "*.z"),
                new FileChooser.ExtensionFilter("Pig Latin", "*.pig"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File f = fc.showSaveDialog(owner);
        return Optional.ofNullable(f);
    }

    public static Optional<File> dialogoDescargarArchivo(Stage owner, String nombreSugerido) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Descargar archivo");
        if (nombreSugerido != null) fc.setInitialFileName(nombreSugerido);
        File f = fc.showSaveDialog(owner);
        return Optional.ofNullable(f);
    }

    public static Optional<File> dialogoDescargarCarpeta(Stage owner) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Descargar carpeta");
        File f = dc.showDialog(owner);
        return Optional.ofNullable(f);
    }
}