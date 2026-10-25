package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.util.*;

/**
 * Recorre las cuartetas y emite ensamblador RISC-V (RV64).
 * Los offsets de variables y temporales los asigna el GeneradorCuartetasPig
 * en la TablaSimbolosPig. El emisor los consulta para generar ld/sd.
 * Los registros t0-t6 se usan como pool ciclado.
 */
public class EmisorRiscV {

    private final StringBuilder data = new StringBuilder();
    private final StringBuilder text = new StringBuilder();

    private final TablaSimbolosPig tabla;

    private int contadorStrings = 0;

    private int siguienteRegistroTemporal = 0;
    private static final int NUM_TEMPORALES = 7;

    public EmisorRiscV(TablaSimbolosPig tabla) {
        this.tabla = tabla;
    }

    private String nuevoRegistroTemporal() {
        String reg = "t" + (siguienteRegistroTemporal % NUM_TEMPORALES);
        siguienteRegistroTemporal++;
        return reg;
    }

    public String emitir(List<Cuarteta> cuartetas) {
        // El framesize lo calcula la tabla (ya tiene todos los offsets asignados)
        int framesize = tabla.getSiguienteOffset();
        framesize = ((framesize + 15) / 16) * 16;

        text.append(".text\n");
        text.append(".globl main\n");
        text.append("main:\n");
        text.append("    addi sp, sp, -").append(framesize).append("\n");
        text.append("    sd   s0, ").append(framesize - 8).append("(sp)\n");
        text.append("    sd   ra, ").append(framesize - 16).append("(sp)\n");
        text.append("    addi s0, sp, ").append(framesize - 8).append("\n\n");

        for (Cuarteta c : cuartetas) {
            emitirCuarteta(c);
        }

        text.append("\n    ld   ra, -8(s0)\n");
        text.append("    ld   s0, 0(s0)\n");
        text.append("    addi sp, sp, ").append(framesize).append("\n");
        text.append("    li   a7, 10\n");
        text.append("    ecall\n");

        StringBuilder resultado = new StringBuilder();
        if (data.length() > 0) {
            resultado.append(".data\n").append(data).append("\n");
        }
        resultado.append(text);
        return resultado.toString();
    }

    // ========== EMISIÓN DE CUARTETAS ==========

    private void emitirCuarteta(Cuarteta c) {
        if (c instanceof AsignacionVariable a) emitirAsignacion(a);
        else if (c instanceof AsignacionArreglo a) emitirAsignacionArreglo(a);
        else if (c instanceof OperacionBinaria o) emitirOperacion(o);
        else if (c instanceof OperacionUnaria o) emitirUnaria(o);
        else if (c instanceof Condicional1 cond) emitirCondicional(cond);
        else if (c instanceof Salto s) emitirSalto(s);
        else if (c instanceof DefinicionEtiqueta e) emitirEtiqueta(e);
        else if (c instanceof Llamada l) emitirLlamada(l);
        else if (c instanceof Retorno1 r) emitirRetorno(r);
        else if (c instanceof ConversionTipo ct) emitirConversion(ct);
        else if (c instanceof ImprimirPig i) emitirImprimir(i);
        else if (c instanceof LeerPig l) emitirLeer(l);
        else if (c instanceof Continuar1 || c instanceof Romper1) emitirSaltoSimple(c);
    }

    private void emitirAsignacion(AsignacionVariable a) {
        String dest = registrar(a.getDestino());
        String r = nuevoRegistroTemporal();
        cargar(a.getValor(), r);
        text.append("    sd   ").append(r).append(", ").append(dest).append("\n");
    }

    private void emitirAsignacionArreglo(AsignacionArreglo a) {
        String rBase = nuevoRegistroTemporal();
        String rIndice = nuevoRegistroTemporal();
        String rDir = nuevoRegistroTemporal();
        String rValor = nuevoRegistroTemporal();

        cargar(a.getBase(), rBase);
        cargar(a.getIndice(), rIndice);
        text.append("    slli ").append(rDir).append(", ").append(rIndice).append(", 2\n");
        text.append("    add  ").append(rDir).append(", ").append(rBase).append(", ").append(rDir).append("\n");
        cargar(a.getValor(), rValor);
        text.append("    sw   ").append(rValor).append(", 0(").append(rDir).append(")\n");
    }

    private void emitirOperacion(OperacionBinaria o) {
        String rIzq = nuevoRegistroTemporal();
        String rDer = nuevoRegistroTemporal();
        String rRes = nuevoRegistroTemporal();

        cargar(o.getIzquierda(), rIzq);
        cargar(o.getDerecha(), rDer);

        String instr = switch (o.getOperador()) {
            case "+" -> "add";
            case "-" -> "sub";
            case "*" -> "mul";
            case "/" -> "div";
            case "%" -> "rem";
            case "==", "!=" -> "sub";
            case "<" -> "slt";
            case ">" -> "sgt";
            case "<=" -> "sle";
            case ">=" -> "sge";
            case "&&" -> "and";
            case "||" -> "or";
            default -> "add";
        };

        text.append("    ").append(instr).append(" ")
                .append(rRes).append(", ").append(rIzq).append(", ").append(rDer).append("\n");

        if (o.getOperador().equals("==")) {
            text.append("    seqz ").append(rRes).append(", ").append(rRes).append("\n");
        } else if (o.getOperador().equals("!=")) {
            text.append("    snez ").append(rRes).append(", ").append(rRes).append("\n");
        }

        String dest = registrar(o.getDestino());
        text.append("    sd   ").append(rRes).append(", ").append(dest).append("\n");
    }

    private void emitirUnaria(OperacionUnaria o) {
        String rOp = nuevoRegistroTemporal();
        String rRes = nuevoRegistroTemporal();
        cargar(o.getOperando(), rOp);

        if ("-".equals(o.getOperador())) {
            text.append("    neg  ").append(rRes).append(", ").append(rOp).append("\n");
        } else if ("!".equals(o.getOperador())) {
            text.append("    seqz ").append(rRes).append(", ").append(rOp).append("\n");
        }

        String dest = registrar(o.getDestino());
        text.append("    sd   ").append(rRes).append(", ").append(dest).append("\n");
    }

    private void emitirCondicional(Condicional1 c) {
        String rIzq = nuevoRegistroTemporal();
        String rDer = nuevoRegistroTemporal();
        cargar(c.getIzquierda(), rIzq);
        cargar(c.getDerecha(), rDer);

        String branch = switch (c.getOperador()) {
            case "==" -> "beq";
            case "!=" -> "bne";
            case "<" -> "blt";
            case ">" -> "bgt";
            case "<=" -> "ble";
            case ">=" -> "bge";
            default -> "beq";
        };

        text.append("    ").append(branch).append(" ")
                .append(rIzq).append(", ").append(rDer)
                .append(", L").append(c.getEtiqueta()).append("\n");
    }

    private void emitirSalto(Salto s) {
        text.append("    j    L").append(s.getEtiqueta()).append("\n");
    }

    private void emitirEtiqueta(DefinicionEtiqueta e) {
        text.append("L").append(e.getEtiqueta()).append(":\n");
    }

    private void emitirLlamada(Llamada l) {
        for (int i = 0; i < l.getArgumentos().size() && i < 8; i++) {
            String r = nuevoRegistroTemporal();
            cargar(l.getArgumentos().get(i), r);
            if (!("a" + i).equals(r)) {
                text.append("    mv   a").append(i).append(", ").append(r).append("\n");
            }
        }
        text.append("    jal  ra, ").append(l.getNombreFuncion()).append("\n");
        if (l.getDestino() != null) {
            String dest = registrar(l.getDestino());
            text.append("    sd   a0, ").append(dest).append("\n");
        }
    }

    private void emitirRetorno(Retorno1 r) {
        if (r.getValor() != null) {
            String reg = nuevoRegistroTemporal();
            cargar(r.getValor(), reg);
            if (!"a0".equals(reg)) {
                text.append("    mv   a0, ").append(reg).append("\n");
            }
        }
        text.append("    ld   ra, -8(s0)\n");
        text.append("    ld   s0, 0(s0)\n");
        text.append("    ret\n");
    }

    private void emitirConversion(ConversionTipo c) {
        String r = nuevoRegistroTemporal();
        cargar(c.getFuente(), r);
        String dest = registrar(c.getDestino());
        // TODO: conversiones de punto flotante (RV64 con F/D). Por ahora solo enteros.
        text.append("    sd   ").append(r).append(", ").append(dest).append("\n");
    }

    private void emitirImprimir(ImprimirPig i) {
        String r = nuevoRegistroTemporal();
        cargar(i.getValor(), r);
        if (!"a1".equals(r)) {
            text.append("    mv   a1, ").append(r).append("\n");
        }
        int syscall = switch (i.getTipo()) {
            case "numerus" -> 1;
            case "decimalis" -> 2;
            case "textum" -> 4;
            case "littera" -> 11;
            default -> 1;
        };
        text.append("    li   a0, ").append(syscall).append("\n");
        text.append("    ecall\n");
    }

    private void emitirLeer(LeerPig l) {
        int syscall = switch (l.getTipo()) {
            case "numerus" -> 5;
            case "decimalis" -> 6;
            case "textum" -> 8;
            case "littera" -> 12;
            default -> 5;
        };
        text.append("    li   a0, ").append(syscall).append("\n");
        text.append("    ecall\n");
        String dest = registrar(l.getDestino());
        text.append("    sd   a0, ").append(dest).append("\n");
    }

    private void emitirSaltoSimple(Cuarteta c) {
        int etiqueta;
        if (c instanceof Continuar1 cont) etiqueta = cont.getEtiqueta();
        else if (c instanceof Romper1 romp) etiqueta = romp.getEtiqueta();
        else return;
        text.append("    j    L").append(etiqueta).append("\n");
    }

    // ========== HELPERS ==========

    /** Devuelve el offset (fp-relativo) de una variable o temporal, consultando la tabla. */
    private String registrar(AccesoMemoria acc) {
        if (acc instanceof AccesoVariable v) {
            int offset = tabla.buscarVariable(v.getNombre())
                    .map(TablaSimbolosPig.SimboloVariable::offset)
                    .orElse(0);
            return offset + "(s0)";
        } else if (acc instanceof AccesoTemporal t) {
            int offset = tabla.getOffsetTemporal(t.getNumero());
            return offset + "(s0)";
        }
        return "0(s0)";
    }

    private void cargar(AccesoMemoria acc, String registro) {
        if (acc instanceof Literal l) {
            String tipo = l.getTipo();
            Object valor = l.getValor();

            if ("numerus".equals(tipo)) {
                text.append("    li   ").append(registro).append(", ").append(valor).append("\n");
            } else if ("bool".equals(tipo)) {
                boolean v = valor instanceof Boolean b ? b : (Integer.parseInt(valor.toString()) != 0);
                text.append("    li   ").append(registro).append(", ").append(v ? 1 : 0).append("\n");
            } else if ("littera".equals(tipo)) {
                int ascii = valor instanceof Character c ? (int) c : Integer.parseInt(valor.toString());
                text.append("    li   ").append(registro).append(", ").append(ascii).append("\n");
            } else if ("textum".equals(tipo)) {
                String etiqueta = "str_" + contadorStrings++;
                data.append("    ").append(etiqueta).append(": .asciiz \"").append(valor).append("\"\n");
                text.append("    la   ").append(registro).append(", ").append(etiqueta).append("\n");
            }
        } else if (acc instanceof AccesoArreglo arr) {
            String rBase = nuevoRegistroTemporal();
            String rIndice = nuevoRegistroTemporal();
            String rDir = nuevoRegistroTemporal();
            cargar(arr.getBase(), rBase);
            cargar(arr.getIndice(), rIndice);
            text.append("    slli ").append(rDir).append(", ").append(rIndice).append(", 2\n");
            text.append("    add  ").append(rDir).append(", ").append(rBase).append(", ").append(rDir).append("\n");
            text.append("    lw   ").append(registro).append(", 0(").append(rDir).append(")\n");
        } else if (acc instanceof AccesoVariable || acc instanceof AccesoTemporal) {
            text.append("    ld   ").append(registro).append(", ").append(registrar(acc)).append("\n");
        }
    }
}