package com.example.stack_over_pig.piglatin.riscv;

import java.io.*;
import java.nio.file.*;

/**
 * Ejecuta un archivo .s en Venus y captura la salida.
 * Busca venus-jvm-latest.jar en:
 *   1. La ruta indicada en el constructor.
 *   2. lib/venus-jvm-latest.jar (raíz del proyecto).
 *   3. src/main/resources/venus-jvm-latest.jar.
 *   4. venus-jvm-latest.jar (raíz del proyecto).
 *
 * Considera exitosa la ejecución si la salida no contiene errores del
 * ensamblador ni excepciones. Venus puede devolver un exitCode distinto
 * de 0 aunque la ejecución sea correcta (p. ej. cuando imprime el resumen
 * al final).
 */
public class EjecutorVenus {

    private static final String NOMBRE_JAR = "venus-jvm-latest.jar";

    private final Path rutaVenusJar;

    public EjecutorVenus(Path rutaVenusJar) {
        this.rutaVenusJar = resolverRutaVenus(rutaVenusJar);
    }

    public EjecutorVenus() {
        this(null);
    }

    /** Busca el JAR en varios sitios si la ruta indicada no existe. */
    private static Path resolverRutaVenus(Path indicada) {
        if (indicada != null && Files.isRegularFile(indicada)) return indicada;

        Path[] candidatos = {
                Path.of("lib", NOMBRE_JAR),
                Path.of("src", "main", "resources", NOMBRE_JAR),
                Path.of(NOMBRE_JAR)
        };

        for (Path c : candidatos) {
            if (Files.isRegularFile(c)) return c.toAbsolutePath();
        }

        return indicada != null ? indicada : Path.of("lib", NOMBRE_JAR).toAbsolutePath();
    }

    public ResultadoEjecucion ejecutar(String codigoRiscV) {
        if (!Files.isRegularFile(rutaVenusJar)) {
            return new ResultadoEjecucion(false,
                    "No se encontró " + NOMBRE_JAR + ". Colócalo en 'lib/" + NOMBRE_JAR
                            + "' o en 'src/main/resources/" + NOMBRE_JAR + "'.\n"
                            + "Ruta buscada: " + rutaVenusJar.toAbsolutePath());
        }

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

            proceso.waitFor();
            Files.deleteIfExists(temp);

            String salidaStr = salida.toString();
            boolean hayError = salidaStr.contains("Could not assemble")
                    || salidaStr.contains("AssemblerError")
                    || salidaStr.contains("could not find label")
                    || salidaStr.contains("Exception")
                    || salidaStr.contains("Error al ejecutar");

            return new ResultadoEjecucion(!hayError, salidaStr);
        } catch (Exception e) {
            return new ResultadoEjecucion(false, "Error al ejecutar Venus: " + e.getMessage());
        }
    }

    public record ResultadoEjecucion(boolean exitoso, String salida) {}
}