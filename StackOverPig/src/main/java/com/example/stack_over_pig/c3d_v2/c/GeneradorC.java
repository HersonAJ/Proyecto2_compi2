package com.example.stack_over_pig.c3d_v2.c;

import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.Cuarteta;

import java.util.List;

public class GeneradorC {

    private final StringBuilder sb;

    public GeneradorC() {
        this.sb = new StringBuilder();
    }

    public String generar(List<FuncionC> funciones,
                          List<EstructuraC> estructuras,
                          List<ParametroC> variablesGlobales,
                          boolean incluirMain) {
        sb.setLength(0);

        escribirCabeceraArchivo();
        escribirEstructuras(estructuras);
        escribirVariablesGlobales(variablesGlobales);
        escribirPrototipos(funciones);

        for (FuncionC f : funciones) {
            escribirFuncion(f);
        }

        if (incluirMain) {
            escribirMain(funciones);
        }

        return sb.toString();
    }

    private void escribirCabeceraArchivo() {
        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <string.h>\n\n");

        // Función auxiliar para concatenar cadenas.
        sb.append("char* concat(char* a, char* b) {\n");
        sb.append("    char* r = malloc(strlen(a) + strlen(b) + 1);\n");
        sb.append("    strcpy(r, a);\n");
        sb.append("    strcat(r, b);\n");
        sb.append("    return r;\n");
        sb.append("}\n\n");

        // Helpers de conversión a string para concatenación.
        sb.append("char* z_to_string_int(int x) {\n");
        sb.append("    char* buf = (char*) malloc(32);\n");
        sb.append("    sprintf(buf, \"%d\", x);\n");
        sb.append("    return buf;\n");
        sb.append("}\n\n");

        sb.append("char* z_to_string_double(double x) {\n");
        sb.append("    char* buf = (char*) malloc(32);\n");
        sb.append("    sprintf(buf, \"%g\", x);\n");
        sb.append("    return buf;\n");
        sb.append("}\n\n");

        sb.append("char* z_to_string_char(char x) {\n");
        sb.append("    char* buf = (char*) malloc(4);\n");
        sb.append("    sprintf(buf, \"%c\", x);\n");
        sb.append("    return buf;\n");
        sb.append("}\n\n");

        sb.append("char* z_to_string_bool(int x) {\n");
        sb.append("    return x ? (char*)\"true\" : (char*)\"false\";\n");
        sb.append("}\n\n");
    }

    private void escribirEstructuras(List<EstructuraC> estructuras) {
        for (EstructuraC e : estructuras) {
            e.aCodigoC(sb);
        }
    }

    private void escribirPrototipos(List<FuncionC> funciones) {
        for (FuncionC f : funciones) {
            f.escribirCabecera(sb);
            sb.append(";\n");
        }
        sb.append('\n');
    }

    private void escribirFuncion(FuncionC f) {
        // Structs locales (renombrados) van antes de la función.
        for (EstructuraC e : f.getEstructurasLocales()) {
            e.aCodigoC(sb);
        }
        f.escribirCabecera(sb);
        sb.append(" {\n");

        // 1. Declaración de variables locales.
        for (VariableLocalC v : f.getVariablesLocales()) {
            sb.append("    ");
            v.aCodigoC(sb);
        }
        if (!f.getVariablesLocales().isEmpty()) {
            sb.append('\n');
        }

        // 2. Declaración de temporales.
        List<String> tipos = f.getTiposTemporales();
        for (int i = 0; i < tipos.size(); i++) {
            String tipoTemp = tipos.get(i);
            if ("cadena".equals(tipoTemp) || "textum".equals(tipoTemp) || "String".equals(tipoTemp)) {
                sb.append("    char* t").append(i).append(";\n");
            } else if (esTipoC(tipoTemp)) {
                // El tipo ya viene en formato C (struct X*, int, double, char, etc.)
                sb.append("    ").append(tipoTemp)
                        .append(" t").append(i).append(";\n");
            } else {
                // Tipo del lenguaje fuente (entero, flotante, etc.) -> traducir.
                sb.append("    ").append(TipoC.primitivoAC(tipoTemp))
                        .append(" t").append(i).append(";\n");
            }
        }
        if (!tipos.isEmpty()) {
            sb.append('\n');
        }

        // 3. Cuádruplas.
        for (Cuarteta c : f.getCuartetas()) {
            c.aCodigoC(sb);
        }

        sb.append("}\n\n");
    }

    private void escribirMain(List<FuncionC> funciones) {
        sb.append("int main(void) {\n");
        for (FuncionC f : funciones) {
            sb.append("    ").append(f.getNombre()).append("();\n");
        }
        sb.append("    return 0;\n");
        sb.append("}\n");
    }

    ///Detecta si un tipo ya está en formato C (no requiere traducción)
    private boolean esTipoC(String tipo) {
        if (tipo == null) return false;
        return tipo.contains("struct ")
                || tipo.contains("*")
                || tipo.equals("int")
                || tipo.equals("double")
                || tipo.equals("float")
                || tipo.equals("char")
                || tipo.equals("void")
                || tipo.equals("long");
    }

    private void escribirVariablesGlobales(List<ParametroC> variables) {
        if (variables == null || variables.isEmpty()) return;
        for (ParametroC v : variables) {
            sb.append(v.getTipoC()).append(' ').append(v.getNombre()).append(";\n");
        }
        sb.append('\n');
    }
}