package com.example.contacto_3xtrat3r3str3.ui.modelo;

public enum Lenguaje {
    Y("Y", ".y"),
    ZETARIANO("Zetariano", ".z"),
    PIG_LATIN("Pig Latin", ".pig"),
    DESCONOCIDO("Desconocido", "");

    private final String nombreVisible;
    private final String extension;

    Lenguaje(String nombreVisible, String extension) {
        this.nombreVisible = nombreVisible;
        this.extension = extension;
    }

    public static Lenguaje porExtension(String ruta) {
        if (ruta == null) return DESCONOCIDO;
        String lower = ruta.toLowerCase();
        for (Lenguaje l : values()) {
            if (!l.extension.isEmpty() && lower.endsWith(l.extension)) return l;
        }
        return DESCONOCIDO;
    }

    public String getNombreVisible() { return nombreVisible; }
    public String getExtension() { return extension; }
    public boolean esConocido() { return this != DESCONOCIDO; }
}