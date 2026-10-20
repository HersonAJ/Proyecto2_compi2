package com.example.stack_over_pig.riscv;

import java.io.*;
import java.nio.file.*;

/**
 * Ejecuta un archivo .s en Venus y captura la salida.
 * Requiere que venus.jar esté en la raíz del proyecto o en una ruta configurada.
 */
public class EjecutorVenus {

    private final Path rutaVenusJar;

    public EjecutorVenus(Path rutaVenusJar) {
        this.rutaVenusJar = rutaVenusJar;
    }

    public ResultadoEjecucion ejecutar(String codigoRiscV) {
        try {
            Path temp = Files.createTempFile("pig_", ".s");
            Files.writeString(temp, codigoRiscV);

            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-jar", rutaVenusJar.toString(), temp.toString()
            );
            pb.redirectErrorStream(true);

            Process proceso = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            StringBuilder salida = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                salida.append(linea).append("\n");
            }

            int exitCode = proceso.waitFor();
            Files.deleteIfExists(temp);

            return new ResultadoEjecucion(exitCode == 0, salida.toString());
        } catch (Exception e) {
            return new ResultadoEjecucion(false, "Error al ejecutar Venus: " + e.getMessage());
        }
    }

    public record ResultadoEjecucion(boolean exitoso, String salida) {}
}