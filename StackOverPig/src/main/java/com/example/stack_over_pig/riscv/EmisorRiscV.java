package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;

import java.util.*;

/**
 * Recorre las cuartetas y emite ensamblador RISC-V (RV32IM) para Venus.
 * Usa fp (s0) para offsets fijos. Calcula FRAMESIZE al recorrer las cuartetas.
 *
 * Los registros t0-t6 se usan como pool ciclado: cada cuarteta pide
 * registros frescos al pool para cargar operandos y calcular resultados.
 * Los temporales del C3D viven en pila (offsets desde fp).
 */
public class EmisorRiscV {

    private final StringBuilder data = new StringBuilder();
    private final StringBuilder text = new StringBuilder();

    private final Map<String, Integer> offsets = new LinkedHashMap<>();
    private final Map<String, String> tipos = new LinkedHashMap<>();
    private int siguienteOffset = 8; // 0(fp) y -4(fp) reservados para s0 y ra
    private int contadorStrings = 0;
    private int contadorFloats = 0;

    // Pool de registros temporales (t0-t6)
    private int siguienteRegistroTemporal = 0;
    private static final int NUM_TEMPORALES = 7;

    private String nuevoRegistroTemporal() {
        String reg = "t" + (siguienteRegistroTemporal % NUM_TEMPORALES);
        siguienteRegistroTemporal++;
        return reg;
    }

    public String emitir(List<Cuarteta> cuartetas) {
        // 1. Primera pasada: asignar offsets a variables y temporales
        for (Cuarteta c : cuartetas) {
            asignarOffsets(c);
        }

        // 2. Calcular FRAMESIZE
        int framesize = siguienteOffset;
        framesize = ((framesize + 15) / 16) * 16; // múltiplo de 16

        // 3. Emitir cabecera
        text.append(".text\n");
        text.append(".globl main\n");
        text.append("main:\n");

        // 4. Prólogo
        text.append("    addi sp, sp, -").append(framesize).append("\n");
        text.append("    sw   s0, ").append(framesize - 4).append("(sp)\n");
        text.append("    sw   ra, ").append(framesize - 8).append("(sp)\n");
        text.append("    addi s0, sp, ").append(framesize - 4).append("\n\n");

        // 5. Segunda pasada: emitir cuartetas
        for (Cuarteta c : cuartetas) {
            emitirCuarteta(c);
        }

        // 6. Epílogo
        text.append("\n    lw   ra, -4(s0)\n");
        text.append("    lw   s0, 0(s0)\n");
        text.append("    addi sp, sp, ").append(framesize).append("\n");
        text.append("    li   a7, 10\n");
        text.append("    ecall\n");

        // 7. Unir .data y .text
        StringBuilder resultado = new StringBuilder();
        if (data.length() > 0) {
            resultado.append(".data\n").append(data).append("\n");
        }
        resultado.append(text);
        return resultado.toString();
    }

    // ========== ASIGNACIÓN DE OFFSETS ==========

    private void asignarOffsets(Cuarteta c) {
        if (c instanceof AsignacionVariable a) {
            asignarAcceso(a.getDestino());
            asignarAcceso(a.getValor());
        } else if (c instanceof OperacionBinaria o) {
            asignarAcceso(o.getDestino());
            asignarAcceso(o.getIzquierda());
            asignarAcceso(o.getDerecha());
        } else if (c instanceof OperacionUnaria o) {
            asignarAcceso(o.getDestino());
            asignarAcceso(o.getOperando());
        } else if (c instanceof Condicional1 cond) {
            asignarAcceso(cond.getIzquierda());
            asignarAcceso(cond.getDerecha());
        } else if (c instanceof Llamada l) {
            if (l.getDestino() != null) asignarAcceso(l.getDestino());
            for (AccesoMemoria a : l.getArgumentos()) asignarAcceso(a);
        } else if (c instanceof Retorno1 r) {
            if (r.getValor() != null) asignarAcceso(r.getValor());
        } else if (c instanceof ImprimirPig i) {
            asignarAcceso(i.getValor());
        } else if (c instanceof LeerPig l) {
            asignarAcceso(l.getDestino());
        } else if (c instanceof ConversionTipo ct) {
            asignarAcceso(ct.getDestino());
            asignarAcceso(ct.getFuente());
        }
    }

    private void asignarAcceso(AccesoMemoria acc) {
        if (acc instanceof AccesoVariable v) {
            offsets.computeIfAbsent(v.getNombre(), k -> {
                int off = -siguienteOffset;
                siguienteOffset += 4;
                tipos.put(k, v.getTipo());
                return off;
            });
        } else if (acc instanceof AccesoTemporal t) {
            String key = "t" + t.getNumero();
            offsets.computeIfAbsent(key, k -> {
                int off = -siguienteOffset;
                siguienteOffset += 4;
                tipos.put(k, t.getTipo());
                return off;
            });
        }
    }

    // ========== EMISIÓN DE CUARTETAS ==========

    private void emitirCuarteta(Cuarteta c) {
        if (c instanceof AsignacionVariable a) emitirAsignacion(a);
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
        text.append("    sw   ").append(r).append(", ").append(dest).append("\n");
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
        text.append("    sw   ").append(rRes).append(", ").append(dest).append("\n");
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
        text.append("    sw   ").append(rRes).append(", ").append(dest).append("\n");
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
            text.append("    sw   a0, ").append(dest).append("\n");
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
        text.append("    lw   ra, -4(s0)\n");
        text.append("    lw   s0, 0(s0)\n");
        text.append("    ret\n");
    }

    private void emitirConversion(ConversionTipo c) {
        String r = nuevoRegistroTemporal();
        cargar(c.getFuente(), r);
        String dest = registrar(c.getDestino());
        if ("float".equals(c.getTipoC())) {
            text.append("    fcvt.s.w ft0, ").append(r).append("\n");
            text.append("    fsw  ft0, ").append(dest).append("\n");
        } else if ("int".equals(c.getTipoC())) {
            text.append("    fcvt.w.s ").append(r).append(", ft0\n");
            text.append("    sw   ").append(r).append(", ").append(dest).append("\n");
        } else {
            text.append("    sw   ").append(r).append(", ").append(dest).append("\n");
        }
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
        text.append("    sw   a0, ").append(dest).append("\n");
    }

    private void emitirSaltoSimple(Cuarteta c) {
        int etiqueta;
        if (c instanceof Continuar1 cont) etiqueta = cont.getEtiqueta();
        else if (c instanceof Romper1 romp) etiqueta = romp.getEtiqueta();
        else return;
        text.append("    j    L").append(etiqueta).append("\n");
    }

    // ========== HELPERS ==========

    private String registrar(AccesoMemoria acc) {
        if (acc instanceof AccesoVariable v) {
            return offsets.get(v.getNombre()) + "(s0)";
        } else if (acc instanceof AccesoTemporal t) {
            return offsets.get("t" + t.getNumero()) + "(s0)";
        }
        return "0(s0)";
    }

    private void cargar(AccesoMemoria acc, String registro) {
        if (acc instanceof Literal l) {
            if ("numerus".equals(l.getTipo()) || "bool".equals(l.getTipo())) {
                text.append("    li   ").append(registro).append(", ").append(l.getValor()).append("\n");
            } else if ("littera".equals(l.getTipo())) {
                text.append("    li   ").append(registro).append(", ").append((int) ((Character) l.getValor()).charValue()).append("\n");
            } else if ("decimalis".equals(l.getTipo())) {
                String etiqueta = "flt_" + contadorFloats++;
                data.append("    ").append(etiqueta).append(": .float ").append(l.getValor()).append("\n");
                text.append("    flw  ft0, ").append(etiqueta).append(", t0\n");
                text.append("    fmv.x.w ").append(registro).append(", ft0\n");
            } else if ("textum".equals(l.getTipo())) {
                String etiqueta = "str_" + contadorStrings++;
                data.append("    ").append(etiqueta).append(": .asciiz \"").append(l.getValor()).append("\"\n");
                text.append("    la   ").append(registro).append(", ").append(etiqueta).append("\n");
            }
        } else if (acc instanceof AccesoVariable || acc instanceof AccesoTemporal) {
            text.append("    lw   ").append(registro).append(", ").append(registrar(acc)).append("\n");
        }
    }
}