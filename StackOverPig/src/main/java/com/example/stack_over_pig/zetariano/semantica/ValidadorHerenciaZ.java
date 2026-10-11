package com.example.stack_over_pig.zetariano.semantica;

import com.example.stack_over_pig.y.semantica.error.ErrorSemantico;
import com.example.stack_over_pig.zetariano.nodo.NodoClase;
import com.example.stack_over_pig.zetariano.nodo.NodoMetodo;
import com.example.stack_over_pig.zetariano.nodo.NodoParametroZ;
import com.example.stack_over_pig.zetariano.nodo.Visibilidad;

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

    public ValidadorHerenciaZ(TablaSimbolosZ tabla, List<ErrorSemantico> errores) {
        this.tabla = tabla;
        this.errores = errores;
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