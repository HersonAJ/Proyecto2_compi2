package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionAtributo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.LlamadaMetodo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.NewArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.NewObjeto;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private Set<String> parametrosPorReferencia = Set.of();

    public EmisorRiscVZ(ContextoEmision ctx) {
        this.ctx = ctx;
    }

    public void emitirFuncion(FuncionRiscV funcion) {
        tipoClaseActual = funcion.tipoClase();
        tiposVariablesActuales = funcion.tiposVariables();
        parametrosPorReferencia = new HashSet<>();
        for (FuncionRiscV.ParametroRiscV p : funcion.parametros()) {
            if (p.esPorReferencia()) parametrosPorReferencia.add(p.nombre());
        }
        asignarOffsets(funcion);
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
        int framesize = ((siguienteOffset + 8 + 15) / 16) * 16;

        StringBuilder sb = ctx.getFunciones();
        sb.append("\n# ---------- Función: ").append(funcion.nombre()).append(" ----------\n");
        sb.append(funcion.nombre()).append(":\n");
        sb.append(prologo(framesize));

        if (funcion.tieneThis()) {
            sb.append("    sd   a0, -16(s0)        # this\n");
        }
        int baseParam = funcion.tieneThis() ? 1 : 0;
        int baseOffset = funcion.tieneThis() ? -24 : -16;
        for (int i = 0; i < funcion.parametros().size(); i++) {
            sb.append("    sd   a").append(baseParam + i).append(", ")
                    .append(baseOffset - i * 8).append("(s0)        # ")
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
        String tipo = tiposVariablesActuales.get(nombre);
        int bytes = tamanoEnBytes(nombre, tipo);
        bytes = ((bytes + 7) / 8) * 8;
        siguienteOffset += bytes;
        int nuevo = -siguienteOffset;
        offsets.put(nombre, nuevo);
        return nuevo;
    }

    private int tamanoEnBytes(String nombre, String tipoPig) {
        if (tipoPig == null) return 8;

        // Struct Y importado: 8 * numCampos.
        var est = ctx.getTabla().buscarEstructura(tipoPig);
        if (est.isPresent()) {
            return 8 * Math.max(1, est.get().atributos().size());
        }

        // Arreglo local: 8 * tamanoConocido (si está en la tabla).
        var sim = ctx.getTabla().buscarVariable(nombre);
        if (sim.isPresent()) {
            var s = sim.get();
            if (s.dimensiones() > 0 && s.tamanoConocido() != null) {
                return 8 * Math.max(1, s.tamanoConocido());
            }
        }

        // Primitivo o clase Z (un puntero de 8 bytes).
        return 8;
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
        else if (c instanceof NewArreglo a) emitirNewArreglo(sb, a);
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
        String tIzq = o.getIzquierda().getTipo();
        String tDer = o.getDerecha().getTipo();
        boolean izqDec = "decimalis".equals(tIzq);
        boolean derDec = "decimalis".equals(tDer);
        if (izqDec || derDec) {
            cargar(sb, o.getIzquierda(), IZQ, null);
            if (!izqDec) {
                sb.append("    li   ").append(AUX).append(", 1000000\n");
                sb.append("    mul  ").append(IZQ).append(", ").append(IZQ).append(", ").append(AUX).append("\n");
            }
            cargar(sb, o.getDerecha(), DER, null);
            if (!derDec) {
                sb.append("    li   ").append(AUX).append(", 1000000\n");
                sb.append("    mul  ").append(DER).append(", ").append(DER).append(", ").append(AUX).append("\n");
            }
            switch (o.getOperador()) {
                case "+" -> sb.append("    add  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                case "-" -> sb.append("    sub  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                case "*" -> {
                    sb.append("    mul  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                    sb.append("    li   ").append(AUX).append(", 1000000\n");
                    sb.append("    div  ").append(RES).append(", ").append(RES).append(", ").append(AUX).append("\n");
                }
                case "/" -> {
                    sb.append("    li   ").append(AUX).append(", 1000000\n");
                    sb.append("    mul  ").append(IZQ).append(", ").append(IZQ).append(", ").append(AUX).append("\n");
                    sb.append("    div  ").append(RES).append(", ").append(IZQ).append(", ").append(DER).append("\n");
                }
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
                default -> throw new UnsupportedOperationException("Operador decimalis: " + o.getOperador());
            }
            almacenar(sb, RES, o.getDestino());
            return;
        }
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
        String dest = "L" + c.getEtiqueta();

        if ("decimalis".equals(c.getIzquierda().getTipo())
                || "decimalis".equals(c.getDerecha().getTipo())) {
            cargar(sb, c.getIzquierda(), IZQ, null);
            if (!"decimalis".equals(c.getIzquierda().getTipo())) {
                sb.append("    li   ").append(AUX).append(", 1000000\n");
                sb.append("    mul  ").append(IZQ).append(", ").append(IZQ).append(", ").append(AUX).append("\n");
            }
            cargar(sb, c.getDerecha(), DER, null);
            if (!"decimalis".equals(c.getDerecha().getTipo())) {
                sb.append("    li   ").append(AUX).append(", 1000000\n");
                sb.append("    mul  ").append(DER).append(", ").append(DER).append(", ").append(AUX).append("\n");
            }
            switch (c.getOperador()) {
                case "==" -> sb.append("    beq  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
                case "!=" -> sb.append("    bne  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
                case "<"  -> sb.append("    blt  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
                case ">=" -> sb.append("    bge  ").append(IZQ).append(", ").append(DER).append(", ").append(dest).append("\n");
                case ">"  -> sb.append("    blt  ").append(DER).append(", ").append(IZQ).append(", ").append(dest).append("\n");
                case "<=" -> sb.append("    bge  ").append(DER).append(", ").append(IZQ).append(", ").append(dest).append("\n");
                default -> throw new UnsupportedOperationException("Comparador decimalis: " + c.getOperador());
            }
            return;
        }

        cargar(sb, c.getIzquierda(), IZQ, null);
        cargar(sb, c.getDerecha(), DER, null);
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
        boolean esFuncionLibre = !l.getNombreFuncion().contains("_constructor")
                && !ctx.getVirtuales().esVirtual(l.getNombreFuncion());
        int base = esFuncionLibre ? 0 : 1;

        List<AccesoMemoria> args = l.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(sb, args.get(i), "a" + (base + i), null);
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
        String metodo = l.getNombreMetodo();
        if (ctx.getVirtuales().esVirtual(metodo)) {
            sb.append("    ld   t5, 0(a0)   # tabla de métodos del objeto\n");
            sb.append("    ld   t5, ").append(ctx.getVirtuales().desplazamiento(metodo)).append("(t5)   # ").append(metodo).append("\n");
            sb.append("    jalr ra, t5, 0\n");
        } else {
            sb.append("    jal  ra, ").append(metodo).append("\n");
        }
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
        var virtuales = ctx.getVirtuales();
        if (virtuales.tieneTabla(n.getNombreClase())) {
            sb.append("    la   ").append(IDX).append(", ").append(virtuales.etiquetaTabla(n.getNombreClase())).append("\n");
            sb.append("    sd   ").append(IDX).append(", 0(a0)   # tabla de métodos del objeto\n");
        }

        List<AccesoMemoria> args = n.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(sb, args.get(i), "a" + (i + 1), null);
        }
        sb.append("    jal  ra, ").append(n.getNombreConstructorC()).append("\n");
        if (n.getDestino() != null) almacenar(sb, "a0", n.getDestino());
    }

    private void emitirNewArreglo(StringBuilder sb, NewArreglo a) {
        // IZQ = número de elementos
        cargar(sb, a.getTamano(), IZQ, null);

        // Bytes a reservar: IZQ * 8
        sb.append("    slli ").append(IZQ).append(", ").append(IZQ).append(", 3\n");

        // Reservar en el heap
        sb.append("    la   ").append(DER).append(", heap_ptr\n");
        sb.append("    ld   ").append(RES).append(", 0(").append(DER).append(")\n");
        sb.append("    add  ").append(DIR).append(", ").append(RES).append(", ").append(IZQ).append("\n");
        sb.append("    sd   ").append(DIR).append(", 0(").append(DER).append(")\n");
        sb.append("    la   ").append(AUX).append(", heap\n");
        sb.append("    add  ").append(AUX).append(", ").append(AUX).append(", ").append(RES).append("\n");
        sb.append("    mv   ").append(IDX).append(", ").append(AUX).append("\n");
        sb.append("    srli ").append(IZQ).append(", ").append(IZQ).append(", 3\n");
        int n = ctx.siguienteEtiqueta();
        sb.append("Larr_ini_").append(n).append(":\n");
        sb.append("    beq  ").append(IZQ).append(", zero, Larr_fin_").append(n).append("\n");
        sb.append("    sd   zero, 0(").append(AUX).append(")\n");
        sb.append("    addi ").append(AUX).append(", ").append(AUX).append(", 8\n");
        sb.append("    addi ").append(IZQ).append(", ").append(IZQ).append(", -1\n");
        sb.append("    j    Larr_ini_").append(n).append("\n");
        sb.append("Larr_fin_").append(n).append(":\n");

        // Guardar la dirección base en el destino
        almacenar(sb, IDX, a.getDestino());
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
            case "decimalis" -> RutinasConsola.IMPRIMIR_DECIMAL;
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
                if (l.getDestino() != null) {
                    sb.append("    la   ").append(IZQ).append(", ").append(buffer).append("\n");
                    almacenar(sb, IZQ, l.getDestino());
                }
            }
            case "littera" -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_CARACTER);
                sb.append("    jal  ra, ").append(RutinasConsola.LEER_CARACTER).append("\n");
                if (l.getDestino() != null) {
                    almacenar(sb, "a0", l.getDestino());
                }
            }
            case "decimalis" -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_DECIMAL);
                sb.append("    jal  ra, ").append(RutinasConsola.LEER_DECIMAL).append("\n");
                if (l.getDestino() != null) {
                    almacenar(sb, "a0", l.getDestino());
                }
            }
            default -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_ENTERO);
                sb.append("    jal  ra, ").append(RutinasConsola.LEER_ENTERO).append("\n");
                if (l.getDestino() != null) {
                    almacenar(sb, "a0", l.getDestino());
                }
            }
        }
    }

    // CARGAR / GUARDAR

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
            boolean porRef = acc instanceof AccesoVariable v && esParametroPorReferencia(v.getNombre());
            String tipoVar = acc instanceof AccesoVariable v ? tipoDeVariable(v.getNombre()) : null;
            boolean esAgregado = tipoVar != null
                    && (ctx.getTabla().buscarEstructura(tipoVar).isPresent()
                    || ctx.getTabla().buscarVariable(vNombre(acc)).map(s -> s.dimensiones() > 0).orElse(false));

            if (esAgregado) {
                if (porRef) {
                    if (cabe(off)) {
                        sb.append("    ld   ").append(registro).append(", ").append(off).append("(s0)\n");
                    } else {
                        sb.append("    li   ").append(AUX).append(", ").append(off).append("\n");
                        sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
                        sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
                    }
                } else {
                    // local por valor: la dirección es s0 + off.
                    if (cabe(off)) {
                        sb.append("    addi ").append(registro).append(", s0, ").append(off).append("\n");
                    } else {
                        sb.append("    li   ").append(registro).append(", ").append(off).append("\n");
                        sb.append("    add  ").append(registro).append(", s0, ").append(registro).append("\n");
                    }
                }
            } else {
                if (cabe(off)) {
                    sb.append("    ld   ").append(registro).append(", ").append(off).append("(s0)\n");
                } else {
                    sb.append("    li   ").append(AUX).append(", ").append(off).append("\n");
                    sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
                    sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
                }
            }
        }
    }

    private static String vNombre(AccesoMemoria acc) {
        return acc instanceof AccesoVariable v ? v.getNombre() : null;
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
        if (base instanceof AccesoVariable v) {
            int off = offsetDe(v.getNombre());
            if (esParametroPorReferencia(v.getNombre())) {
                if (cabe(off)) {
                    sb.append("    ld   ").append(registro).append(", ").append(off).append("(s0)\n");
                } else {
                    sb.append("    li   ").append(AUX).append(", ").append(off).append("\n");
                    sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
                    sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
                }
            } else {
                if (cabe(off)) {
                    sb.append("    addi ").append(registro).append(", s0, ").append(off).append("\n");
                } else {
                    sb.append("    li   ").append(registro).append(", ").append(off).append("\n");
                    sb.append("    add  ").append(registro).append(", s0, ").append(registro).append("\n");
                }
            }
            return;
        }
        if (base instanceof AccesoAtributo1 attr) {
            direccionCampo(sb, attr.getBase(), attr.getCampo(), registro);
            if (esCampoArregloPuntero(attr)) {
                sb.append("    ld   ").append(registro).append(", 0(").append(registro).append(")\n");
            }
            return;
        }
        throw new UnsupportedOperationException(
                "EmisorRiscVZ: base de arreglo no soportada: " + base.getClass().getSimpleName());
    }

    private boolean esCampoArregloPuntero(AccesoAtributo1 attr) {
        String tipoObj = tipoDeBase(attr.getBase());
        if (tipoObj == null) return false;
        // 1. Clase Z: sus arreglos son punteros.
        var clase = ctx.getTabla().buscarClase(tipoObj);
        if (clase.isPresent()) {
            var a = clase.get().atributos().get(attr.getCampo());
            return a != null && a.dimensiones() > 0;
        }
        // 2. Struct Y: sus arreglos son inline.
        return false;
    }

    private String tipoDeBase(AccesoMemoria base) {
        if (base instanceof AccesoVariable v) {
            if (v.getNombre().equals("this")) return tipoClaseActual;
            return tipoDeVariable(v.getNombre());
        }
        if (base instanceof AccesoAtributo1 at) {
            String tipoPadre = tipoDeBase(at.getBase());
            if (tipoPadre == null) return null;
            var clase = ctx.getTabla().buscarClase(tipoPadre);
            if (clase.isPresent()) {
                var a = clase.get().atributos().get(at.getCampo());
                if (a != null) return a.tipo();
            }
            var est = ctx.getTabla().buscarEstructura(tipoPadre);
            if (est.isPresent()) {
                return est.get().atributos().get(at.getCampo());
            }
        }
        return null;
    }

    private boolean esParametroPorReferencia(String nombre) {
        if (parametrosPorReferencia == null) return false;
        return parametrosPorReferencia.contains(nombre);
    }

    private void direccionCampo(StringBuilder sb, AccesoMemoria base, String campo, String registro) {
        String tipoClase;
        int offObjeto;
        boolean porReferencia = false;

        if (base instanceof AccesoVariable v && v.getNombre().equals("this")) {
            tipoClase = tipoClaseActual;
            offObjeto = offsets.getOrDefault("this", -16);
        } else if (base instanceof AccesoVariable v) {
            tipoClase = tipoDeVariable(v.getNombre());
            offObjeto = offsetDe(v.getNombre());
            porReferencia = esParametroPorReferencia(v.getNombre());
        } else {
            throw new UnsupportedOperationException(
                    "EmisorRiscVZ: base de atributo debe ser 'this' o una variable");
        }

        var estOpt = ctx.getTabla().buscarEstructura(tipoClase);
        if (estOpt.isPresent()) {
            int offCampo = 0;
            for (var entry : estOpt.get().atributos().entrySet()) {
                if (entry.getKey().equals(campo)) break;
                offCampo += 8;
            }
            if (porReferencia) {
                if (cabe(offObjeto)) {
                    sb.append("    ld   ").append(registro).append(", ").append(offObjeto).append("(s0)\n");
                } else {
                    sb.append("    li   ").append(AUX).append(", ").append(offObjeto).append("\n");
                    sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
                    sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
                }
            } else {
                if (cabe(offObjeto)) {
                    sb.append("    addi ").append(registro).append(", s0, ").append(offObjeto).append("\n");
                } else {
                    sb.append("    li   ").append(registro).append(", ").append(offObjeto).append("\n");
                    sb.append("    add  ").append(registro).append(", s0, ").append(registro).append("\n");
                }
            }
            if (offCampo != 0) {
                sb.append("    addi ").append(registro).append(", ").append(registro)
                        .append(", ").append(offCampo).append("\n");
            }
            return;
        }

        int offCampo = calcularOffsetCampo(tipoClase, campo);
        if (cabe(offObjeto)) {
            sb.append("    ld   ").append(registro).append(", ").append(offObjeto).append("(s0)\n");
        } else {
            sb.append("    li   ").append(AUX).append(", ").append(offObjeto).append("\n");
            sb.append("    add  ").append(AUX).append(", s0, ").append(AUX).append("\n");
            sb.append("    ld   ").append(registro).append(", 0(").append(AUX).append(")\n");
        }
        if (offCampo != 0) {
            sb.append("    addi ").append(registro).append(", ").append(registro)
                    .append(", ").append(offCampo).append("\n");
        }
    }

    private String tipoDeVariable(String nombre) {
        return tiposVariablesActuales.getOrDefault(nombre, "?");
    }

    private Map<String, String> tiposVariablesActuales = Map.of();

    private int calcularOffsetCampo(String tipoClase, String campo) {
        return ctx.getDisposicion().offsetCampo(tipoClase, campo);
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
            case "decimalis" -> {
                long escalado;
                if (valor instanceof Double d) escalado = Math.round(d * 1_000_000L);
                else if (valor instanceof Float f) escalado = Math.round(f * 1_000_000L);
                else if (valor instanceof Number n) escalado = Math.round(n.doubleValue() * 1_000_000L);
                else escalado = Math.round(Double.parseDouble(valor.toString()) * 1_000_000L);
                sb.append("    li   ").append(registro).append(", ").append(escalado).append("\n");
            }
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
        return ctx.etiquetaCadena(valor);
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
        return ctx.getDisposicion().tamano(clase);
    }

    private void convertirAString(StringBuilder sb, String tipo) {
        String rutina = switch (tipo == null ? "" : tipo) {
            case "littera" -> RutinasConsola.Z_TO_STRING_CHAR;
            case "bool"    -> RutinasConsola.Z_TO_STRING_BOOL;
            case "decimalis" -> RutinasConsola.Z_TO_STRING_DOUBLE;
            default        -> RutinasConsola.Z_TO_STRING_INT;
        };
        ctx.getRutinasUsadas().add(rutina);
        sb.append("    mv   a1, a0\n");
        sb.append("    jal  ra, ").append(rutina).append("\n");
    }
}