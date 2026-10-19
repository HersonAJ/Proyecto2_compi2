package com.example.stack_over_pig.zetariano.semantica;

import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;
import com.example.stack_over_pig.zetariano.nodo.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Valida la herencia de la clase del archivo.
 *
 * Validaciones:
 *   - La clase hereda de si misma o la herencia es circular   -> validarSuperclase
 *   - La superclase no existe                                 -> validarSuperclase
 *   - @Override sin un metodo que sobrescribir                -> validarMetodo
 *   - Sobrescribir sin @Override (ver OVERRIDE_OBLIGATORIO)   -> validarMetodo
 *   - Tipo de retorno distinto al del metodo sobrescrito      -> validarMetodo
 *   - Visibilidad menor a la del metodo sobrescrito           -> validarMetodo
 *
 * Un metodo sobrescribe a otro cuando tiene el mismo nombre y los mismos tipos de parametros.
 * Si el parametro cambia es una sobrecarga, no una sobrescritura. Los metodos 'private' del
 * padre no se heredan, asi que no se pueden sobrescribir.
 */
public class ValidadorHerenciaZ {

    /**
     * Regla pendiente de confirmar: el enunciado dice que @Override es
     * "obligatoria para definir polimorfismo". Con 'true' sobrescribir sin la anotacion es error;
     * con 'false' se permite. Es el unico lugar donde cambia.
     */
    static final boolean OVERRIDE_OBLIGATORIO = true;

    private final TablaSimbolosZ tabla;
    private final List<ErrorSemantico> errores;
    private final ValidadorTiposZ tipos;

    public ValidadorHerenciaZ(TablaSimbolosZ tabla, List<ErrorSemantico> errores, ValidadorTiposZ tipos) {
        this.tabla = tabla;
        this.errores = errores;
        this.tipos = tipos;
    }

    /** Se llama despues de declarar la clase, sus atributos, constructores y metodos. */
    public void validar(NodoClase clase) {
        if (!validarSuperclase(clase)) return;   // con la jerarquia rota no tiene sentido seguir
        for (NodoMetodo m : clase.metodos()) {
            validarMetodo(clase, m);
        }
    }

    /** Devuelve false si la jerarquia no sirve (circular o superclase desconocida). */
    private boolean validarSuperclase(NodoClase clase) {
        String padre = clase.superclase();
        if (padre == null) return true;

        if (padre.equals(clase.nombre())) {
            errores.add(new ErrorSemantico(clase.linea(), clase.columna(), "Herencia invalida",
                    "La clase '" + clase.nombre() + "' no puede heredar de si misma"));
            return false;
        }
        if (tabla.tieneHerenciaCircular(clase.nombre())) {
            errores.add(new ErrorSemantico(clase.linea(), clase.columna(), "Herencia circular",
                    "La herencia de '" + clase.nombre() + "' es circular: una de sus superclases hereda de ella"));
            return false;
        }
        if (!tabla.esClaseConocida(padre)) {
            errores.add(new ErrorSemantico(clase.linea(), clase.columna(), "Superclase no declarada",
                    "'" + padre + "' no corresponde a ninguna clase conocida"));
            return false;
        }
        return true;
    }

    private void validarMetodo(NodoClase clase, NodoMetodo m) {
        List<String> tipos = m.parametros().stream().map(NodoParametroZ::tipo).toList();
        Optional<TablaSimbolosZ.Firma> sobrescrito =
                tabla.buscarMetodoSobrescrito(clase.nombre(), m.nombre(), tipos);

        // No hay nada que sobrescribir
        if (sobrescrito.isEmpty()) {
            if (m.esOverride() && jerarquiaCompleta(clase)) {
                errores.add(new ErrorSemantico(m.linea(), m.columna(), "@Override invalido",
                        "El metodo '" + m.nombre() + "' lleva @Override pero ninguna superclase de '"
                                + clase.nombre() + "' declara un metodo con ese nombre y esos parametros"));
            }
            return;
        }

        TablaSimbolosZ.Firma padre = sobrescrito.get();

        if (!m.esOverride() && OVERRIDE_OBLIGATORIO) {
            errores.add(new ErrorSemantico(m.linea(), m.columna(), "Falta @Override",
                    "El metodo '" + m.nombre() + "' sobrescribe al de la clase '" + padre.claseDeclarante()
                            + "' y debe llevar @Override"));
        }

        if (!Objects.equals(padre.tipoRetorno(), m.tipoRetorno())) {
            errores.add(new ErrorSemantico(m.linea(), m.columna(), "Sobrescritura invalida",
                    "El metodo '" + m.nombre() + "' retorna '" + nombreTipo(m.tipoRetorno())
                            + "' pero el de '" + padre.claseDeclarante() + "' retorna '"
                            + nombreTipo(padre.tipoRetorno()) + "'"));
        }

        if (rango(m.visibilidad()) < rango(padre.visibilidad())) {
            errores.add(new ErrorSemantico(m.linea(), m.columna(), "Sobrescritura invalida",
                    "El metodo '" + m.nombre() + "' es " + descripcion(m.visibilidad())
                            + " y no puede reducir la visibilidad del de '" + padre.claseDeclarante()
                            + "', que es " + descripcion(padre.visibilidad())));
        }
    }

    /** true si se conocen todas las clases de la cadena de herencia (si falta alguna, no se puede asegurar nada). */
    private boolean jerarquiaCompleta(NodoClase clase) {
        return tabla.cadenaDe(clase.nombre()).stream().allMatch(tabla::esClaseConocida);
    }

    /** Valida los constructores: super() implícito, super() explícito y visibilidad. */
    public void validarConstructores(NodoClase clase) {
        String padre = clase.superclase();
        if (padre == null || !tabla.esClaseConocida(padre)) return;

        boolean padreTieneConstructorSinParams = tabla.getConstructoresDe(padre)
                .stream()
                .anyMatch(f -> f.parametros().isEmpty() && f.visibilidad() != Visibilidad.PRIVADO);

        if (clase.constructores().isEmpty()) {
            // Constructor implícito sin parámetros -> llama a super() sin params
            if (!padreTieneConstructorSinParams) {
                errores.add(new ErrorSemantico(clase.linea(), clase.columna(),
                        "Falta constructor",
                        "La clase '" + clase.nombre() + "' no declara constructores y '" + padre
                                + "' no tiene un constructor sin parametros visible"));
            }
            return;
        }

        for (NodoConstructor c : clase.constructores()) {
            // Buscar si el cuerpo tiene una LlamadaSuperConstructor.
            NodoSentencia.LlamadaSuperConstructor llamada = null;
            for (NodoSentencia s : c.cuerpo()) {
                if (s instanceof NodoSentencia.LlamadaSuperConstructor lsc) {
                    llamada = lsc;
                    break;
                }
            }

            if (llamada == null) {
                // Sin super() explícito: el padre debe tener constructor sin params visible.
                if (!padreTieneConstructorSinParams) {
                    errores.add(new ErrorSemantico(c.linea(), c.columna(),
                            "Falta llamada a super",
                            "El constructor de '" + clase.nombre()
                                    + "' debe llamar a un constructor de '" + padre
                                    + "' porque '" + padre + "' no tiene un constructor sin parametros visible"));
                }
                continue;
            }

            // Debe ser la primera instrucción.
            if (c.cuerpo().get(0) != llamada) {
                errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(),
                        "super mal ubicado",
                        "super(...) debe ser la primera instruccion del constructor"));
            }

            // Resolver el constructor del padre según los tipos de argumentos.
            List<String> tiposArgs = llamada.argumentos().stream()
                    .map(this::tipoDeExpresionComoString)
                    .toList();

            // Verificar que exista un constructor del padre con esos tipos.
            boolean existe = tabla.getConstructoresDe(padre).stream()
                    .anyMatch(f -> f.tiposParametros().equals(tiposArgs));

            if (!existe) {
                errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(),
                        "Constructor del padre no encontrado",
                        "'" + padre + "' no tiene un constructor que acepte esos argumentos"));
                continue;
            }

            // Verificar visibilidad.
            boolean visible = tabla.getConstructoresDe(padre).stream()
                    .filter(f -> f.tiposParametros().equals(tiposArgs))
                    .anyMatch(f -> esVisibleDesdeLaSubclase(f.visibilidad(), padre, clase.nombre()));

            if (!visible) {
                errores.add(new ErrorSemantico(llamada.linea(), llamada.columna(),
                        "Constructor del padre no visible",
                        "El constructor de '" + padre + "' no es visible desde '" + clase.nombre() + "'"));
            }
        }
    }

    private boolean esVisibleDesdeLaSubclase(Visibilidad v, String padre, String subclase) {
        return switch (v) {
            case PUBLICO, PROTEGIDO -> true;
            case PAQUETE -> tabla.paqueteDe(padre).equals(tabla.paqueteDe(subclase));
            case PRIVADO -> false;
        };
    }

    /** Tipo de una expresión como string para comparar con parámetros (sin validar). */
    private String tipoDeExpresionComoString(NodoExpr expr) {
        // Reutilizamos la lógica de tipos existente. Si no la tienes accesible,
        // hay que inyectar ValidadorTiposZ o exponer un método en ValidadorSemanticoZ.
        return tipos.tipoDeExpresion(expr) == null ? "?" : tipos.tipoDeExpresion(expr).base();
    }

    private String nombreTipo(String tipo) {
        return tipo == null ? "void" : tipo;
    }

    /** Mayor numero = mas visible. */
    private int rango(Visibilidad v) {
        return switch (v) {
            case PUBLICO -> 3;
            case PROTEGIDO -> 2;
            case PAQUETE -> 1;
            case PRIVADO -> 0;
        };
    }

    private String descripcion(Visibilidad v) {
        return switch (v) {
            case PUBLICO -> "public";
            case PROTEGIDO -> "protected";
            case PAQUETE -> "de paquete (sin modificador)";
            case PRIVADO -> "private";
        };
    }
}