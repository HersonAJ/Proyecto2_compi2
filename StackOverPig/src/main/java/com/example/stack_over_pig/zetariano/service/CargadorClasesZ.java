package com.example.stack_over_pig.zetariano.service;

import com.example.stack_over_pig.zetariano.semantica.TablaSimbolosZ.DefinicionClaseExterna;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Encuentra en disco las clases .z que una clase necesita y calcula el paquete de un archivo.
 *
 * Zetariano no tiene 'import' ni 'package', asi que:
 *   - el paquete de un archivo es su carpeta relativa a la raiz del proyecto ("zoo", "a.b"; "" en la raiz);
 *   - la superclase de una clase se busca como '<Superclase>.z' en la carpeta de la clase hija.
 */
public class CargadorClasesZ {

    private final Path carpetaRaiz;

    public CargadorClasesZ(Path carpetaRaiz) {
        this.carpetaRaiz = carpetaRaiz.toAbsolutePath().normalize();
    }

    /** Paquete de un archivo: su carpeta relativa a la raiz, con '.' entre carpetas ("" si esta en la raiz). */
    public String paqueteDe(Path archivo) {
        Path carpeta = archivo.toAbsolutePath().normalize().getParent();
        if (carpeta == null || !carpeta.startsWith(carpetaRaiz)) return "";
        return carpetaRaiz.relativize(carpeta).toString().replace(File.separatorChar, '.');
    }

    /**
     * Carga las superclases del archivo (y las de sus superclases, hacia arriba) que no esten ya en
     * 'conocidas'. Devuelve solo las clases nuevas, sin incluir la del propio archivo.
     *
     * Se detiene sin error cuando no encuentra el archivo de una superclase (el analisis semantico
     * reportara "superclase no declarada") o cuando el archivo no declara la clase que se buscaba.
     * Una herencia circular tampoco la cuelga.
     */
    public Map<String, DefinicionClaseExterna> cargarAncestros(Path archivo,
                                                               Map<String, DefinicionClaseExterna> conocidas) {
        Map<String, DefinicionClaseExterna> encontradas = new LinkedHashMap<>();
        Set<Path> visitados = new HashSet<>();

        Path actual = archivo.toAbsolutePath().normalize();
        visitados.add(actual);
        Optional<DefinicionClaseExterna> def = leerFirmas(actual);

        while (def.isPresent() && def.get().superclase() != null) {
            String padre = def.get().superclase();
            if (conocidas.containsKey(padre) || encontradas.containsKey(padre)) break;

            Path archivoPadre = actual.getParent().resolve(padre + ".z").normalize();
            if (!visitados.add(archivoPadre)) break;

            Optional<DefinicionClaseExterna> defPadre = leerFirmas(archivoPadre);
            if (defPadre.isEmpty() || !defPadre.get().nombre().equals(padre)) break;

            encontradas.put(padre, defPadre.get());
            actual = archivoPadre;
            def = defPadre;
        }
        return encontradas;
    }

    private Optional<DefinicionClaseExterna> leerFirmas(Path archivo) {
        if (!Files.isRegularFile(archivo)) return Optional.empty();
        try {
            String contenido = Files.readString(archivo);
            return ServicioCompilacionZ.recolectarFirmas(contenido, paqueteDe(archivo));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}