package com.example.contacto_3xtrat3r3str3.c3d_v2.c;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GeneradorArchivoC {

    private final String nombreArchivo;
    private final String nombreEjecutable;

    public GeneradorArchivoC() {
        this("programa.c", "programa");
    }

    public GeneradorArchivoC(String nombreArchivo, String nombreEjecutable) {
        this.nombreArchivo = nombreArchivo;
        this.nombreEjecutable = nombreEjecutable;
    }

    public boolean generarYCompilar(String codigoC, Path rutaC, Path rutaExe) {
        try {
            Files.writeString(rutaC, codigoC);
            System.out.println("[OK] Archivo .c escrito en: " + rutaC.toAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(
                    "gcc", rutaC.getFileName().toString(), "-o", rutaExe.getFileName().toString()
            );
            pb.directory(rutaC.getParent().toFile());
            pb.redirectErrorStream(true);

            Process proceso = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    System.out.println(linea);
                }
            }

            int exitCode = proceso.waitFor();
            if (exitCode == 0) {
                System.out.println("[OK] Compilación exitosa. Ejecutable: " + rutaExe.toAbsolutePath());
                return true;
            } else {
                System.err.println("[ERROR] gcc salió con código: " + exitCode);
                return false;
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR] Falló la escritura/compilación: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public boolean generarYCompilar(String codigoC) {
        String dirProyecto = System.getProperty("user.dir");
        Path rutaC = Paths.get(dirProyecto, nombreArchivo);
        Path rutaExe = Paths.get(dirProyecto, nombreEjecutable);
        return generarYCompilar(codigoC, rutaC, rutaExe);
    }
}