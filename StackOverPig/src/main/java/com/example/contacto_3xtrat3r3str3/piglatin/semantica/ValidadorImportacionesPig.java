package com.example.contacto_3xtrat3r3str3.piglatin.semantica;

import com.example.contacto_3xtrat3r3str3.piglatin.nodo.NodoImportacion;
import com.example.contacto_3xtrat3r3str3.y.ast.*;
import com.example.contacto_3xtrat3r3str3.y.errores.ResultadoCompilacionY;
import com.example.contacto_3xtrat3r3str3.y.semantica.error.ErrorSemantico;
import com.example.contacto_3xtrat3r3str3.y.service.ServicioCompilacionY;
import com.example.contacto_3xtrat3r3str3.zetariano.nodo.*;
import com.example.contacto_3xtrat3r3str3.zetariano.service.ResultadoCompilacionZ;
import com.example.contacto_3xtrat3r3str3.zetariano.service.ServicioCompilacionZ;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga las importaciones de PigLatin en la tabla de símbolos.
 *
 * Soporta dos tipos:
 *   - import carpeta.Archivo.y  → carga estructuras y funciones de Y?.
 *   - import carpeta.Archivo.z  → carga la clase pública de Zetariano.
 *
 * Además de poblar la tabla de símbolos, genera los FuncionC y EstructuraC
 * de los archivos importados para que el CompiladorPig los incluya en el .c final.
 */
public class ValidadorImportacionesPig {

    private final TablaSimbolosPig tabla;
    private final List<ErrorSemantico> errores;
    private final Path carpetaRaiz;

    // codigo C generado de los imports
    private final List<com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC> funcionesImportadas = new ArrayList<>();
    private final List<com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC> estructurasImportadas = new ArrayList<>();

    public ValidadorImportacionesPig(TablaSimbolosPig tabla,
                                     List<ErrorSemantico> errores,
                                     Path carpetaRaiz) {
        this.tabla = tabla;
        this.errores = errores;
        this.carpetaRaiz = carpetaRaiz;
    }

    public List<com.example.contacto_3xtrat3r3str3.c3d_v2.c.FuncionC> getFuncionesImportadas() {
        return funcionesImportadas;
    }

    public List<com.example.contacto_3xtrat3r3str3.c3d_v2.c.EstructuraC> getEstructurasImportadas() {
        return estructurasImportadas;
    }

    //Procesa todas las importaciones del programa. Devuelve true si todas se cargaron correctamente.
    public boolean procesarImportaciones(List<NodoImportacion> importaciones) {
        boolean todasOk = true;

        for (NodoImportacion imp : importaciones) {
            boolean ok = procesarImportacion(imp);
            if (!ok) todasOk = false;
        }

        return todasOk;
    }

    private boolean procesarImportacion(NodoImportacion imp) {
        String ruta = imp.ruta();

        // Determinar la extensión (.y o .z)
        if (ruta.endsWith(".y")) {
            return importarY(ruta, imp);
        } else if (ruta.endsWith(".z")) {
            return importarZ(ruta, imp);
        } else {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Importación inválida",
                    "La ruta '" + ruta + "' no termina en '.y' ni en '.z'"));
            return false;
        }
    }

    // IMPORTAR DE .y
    private boolean importarY(String ruta, NodoImportacion imp) {
        // Convertir 'carpeta.Archivo.y' a 'carpeta/Archivo.y'
        String rutaRelativa = convertirRuta(ruta);

        // Resolver la ruta absoluta
        Path archivo = carpetaRaiz.resolve(rutaRelativa);

        if (!Files.exists(archivo)) {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Archivo no encontrado",
                    "No se encontró el archivo '" + ruta + "' en '" + archivo.toAbsolutePath() + "'"));
            return false;
        }

        // Leer contenido
        String contenido;
        try {
            contenido = Files.readString(archivo);
        } catch (IOException e) {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Error al leer archivo",
                    "No se pudo leer '" + archivo.toAbsolutePath() + "': " + e.getMessage()));
            return false;
        }

        // Ejecutar el servicio de Y?
        ServicioCompilacionY servicioY = new ServicioCompilacionY();
        ResultadoCompilacionY resultado = servicioY.analizar(contenido);

        if (!resultado.isExitoso()) {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Error en archivo importado",
                    "El archivo '" + ruta + "' tiene errores: "
                            + resultado.getErroresSemanticos().size() + " semánticos, "
                            + resultado.getErroresSintacticos().size() + " sintácticos"));
            return false;
        }

        // Cargar estructuras y funciones en la tabla (con tipos traducidos a .pig)
        if (resultado.getPrograma() != null) {
            for (NodoEstructura est : resultado.getPrograma().estructuras()) {
                NodoEstructura.Estructura e = (NodoEstructura.Estructura) est;

                // Verificar colisión con una clase ya importada
                if (tabla.buscarClase(e.nombre()).isPresent()) {
                    errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                            "Nombre en conflicto",
                            "La estructura '" + e.nombre() + "' del archivo importado '" + ruta
                                    + "' entra en conflicto con una clase ya importada con el mismo nombre"));
                    return false;
                }
                // Verificar colisión con otra estructura ya importada
                if (tabla.buscarEstructura(e.nombre()).isPresent()) {
                    errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                            "Declaración duplicada",
                            "La estructura '" + e.nombre() + "' ya fue declarada por otro archivo importado"));
                    return false;
                }

                Map<String, String> atributos = new LinkedHashMap<>();
                for (NodoAtributo a : e.atributos()) {
                    NodoAtributo.Atributo at = (NodoAtributo.Atributo) a;
                    String tipo = at.tipoPrimitivo() != null ? at.tipoPrimitivo() : at.tipoEstructura();
                    atributos.put(at.nombre(), tipoYaPig(tipo));
                }
                tabla.declararEstructura(e.nombre(), atributos);
            }

            for (NodoFuncion f : resultado.getPrograma().funciones()) {
                NodoFuncion.Funcion fn = (NodoFuncion.Funcion) f;
                List<TablaSimbolosPig.Parametro> params = new ArrayList<>();
                for (NodoParametro p : fn.parametros()) {
                    NodoParametro.Parametro param = (NodoParametro.Parametro) p;
                    String tipo = param.tipoPrimitivo() != null ? param.tipoPrimitivo() : param.tipoEstructura();
                    params.add(new TablaSimbolosPig.Parametro(
                            param.nombre(), tipoYaPig(tipo), param.esArreglo() ? 1 : 0));
                }
                tabla.declararFuncion(fn.nombre(), params, tipoYaPig(fn.tipoRetorno()));
            }
        }

        // generar FuncionC y EstructuraC del .y importado
        if (resultado.getPrograma() != null) {
            estructurasImportadas.addAll(resultado.getPrograma().aEstructurasC());

            var tablaY = resultado.getTablaSimbolos();
            if (tablaY != null) {
                funcionesImportadas.addAll(resultado.getPrograma().aFuncionesC(tablaY));
            }
        }

        return true;
    }


    // IMPORTAR DE .z
    private boolean importarZ(String ruta, NodoImportacion imp) {
        // Convertir 'carpeta.Archivo.z' a 'carpeta/Archivo.z'
        String rutaRelativa = convertirRuta(ruta);

        Path archivo = carpetaRaiz.resolve(rutaRelativa);

        if (!Files.exists(archivo)) {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Archivo no encontrado",
                    "No se encontró el archivo '" + ruta + "' en '" + archivo.toAbsolutePath() + "'"));
            return false;
        }

        String contenido;
        try {
            contenido = Files.readString(archivo);
        } catch (IOException e) {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Error al leer archivo",
                    "No se pudo leer '" + archivo.toAbsolutePath() + "': " + e.getMessage()));
            return false;
        }

        // Ejecutar el servicio de Z
        ServicioCompilacionZ servicioZ = new ServicioCompilacionZ();
        ResultadoCompilacionZ resultado = servicioZ.analizar(contenido);

        if (!resultado.isExitoso()) {
            errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                    "Error en archivo importado",
                    "El archivo '" + ruta + "' tiene errores: "
                            + resultado.getErroresSemanticos().size() + " semánticos, "
                            + resultado.getErroresSintacticos().size() + " sintácticos"));
            return false;
        }

        // Cargar la clase en la tabla (con tipos traducidos a .pig)
        if (resultado.getPrograma() != null) {
            NodoClase clase = resultado.getPrograma().clase();

            // Verificar colisión con una estructura ya importada
            if (tabla.buscarEstructura(clase.nombre()).isPresent()) {
                errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                        "Nombre en conflicto",
                        "La clase '" + clase.nombre() + "' del archivo importado '" + ruta
                                + "' entra en conflicto con una estructura ya importada con el mismo nombre"));
                return false;
            }
            // Verificar colisión con otra clase ya importada
            if (tabla.buscarClase(clase.nombre()).isPresent()) {
                errores.add(new ErrorSemantico(imp.linea(), imp.columna(),
                        "Declaración duplicada",
                        "La clase '" + clase.nombre() + "' ya fue declarada por otro archivo importado"));
                return false;
            }

            // Atributos
            Map<String, TablaSimbolosPig.AtributoClase> atributos = new LinkedHashMap<>();
            for (NodoAtributoZ a : clase.atributos()) {
                atributos.put(a.nombre(), new TablaSimbolosPig.AtributoClase(
                        a.nombre(), tipoZaPig(a.tipo()), 0));
            }

            // Constructores
            List<TablaSimbolosPig.Firma> constructores = new ArrayList<>();
            for (NodoConstructor c : clase.constructores()) {
                List<TablaSimbolosPig.Parametro> params = new ArrayList<>();
                for (NodoParametroZ p : c.parametros()) {
                    params.add(new TablaSimbolosPig.Parametro(
                            p.nombre(), tipoZaPig(p.tipo()), 0));
                }
                constructores.add(new TablaSimbolosPig.Firma(c.nombre(), params, null));
            }

            // Métodos
            List<TablaSimbolosPig.Firma> metodos = new ArrayList<>();
            for (NodoMetodo m : clase.metodos()) {
                List<TablaSimbolosPig.Parametro> params = new ArrayList<>();
                for (NodoParametroZ p : m.parametros()) {
                    params.add(new TablaSimbolosPig.Parametro(
                            p.nombre(), tipoZaPig(p.tipo()), 0));
                }
                metodos.add(new TablaSimbolosPig.Firma(m.nombre(), params, tipoZaPig(m.tipoRetorno())));
            }

            TablaSimbolosPig.DefinicionClase def = new TablaSimbolosPig.DefinicionClase(
                    clase.nombre(), atributos, constructores, metodos);
            tabla.declararClase(def);
            estructurasImportadas.add(clase.aEstructuraC());

            var tablaZ = resultado.getTablaSimbolos();
            if (tablaZ != null) {
                funcionesImportadas.addAll(clase.aFuncionesC(tablaZ));
            }
        }

        return true;
    }

    //Convierte 'carpeta.Archivo.y' → 'carpeta/Archivo.y'. El último punto (extensión) se conserva
    private String convertirRuta(String ruta) {
        int ultimoPunto = ruta.lastIndexOf('.');
        if (ultimoPunto < 0) return ruta;

        String sinExtension = ruta.substring(0, ultimoPunto);
        String extension = ruta.substring(ultimoPunto);

        return sinExtension.replace(".", "/") + extension;
    }

    // HELPERS DE TRADUCCIÓN DE TIPOS
    // Traduce un tipo de .z a su equivalente en .pig
    private static String tipoZaPig(String tipoZ) {
        if (tipoZ == null) return null;
        return switch (tipoZ) {
            case "int"     -> "numerus";
            case "double"  -> "decimalis";
            case "char"    -> "littera";
            case "String"  -> "textum";
            case "boolean" -> "bool";
            default        -> tipoZ;
        };
    }

    // Traduce un tipo de .y a su equivalente en .pig
    private static String tipoYaPig(String tipoY) {
        if (tipoY == null) return null;
        return switch (tipoY) {
            case "entero"   -> "numerus";
            case "flotante" -> "decimalis";
            case "caracter" -> "littera";
            case "cadena"   -> "textum";
            case "bool"     -> "bool";
            default         -> tipoY;
        };
    }
}