package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionAtributo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.LlamadaMetodo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.NewObjeto;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Emite una función RISC-V (constructor o método de Zetariano) a partir de un FuncionRiscV.
 *
 * Marco de la función:
 *   0(fp)     s0 anterior
 *  -8(fp)     ra
 * -16(fp)     this (a0)
 * -24(fp)     1er parámetro (a1)
 * -32(fp)     2do parámetro (a2)
 *  ...
 *  -N(fp)      1ra variable local
 *
 * Los offsets los calcula este emisor, por función. No toca TablaSimbolosPig.
 */
public class EmisorRiscVZ {

    private static final String IZQ = "t0";
    private static final String DER = "t1";
    private static final String RES = "t2";
    private static final String DIR = "t3";
    private static final String IDX = "t4";
    private static final String AUX = "t6";

    private final ContextoEmision ctx;
    private boolean esTextum(AccesoMemoria a) { return "textum".equals(a.getTipo()); }

    private final Map<String, Integer> offsets = new LinkedHashMap<>();
    private int siguienteOffset = 8;
    private String tipoClaseActual;

    public EmisorRiscVZ(ContextoEmision ctx) {
        this.ctx = ctx;
    }

    public void emitirFuncion(FuncionRiscV funcion) {
        tipoClaseActual = funcion.tipoClase();
        tiposVariablesActuales = funcion.tiposVariables();
        asignarOffsets(funcion);

        // Cuerpo primero: durante la emisión offsetDe() va reservando temporales y locales.
        StringBuilder cuerpo = new StringBuilder();
        int numero = 0;
        for (Cuarteta c : funcion.cuartetas()) {
            numero++;
            cuerpo.append("    # [").append(numero).append("] ").append(c.getClass().getSimpleName()).append("\n");
            try {
                emitirCuarteta(cuerpo, c, funcion);
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "EmisorRiscVZ: falló en cuarteta #" + numero + " de " + funcion.nombre() + " | " + e.getMessage(), e);
            }
        }

        // +8 porque el slot más profundo (-N(s0)) debe quedar DENTRO del marco: s0 = sp + framesize - 8.
        int framesize = ((siguienteOffset + 8 + 15) / 16) * 16;

        StringBuilder sb = ctx.getFunciones();
        sb.append("\n# ---------- Función: ").append(funcion.nombre()).append(" ----------\n");
        sb.append(funcion.nombre()).append(":\n");
        sb.append(prologo(framesize));

        if (funcion.tieneThis()) {
            sb.append("    sd   a0, -16(s0)        # this\n");
        }
        for (int i = 0; i < funcion.parametros().size(); i++) {
            sb.append("    sd   a").append(i + 1).append(", ")
                    .append(-24 - i * 8).append("(s0)        # ")
                    .append(funcion.parametros().get(i).nombre()).append("\n");
        }

        sb.append(cuerpo);
        sb.append(epilogo(framesize, funcion));
    }

    private void asignarOffsets(FuncionRiscV funcion) {
        offsets.clear();
        siguienteOffset = 8;
        if (funcion.tieneThis()) {
            offsets.put("this", -16);
            siguienteOffset = 16;
        }
        for (FuncionRiscV.ParametroRiscV p : funcion.parametros()) {
            siguienteOffset += 8;
            offsets.put(p.nombre(), -siguienteOffset);
        }
    }

    private int offsetDe(String nombre) {
        Integer off = offsets.get(nombre);
        if (off != null) return off;
        siguienteOffset += 8;
        int nuevo = -siguienteOffset;
        offsets.put(nombre, nuevo);
        return nuevo;
    }

    private String prologo(int framesize) {
        return "    addi sp, sp, -" + framesize + "\n" +
                "    sd   s0, " + (framesize - 8) + "(sp)\n" +
                "    sd   ra, " + (framesize - 16) + "(sp)\n" +
                "    addi s0, sp, " + (framesize - 8) + "\n";
    }

    private String epilogo(int framesize, FuncionRiscV funcion) {
        return "__epilogo_" + funcion.nombre() + ":\n" +
                "    ld   ra, -8(s0)\n" +
                "    ld   s0, 0(s0)\n" +
                "    addi sp, sp, " + framesize + "\n" +
                "    ret\n";
    }

    private void emitirCuarteta(StringBuilder sb, Cuarteta c, FuncionRiscV funcion) {
        if (c instanceof AsignacionVariable a) emitirAsignacion(sb, a);
        else if (c instanceof AsignacionArreglo a) emitirAsignacionArreglo(sb, a);
        else if (c instanceof AsignacionAtributo a) emitirAsignacionAtributo(sb, a);
        else if (c instanceof OperacionBinaria o) emitirOperacion(sb, o);
        else if (c instanceof OperacionUnaria o) emitirUnaria(sb, o);
        else if (c instanceof Condicional1 k) emitirCondicional(sb, k);
        else if (c instanceof Salto s) sb.append("    j    L").append(s.getEtiqueta()).append("\n");
        else if (c instanceof DefinicionEtiqueta e) sb.append("L").append(e.getEtiqueta()).append(":\n");
        else if (c instanceof Llamada l) emitirLlamada(sb, l);
        else if (c instanceof LlamadaMetodo1 l) emitirLlamadaMetodo(sb, l);
        else if (c instanceof NewObjeto n) emitirNewObjeto(sb, n);
        else if (c instanceof Retorno1 r) emitirRetorno(sb, r, funcion);
        else if (c instanceof ConversionTipo ct) emitirConversion(sb, ct);
        else if (c instanceof ImprimirPig i) emitirImprimir(sb, i);
        else if (c instanceof LeerPig l) emitirLeer(sb, l);
        else if (c instanceof Continuar1 k) sb.append("    j    L").append(k.getEtiqueta()).append("\n");
        else if (c instanceof Romper1 r) sb.append("    j    L").append(r.getEtiqueta()).append("\n");
        else throw new UnsupportedOperationException(
                    "EmisorRiscVZ: cuarteta no soportada: " + c.getClass().getSimpleName());
    }

    private void emitirAsignacion(StringBuilder sb, AsignacionVariable a) {
        cargar(sb, a.getValor(), IZQ, null);
        almacenar(sb, IZQ, a.getDestino());
    }

    private void emitirAsignacionArreglo(StringBuilder sb, AsignacionArreglo a) {
        cargar(sb, a.getValor(), IZQ, null);
        almacenarEnArreglo(sb, IZQ, a.getBase(), a.getIndice());
    }

    private void emitirAsignacionAtributo(StringBuilder sb, AsignacionAtributo a) {
        cargar(sb, a.getValor(), IZQ, null);
        direccionCampo(sb, a.getBase(), a.getCampo(), DIR);
        sb.append("    sd   ").append(IZQ).append(", 0(").append(DIR).append(")\n");
    }

    private void emitirOperacion(StringBuilder sb, OperacionBinaria o) {
        // Concatenación: '+' con al menos un operando textum.
        if ("+".equals(o.getOperador()) && (esTextum(o.getIzquierda()) || esTextum(o.getDerecha()))) {
            ctx.getRutinasUsadas().add(RutinasConsola.CONCAT);

            cargar(sb, o.getIzquierda(), "a0", null);
            if (!esTextum(o.getIzquierda())) convertirAString(sb, o.getIzquierda().getTipo());
            almacenar(sb, "a0", o.getDestino());

            cargar(sb, o.getDerecha(), "a0", null);
            if (!esTextum(o.getDerecha())) convertirAString(sb, o.getDerecha().getTipo());
            sb.append("    mv   a1, a0\n");

            cargar(sb, o.getDestino(), "a0", null);
            sb.append("    jal  ra, ").append(RutinasConsola.CONCAT).append("\n");
            almacenar(sb, "a0", o.getDestino());
            return;
        }

        cargar(sb, o.getIzquierda(), IZQ, null);
        cargar(sb, o.getDerecha(), DER, null);

        switch (o.getOperador()) {
            case "+" -> sb.append("    add  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            case "-" -> sb.append("    sub  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            case "*" -> sb.append("    mul  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            case "/" -> sb.append("    div  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            case "%" -> sb.append("    rem  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            case "<" -> sb.append("    slt  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            case ">" -> sb.append("    slt  ").append(RES).append(", ").append(DER).append(", ").append(IZQ).append("\n");
            case "<=" -> {
                sb.append("    slt  ").append(RES).append(", ").append(DER).append(", ").append(IZQ).append("\n");
                sb.append("    xori ").append(RES).append(", ").append(RES).append(", 1\n");
            }
            case ">=" -> {
                sb.append("    slt  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                sb.append("    xori ").append(RES).append(", ").append(RES).append(", 1\n");
            }
            case "==" -> {
                sb.append("    sub  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                sb.append("    sltiu ").append(RES).append(", ").append(RES).append(", 1\n");
            }
            case "!=" -> {
                sb.append("    sub  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                sb.append("    sltu ").append(RES).append(", zero, ").append(RES).append("\n");
            }
            case "&&" -> {
                sb.append("    sltu ").append(IZQ).append(", zero, ").append(IZQ).append("\n");
                sb.append("    sltu ").append(DER).append(", zero, ").append(DER).append("\n");
                sb.append("    and  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
            }
            case "||" -> {
                sb.append("    or   ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                sb.append("    sltu ").append(RES).append(", zero, ").append(RES).append("\n");
            }
            default -> throw new UnsupportedOperationException("Operador binario: " + o.getOperador());
        }
        almacenar(sb, RES, o.getDestino());
    }

    private void emitirUnaria(StringBuilder sb, OperacionUnaria o) {
        cargar(sb, o.getOperando(), IZQ, null);
        switch (o.getOperador()) {
            case "-" -> sb.append("    sub  ").append(RES).append(", zero, ").append(IZQ).append("\n");
            case "!" -> sb.append("    sltiu ").append(RES).append(", ").append(IZQ).append(", 1\n");
            case "+" -> sb.append("    mv   ").append(RES).append(", ").append(IZQ).append("\n");
            default -> throw new UnsupportedOperationException("Operador unario: " + o.getOperador());
        }
        almacenar(sb, RES, o.getDestino());
    }

    private void emitirCondicional(StringBuilder sb, Condicional1 c) {
        cargar(sb, c.getIzquierda(), IZQ, null);
        cargar(sb, c.getDerecha(), DER, null);
        String dest = "L" + c.getEtiqueta();
        switch (c.getOperador()) {
            case "==" -> sb.append("    beq  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
            case "!=" -> sb.append("    bne  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
            case "<"  -> sb.append("    blt  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
            case ">=" -> sb.append("    bge  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
            case ">"  -> sb.append("    blt  ").append(DER).append(", ").append(IZQ).append(", ").append(dest).append("\n");
            case "<=" -> sb.append("    bge  ").append(DER).append(", ").append(IZQ).append(", ").append(dest).append("\n");
            default -> throw new UnsupportedOperationException("Comparador: " + c.getOperador());
        }
    }

    private void emitirLlamada(StringBuilder sb, Llamada l) {
        List<AccesoMemoria> args = l.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(sb, args.get(i), "a" + i, null);
        }
        sb.append("    jal  ra, ").append(l.getNombreFuncion()).append("\n");
        if (l.getDestino() != null) almacenar(sb, "a0", l.getDestino());
    }

    private void emitirLlamadaMetodo(StringBuilder sb, LlamadaMetodo1 l) {
        cargar(sb, l.getReceptor(), "a0", null);
        List<AccesoMemoria> args = l.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(sb, args.get(i), "a" + (i + 1), null);
        }
        sb.append("    jal  ra, ").append(l.getNombreMetodo()).append("\n");
        if (l.getDestino() != null) almacenar(sb, "a0", l.getDestino());
    }

    private void emitirNewObjeto(StringBuilder sb, NewObjeto n) {
        int tam = tamanoDeClase(n.getNombreClase());
        sb.append("    la   ").append(IZQ).append(", heap_ptr\n");
        sb.append("    ld   ").append(DER).append(", 0(").append(IZQ).append(")\n");
        sb.append("    li   ").append(RES).append(", ").append(tam).append("\n");
        sb.append("    add  ").append(DIR).append(", ").append(DER).append(", ").append(RES).append("\n");
        sb.append("    sd   ").append(DIR).append(", 0(").append(IZQ).append(")\n");
        sb.append("    la   ").append(AUX).append(", heap\n");
        sb.append("    add  a0, ").append(AUX).append(", ").append(DER).append("\n");

        List<AccesoMemoria> args = n.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(sb, args.get(i), "a" + (i + 1), null);
        }
        sb.append("    jal  ra, ").append(n.getNombreConstructorC()).append("\n");
        if (n.getDestino() != null) almacenar(sb, "a0", n.getDestino());
    }

    private void emitirRetorno(StringBuilder sb, Retorno1 r, FuncionRiscV funcion) {
        if (r.getValor() != null) cargar(sb, r.getValor(), "a0", null);
        sb.append("    j    __epilogo_").append(funcion.nombre()).append("\n");
    }

    private void emitirConversion(StringBuilder sb, ConversionTipo c) {
        cargar(sb, c.getFuente(), IZQ, null);
        almacenar(sb, IZQ, c.getDestino());
    }

    private void emitirImprimir(StringBuilder sb, ImprimirPig i) {
        String rutina = switch (i.getTipo()) {
            case "textum" -> RutinasConsola.IMPRIMIR_CADENA;
            case "littera" -> RutinasConsola.IMPRIMIR_CARACTER;
            case "decimalis" -> throw new UnsupportedOperationException(
                    "EmisorRiscVZ: escribir decimalis no soportado");
            default -> RutinasConsola.IMPRIMIR_ENTERO;
        };
        ctx.getRutinasUsadas().add(rutina);
        cargar(sb, i.getValor(), "a0", null);
        sb.append("    jal  ra, ").append(rutina).append("\n");
    }

    private void emitirLeer(StringBuilder sb, LeerPig l) {
        switch (l.getTipo()) {
            case "textum" -> {
                String buffer = "buf_lectura_" + ctx.siguienteBuffer();
                ctx.getBss().append("    ").append(buffer).append(": .space 256\n");
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_CADENA);
                sb.append("    la   a0, ").append(buffer).append("\n");
                sb.append("    li   a1, 256\n");
                sb.append("    jal  ra, ").append(RutinasConsola.LEER_CADENA).append("\n");
                sb.append("    la   ").append(IZQ).append(", ").append(buffer).append("\n");
                almacenar(sb, IZQ, l.getDestino());
            }
            case "littera" -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_CARACTER);
                sb.append("    jal  ra, ").append(RutinasConsola.LEER_CARACTER).append("\n");
                almacenar(sb, "a0", l.getDestino());
            }
            case "decimalis" -> throw new UnsupportedOperationException(
                    "EmisorRiscVZ: leer decimalis no soportado");
            default -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_ENTERO);
                sb.append("    jal  ra, ").append(RutinasConsola.LEER_ENTERO).append("\n");
                almacenar(sb, "a0", l.getDestino());
            }
        }
    }

    // ========== CARGAR / GUARDAR ==========

    private void cargar(StringBuilder sb, AccesoMemoria acc, String registro, String tipo) {
        if (acc instanceof Literal l) {
            cargarLiteral(sb, l, registro);
        } else if (acc instanceof AccesoAtributo1 attr) {
            direccionCampo(sb, attr.getBase(), attr.getCampo(), DIR);
            sb.append("    ld   ").append(registro).append(", 0(").append(DIR).append(")\n");
        } else if (acc instanceof AccesoArreglo arr) {
            cargar(sb, arr.getIndice(), registro, null);
            direccionBase(sb, arr.getBase(), DIR);
            sb.append("    slli ").append(registro).append(", ").append(registro).append(", 3\n");
            sb.append("    add  ").append(DIR).append(", ").append(DIR).append(", ").append(registro).append("\n");
            sb.append("    ld   ").append(registro).append(", 0(").append(DIR).append(")\n");
        } else {
            int off = ubicar(acc);
            if (cabe(off)) {
                sb.append("    ld   ").append(registro).append(", ").append(off).append("(s0)\n");
            } else {
                sb.append("    li   ").append(AUX).append(", ").append(off).append("\n");
                sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
                sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
            }
        }
    }

    private void almacenar(StringBuilder sb, String registro, AccesoMemoria destino) {
        if (destino instanceof AccesoArreglo arr) {
            almacenarEnArreglo(sb, registro, arr.getBase(), arr.getIndice());
        } else if (destino instanceof AccesoAtributo1 attr) {
            direccionCampo(sb, attr.getBase(), attr.getCampo(), DIR);
            sb.append("    sd   ").append(registro).append(", 0(").append(DIR).append(")\n");
        } else {
            int off = ubicar(destino);
            if (cabe(off)) {
                sb.append("    sd   ").append(registro).append(", ").append(off).append("(s0)\n");
            } else {
                sb.append("    li   ").append(AUX).append(", ").append(off).append("\n");
                sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
                sb.append("    sd   ").append(registro).append(", 0(").append(AUX).append(")\n");
            }
        }
    }

    private void almacenarEnArreglo(StringBuilder sb, String regValor, AccesoMemoria base, AccesoMemoria indice) {
        cargar(sb, indice, IDX, null);
        direccionBase(sb, base, DIR);
        sb.append("    slli ").append(IDX).append(", ").append(IDX).append(", 3\n");
        sb.append("    add  ").append(DIR).append(", ").append(DIR).append(", ").append(IDX).append("\n");
        sb.append("    sd   ").append(regValor).append(", 0(").append(DIR).append(")\n");
    }

    private void direccionBase(StringBuilder sb, AccesoMemoria base, String registro) {
        if (!(base instanceof AccesoVariable v)) {
            throw new UnsupportedOperationException(
                    "EmisorRiscVZ: base de arreglo debe ser variable");
        }
        int off = offsetDe(v.getNombre());
        if (cabe(off)) {
            sb.append("    addi ").append(registro).append(", s0, ").append(off).append("\n");
        } else {
            sb.append("    li   ").append(registro).append(", ").append(off).append("\n");
            sb.append("    add  ").append(registro).append(", s0, ").append(registro).append("\n");
        }
    }

    /**
     * Deja en 'registro' la dirección del campo 'campo' del objeto 'base'.
     * Si base es 'this', usa tipoClaseActual. Si base es una variable, usa su tipo del mapa.
     */
    private void direccionCampo(StringBuilder sb, AccesoMemoria base, String campo, String registro) {
        String tipoClase;
        int offObjeto;

        if (base instanceof AccesoVariable v && v.getNombre().equals("this")) {
            tipoClase = tipoClaseActual;
            offObjeto = offsets.getOrDefault("this", -16);
        } else if (base instanceof AccesoVariable v) {
            tipoClase = tipoDeVariable(v.getNombre());
            offObjeto = offsetDe(v.getNombre());
        } else {
            throw new UnsupportedOperationException(
                    "EmisorRiscVZ: base de atributo debe ser 'this' o una variable");
        }

        int offCampo = calcularOffsetCampo(tipoClase, campo);

        // Cargar el puntero del objeto en registro.
        if (cabe(offObjeto)) {
            sb.append("    ld   ").append(registro).append(", ").append(offObjeto).append("(s0)\n");
        } else {
            sb.append("    li   ").append(AUX).append(", ").append(offObjeto).append("\n");
            sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
            sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
        }
        // Sumar offset del campo.
        if (offCampo != 0) {
            sb.append("    addi ").append(registro).append(", ").append(registro)
                    .append(", ").append(offCampo).append("\n");
        }
    }

    private String tipoDeVariable(String nombre) {
        // Buscar en los tipos de variables de la función en curso.
        // Se resuelve vía FuncionRiscV.tiposVariables. Aquí usamos un campo auxiliar.
        // Lo seteamos al inicio de emitirFuncion.
        return tiposVariablesActuales.getOrDefault(nombre, "?");
    }

    private Map<String, String> tiposVariablesActuales = Map.of();

    private int calcularOffsetCampo(String tipoClase, String campo) {
        var def = ctx.getTabla().buscarClase(tipoClase);
        if (def.isEmpty()) {
            throw new IllegalStateException("EmisorRiscVZ: clase no encontrada: " + tipoClase);
        }
        int offset = 0;
        for (var entry : def.get().atributos().entrySet()) {
            if (entry.getKey().equals(campo)) return offset;
            offset += 8;
        }
        throw new IllegalStateException(
                "EmisorRiscVZ: campo no encontrado: " + campo + " en " + tipoClase);
    }

    private void cargarLiteral(StringBuilder sb, Literal l, String registro) {
        Object valor = l.getValor();
        switch (l.getTipo()) {
            case "numerus" -> sb.append("    li   ").append(registro).append(", ").append(valor).append("\n");
            case "bool" -> {
                boolean v = valor instanceof Boolean b ? b : (Integer.parseInt(valor.toString()) != 0);
                sb.append("    li   ").append(registro).append(", ").append(v ? 1 : 0).append("\n");
            }
            case "littera" -> {
                int ascii = valor instanceof Character c ? c : Integer.parseInt(valor.toString());
                sb.append("    li   ").append(registro).append(", ").append(ascii).append("\n");
            }
            case "textum" -> {
                String etq = etiquetaCadena(String.valueOf(valor));
                sb.append("    la   ").append(registro).append(", ").append(etq).append("\n");
            }
            case "decimalis" -> throw new UnsupportedOperationException(
                    "EmisorRiscVZ: literales decimalis no soportados");
            case "null" -> sb.append("    li   ").append(registro).append(", 0\n");
            default -> throw new IllegalArgumentException("Tipo literal: " + l.getTipo());
        }
    }

    private int ubicar(AccesoMemoria acc) {
        if (acc instanceof AccesoVariable v) return offsetDe(v.getNombre());
        if (acc instanceof AccesoTemporal t) return offsetDe("t" + t.getNumero());
        throw new IllegalStateException("EmisorRiscVZ: no ubicable: " + acc);
    }

    private static boolean cabe(int offset) {
        return offset >= -2048 && offset <= 2047;
    }

    private String etiquetaCadena(String valor) {
        String esc = escaparCadena(valor);
        String existente = ctx.getCadenas().get(esc);
        if (existente != null) return existente;
        String etq = "str_" + ctx.getCadenas().size();
        ctx.getCadenas().put(esc, etq);
        ctx.getData().append("    ").append(etq).append(": .string \"").append(esc).append("\"\n");
        return etq;
    }

    private static String escaparCadena(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\\') {
                if (i < s.length()) { sb.append('\\').append(s.charAt(i)); i++; } else sb.append("\\\\");
            } else if (cp == '"') sb.append("\\\"");
            else if (cp == '\n') sb.append("\\n");
            else if (cp == '\t') sb.append("\\t");
            else if (cp == '\r') sb.append("\\r");
            else if (cp >= 32 && cp < 127) sb.append((char) cp);
            else for (byte b : new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8))
                    sb.append(String.format("\\%03o", b & 0xFF));
        }
        return sb.toString();
    }

    private int tamanoDeClase(String clase) {
        var def = ctx.getTabla().buscarClase(clase);
        if (def.isEmpty()) return 8;
        return 8 * def.get().atributos().size();
    }

    private void convertirAString(StringBuilder sb, String tipo) {
        String rutina = switch (tipo == null ? "" : tipo) {
            case "littera" -> RutinasConsola.Z_TO_STRING_CHAR;
            case "bool"    -> RutinasConsola.Z_TO_STRING_BOOL;
            default        -> RutinasConsola.Z_TO_STRING_INT;
        };
        ctx.getRutinasUsadas().add(rutina);
        sb.append("    mv   a1, a0\n");
        sb.append("    jal  ra, ").append(rutina).append("\n");
    }
}