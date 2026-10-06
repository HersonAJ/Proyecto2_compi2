package com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ;

import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.AccesoMemoria;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

import java.util.List;

/**
 * Cuarteta: t0 = new Persona("Carlos", 25)
 *
 * Reserva memoria en heap y llama al constructor.
 * Ejemplo en .z: Persona p = new Persona("Carlos", 25)
 * C generado:
 *     struct Persona* t0 = malloc(sizeof(struct Persona));
 *     Persona_constructor_String_int(t0, "Carlos", 25);
 */
public class NewObjeto extends Cuarteta {

    private final AccesoMemoria destino;         // el temporal que recibe el puntero
    private final String nombreClase;            // "Persona"
    private final String nombreConstructorC;     // "Persona_constructor_String_int"
    private final List<AccesoMemoria> argumentos;

    public NewObjeto(AccesoMemoria destino,
                     String nombreClase,
                     String nombreConstructorC,
                     List<AccesoMemoria> argumentos) {
        this.destino = destino;
        this.nombreClase = nombreClase;
        this.nombreConstructorC = nombreConstructorC;
        this.argumentos = argumentos;
    }

    public AccesoMemoria getDestino()             { return destino; }
    public String getNombreClase()                { return nombreClase; }
    public String getNombreConstructorC()         { return nombreConstructorC; }
    public List<AccesoMemoria> getArgumentos()    { return argumentos; }

    @Override
    public void aCodigoC(StringBuilder sb) {
        // 1. Reservar memoria
        sb.append("    ");
        destino.aCodigoC(sb);
        sb.append(" = malloc(sizeof(struct ").append(nombreClase).append("));\n");

        // 2. Llamar al constructor
        sb.append("    ").append(nombreConstructorC).append('(');
        destino.aCodigoC(sb);
        for (AccesoMemoria arg : argumentos) {
            sb.append(", ");
            arg.aCodigoC(sb);
        }
        sb.append(");\n");
    }
}