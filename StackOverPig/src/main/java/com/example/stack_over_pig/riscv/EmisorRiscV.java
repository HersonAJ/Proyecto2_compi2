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
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Emite el programa RISC-V completo: _start, main (Pig) y funciones de Zetariano.
 * Usa ContextoEmision para compartir estado con EmisorRiscVZ.
 */
public class EmisorRiscV {

    private static final String IZQ = "t0";
    private static final String DER = "t1";
    private static final String RES = "t2";
    private static final String DIR = "t3";
    private static final String IDX = "t4";
    private static final String AUX = "t6";

    private static final boolean RAMAS_LARGAS = false;
    private static final boolean IMPRIMIR_CUARTETAS_EN_CONSOLA = true;
    private static final boolean COMENTAR_CUARTETAS = true;

    private static final int BYTES_SLOT = 8;
    private static final int CAPACIDAD_LECTURA_TEXTO = 256;

    private final TablaSimbolosPig tabla;
    private final int tamanoHeap;
    private ContextoEmision ctx;

    public EmisorRiscV(TablaSimbolosPig tabla, int tamanoHeap) {
        this.tabla = tabla;
        this.tamanoHeap = tamanoHeap;
    }

    public EmisorRiscV(TablaSimbolosPig tabla) {
        this(tabla, 0);
    }

    private record Ubicacion(int offset, String nombre) {}

    public String emitir(List<Cuarteta> main, List<FuncionRiscV> funciones) {
        return emitir(main, funciones, TablaMetodosVirtuales.vacia());
    }

    public String emitir(List<Cuarteta> main, List<FuncionRiscV> funciones, TablaMetodosVirtuales virtuales) {
        ctx = new ContextoEmision(tabla, tamanoHeap);
        ctx.setVirtuales(virtuales);

        // 1. Emitir main.
        int numero = 0;
        for (Cuarteta c : main) {
            numero++;
            if (COMENTAR_CUARTETAS) {
                ctx.getText().append("    # [").append(numero).append("] ").append(describir(c)).append("\n");
            }
            try {
                emitirCuarteta(c);
            } catch (RuntimeException e) {
                throw new IllegalStateException("EmisorRiscV: falló cuarteta #" + numero
                        + " -> " + describir(c) + " | " + e.getMessage(), e);
            }
        }

        int framesize = ((tabla.getSiguienteOffset() + 8 + 15) / 16) * 16;

        // 2. Emitir funciones de Z.
        EmisorRiscVZ emisorZ = new EmisorRiscVZ(ctx);
        for (FuncionRiscV f : funciones) {
            emisorZ.emitirFuncion(f);
        }

        // 3. Ensamblar el .s completo.
        StringBuilder out = new StringBuilder();
        out.append("# Generado por stack_over_pig - RISC-V RV64 (write=64, read=63, exit=93)\n");

        if (ctx.getData().length() > 0) {
            out.append(".data\n").append(ctx.getData()).append("\n");
        }

        // .bss: buffers de lectura + heap estático (si hay objetos)
        StringBuilder bssFinal = new StringBuilder(ctx.getBss());
        bssFinal.append(ctx.getVirtuales().seccionData());
        if (tamanoHeap > 0) {
            bssFinal.append("    .align 3\n");
            bssFinal.append("    heap:       .space ").append(tamanoHeap).append("\n");
            bssFinal.append("    .align 3\n");
            bssFinal.append("    heap_ptr:   .dword 0\n");
        }
        if (bssFinal.length() > 0) {
            if (ctx.getData().length() == 0) {
                out.append(".data\n");
            }
            out.append(bssFinal).append("\n");
        }

        out.append(".text\n");
        out.append(".globl _start\n");
        out.append("_start:\n");
        out.append(ctx.getVirtuales().codigoInicializacion());
        out.append("    jal  ra, main\n");
        out.append("    li   a0, 0\n");
        out.append("    li   a7, 93\n");
        out.append("    ecall\n\n");

        out.append("main:\n");
        out.append(prologo(framesize)).append("\n");
        out.append(ctx.getText());
        out.append("__epilogo_main:\n");
        out.append(epilogo(framesize));

        out.append(ctx.getFunciones());

        if (!ctx.getRutinasUsadas().isEmpty()) {
            out.append("\n# ---------- rutinas de entrada/salida ----------\n");
            out.append(RutinasConsola.generar(ctx.getRutinasUsadas()));
        }
        return out.toString();
    }

    public String emitir(List<Cuarteta> cuartetas) {
        return emitir(cuartetas, List.of());
    }

    private static String prologo(int framesize) {
        StringBuilder sb = new StringBuilder();
        if (framesize <= 2048) {
            sb.append("    addi sp, sp, -").append(framesize).append("\n");
            sb.append("    sd   s0, ").append(framesize - 8).append("(sp)\n");
            sb.append("    sd   ra, ").append(framesize - 16).append("(sp)\n");
            sb.append("    addi s0, sp, ").append(framesize - 8).append("\n");
        } else {
            sb.append("    li   t0, ").append(framesize).append("\n");
            sb.append("    sub  sp, sp, t0\n");
            sb.append("    add  t0, sp, t0\n");
            sb.append("    sd   s0, -8(t0)\n");
            sb.append("    sd   ra, -16(t0)\n");
            sb.append("    addi s0, t0, -8\n");
        }
        return sb.toString();
    }

    private static String epilogo(int framesize) {
        StringBuilder sb = new StringBuilder();
        sb.append("    ld   ra, -8(s0)\n");
        sb.append("    ld   s0, 0(s0)\n");
        if (framesize <= 2048) {
            sb.append("    addi sp, sp, ").append(framesize).append("\n");
        } else {
            sb.append("    li   t0, ").append(framesize).append("\n");
            sb.append("    add  sp, sp, t0\n");
        }
        sb.append("    ret\n");
        return sb.toString();
    }

    // CUARTETAS

    private void emitirCuarteta(Cuarteta c) {
        if (c instanceof AsignacionArreglo a) emitirAsignacionArreglo(a);
        else if (c instanceof AsignacionAtributo a) emitirAsignacionAtributo(a);
        else if (c instanceof AsignacionVariable a) emitirAsignacion(a);
        else if (c instanceof OperacionBinaria o) emitirOperacion(o);
        else if (c instanceof OperacionUnaria o) emitirUnaria(o);
        else if (c instanceof Condicional1 cond) emitirCondicional(cond);
        else if (c instanceof Salto s) emitirSalto(s.getEtiqueta());
        else if (c instanceof DefinicionEtiqueta e) etiqueta("L" + e.getEtiqueta());
        else if (c instanceof Llamada l) emitirLlamada(l);
        else if (c instanceof NewObjeto n) emitirNewObjeto(n);
        else if (c instanceof NewArreglo a) emitirNewArreglo(a);
        else if (c instanceof LlamadaMetodo1 lm) emitirLlamadaMetodo(lm);
        else if (c instanceof Retorno1 r) emitirRetorno(r);
        else if (c instanceof ConversionTipo ct) emitirConversion(ct);
        else if (c instanceof ImprimirPig i) emitirImprimir(i);
        else if (c instanceof LeerPig l) emitirLeer(l);
        else if (c instanceof Continuar1 cont) emitirSalto(cont.getEtiqueta());
        else if (c instanceof Romper1 romp) emitirSalto(romp.getEtiqueta());
        else throw new UnsupportedOperationException(
                    "EmisorRiscV: cuarteta no soportada: " + c.getClass().getSimpleName());
    }

    private void emitirNewArreglo(NewArreglo a) {
        cargar(a.getTamano(), IZQ);
        linea("slli %s, %s, 3", IZQ, IZQ);
        linea("la   %s, heap_ptr", DER);
        linea("ld   %s, 0(%s)", RES, DER);
        linea("add  %s, %s, %s", DIR, RES, IZQ);
        linea("sd   %s, 0(%s)", DIR, DER);
        linea("la   %s, heap", AUX);
        linea("add  %s, %s, %s", AUX, AUX, RES);
        linea("mv   %s, %s", IDX, AUX);
        linea("srli %s, %s, 3", IZQ, IZQ);
        int n = ctx.siguienteEtiqueta();
        etiqueta("Larr_ini_" + n);
        linea("beq  %s, zero, Larr_fin_%d", IZQ, n);
        linea("sd   zero, 0(%s)", AUX);
        linea("addi %s, %s, 8", AUX, AUX);
        linea("addi %s, %s, -1", IZQ, IZQ);
        linea("j    Larr_ini_%d", n);
        etiqueta("Larr_fin_" + n);
        almacenar(IDX, a.getDestino());
    }

    private void emitirAsignacion(AsignacionVariable a) {
        cargar(a.getValor(), IZQ);
        almacenar(IZQ, a.getDestino());
    }

    private void emitirAsignacionArreglo(AsignacionArreglo a) {
        cargar(a.getValor(), IZQ);
        almacenarEnArreglo(IZQ, a.getBase(), a.getIndice());
    }

    private void emitirOperacion(OperacionBinaria o) {
        cargar(o.getIzquierda(), IZQ);
        cargar(o.getDerecha(), DER);
        switch (o.getOperador()) {
            case "+" -> linea("add  %s, %s, %s", RES, IZQ, DER);
            case "-" -> linea("sub  %s, %s, %s", RES, IZQ, DER);
            case "*" -> linea("mul  %s, %s, %s", RES, IZQ, DER);
            case "/" -> linea("div  %s, %s, %s", RES, IZQ, DER);
            case "%" -> linea("rem  %s, %s, %s", RES, IZQ, DER);
            case "<" -> linea("slt  %s, %s, %s", RES, IZQ, DER);
            case ">" -> linea("slt  %s, %s, %s", RES, DER, IZQ);
            case "<=" -> { linea("slt  %s, %s, %s", RES, DER, IZQ); linea("xori %s, %s, 1", RES, RES); }
            case ">=" -> { linea("slt  %s, %s, %s", RES, IZQ, DER); linea("xori %s, %s, 1", RES, RES); }
            case "==" -> { linea("sub  %s, %s, %s", RES, IZQ, DER); linea("sltiu %s, %s, 1", RES, RES); }
            case "!=" -> { linea("sub  %s, %s, %s", RES, IZQ, DER); linea("sltu %s, zero, %s", RES, RES); }
            case "&&" -> {
                linea("sltu %s, zero, %s", IZQ, IZQ);
                linea("sltu %s, zero, %s", DER, DER);
                linea("and  %s, %s, %s", RES, IZQ, DER);
            }
            case "||" -> { linea("or   %s, %s, %s", RES, IZQ, DER); linea("sltu %s, zero, %s", RES, RES); }
            default -> throw new UnsupportedOperationException("Operador: " + o.getOperador());
        }
        almacenar(RES, o.getDestino());
    }

    private void emitirUnaria(OperacionUnaria o) {
        cargar(o.getOperando(), IZQ);
        switch (o.getOperador()) {
            case "-" -> linea("sub  %s, zero, %s", RES, IZQ);
            case "!" -> linea("sltiu %s, %s, 1", RES, IZQ);
            case "+" -> linea("mv   %s, %s", RES, IZQ);
            default -> throw new UnsupportedOperationException("Unario: " + o.getOperador());
        }
        almacenar(RES, o.getDestino());
    }

    private void emitirCondicional(Condicional1 c) {
        cargar(c.getIzquierda(), IZQ);
        cargar(c.getDerecha(), DER);
        String destino = "L" + c.getEtiqueta();
        switch (c.getOperador()) {
            case "==" -> rama("beq", IZQ, DER, destino);
            case "!=" -> rama("bne", IZQ, DER, destino);
            case "<" -> rama("blt", IZQ, DER, destino);
            case ">=" -> rama("bge", IZQ, DER, destino);
            case ">" -> rama("blt", DER, IZQ, destino);
            case "<=" -> rama("bge", DER, IZQ, destino);
            default -> throw new UnsupportedOperationException("Comparador: " + c.getOperador());
        }
    }

    private void rama(String instr, String r1, String r2, String destino) {
        if (!RAMAS_LARGAS) {
            linea("%s  %s, %s, %s", instr, r1, r2, destino);
            return;
        }
        String inv = switch (instr) {
            case "beq" -> "bne";
            case "bne" -> "beq";
            case "blt" -> "bge";
            default -> "blt";
        };
        String salto = "__salto_" + (ctx.siguienteSalto());
        linea("%s  %s, %s, %s", inv, r1, r2, salto);
        linea("j    %s", destino);
        etiqueta(salto);
    }

    private void emitirSalto(int etiqueta) { linea("j    L%d", etiqueta); }

    private void emitirLlamada(Llamada l) {
        boolean esFuncionLibre = !l.getNombreFuncion().contains("_constructor")
                && !ctx.getVirtuales().esVirtual(l.getNombreFuncion());
        int base = esFuncionLibre ? 0 : 1;

        List<AccesoMemoria> args = l.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(args.get(i), "a" + (base + i));
        }
        linea("jal  ra, %s", l.getNombreFuncion());
        if (l.getDestino() != null) almacenar("a0", l.getDestino());
    }

    private void emitirRetorno(Retorno1 r) {
        if (r.getValor() != null) cargar(r.getValor(), "a0");
        linea("j    __epilogo_main");
    }

    private void emitirConversion(ConversionTipo c) {
        cargar(c.getFuente(), IZQ);
        almacenar(IZQ, c.getDestino());
    }

    // E/S

    private void emitirImprimir(ImprimirPig i) {
        String rutina = switch (i.getTipo()) {
            case "textum" -> RutinasConsola.IMPRIMIR_CADENA;
            case "littera" -> RutinasConsola.IMPRIMIR_CARACTER;
            case "decimalis" -> RutinasConsola.IMPRIMIR_DECIMAL;
            default -> RutinasConsola.IMPRIMIR_ENTERO;
        };
        ctx.getRutinasUsadas().add(rutina);
        cargar(i.getValor(), "a0");
        linea("jal  ra, %s", rutina);
    }

    private void emitirLeer(LeerPig l) {
        switch (l.getTipo()) {
            case "textum" -> {
                String buffer = "buf_lectura_" + ctx.siguienteBuffer();
                ctx.getBss().append("    ").append(buffer).append(": .space ").append(CAPACIDAD_LECTURA_TEXTO).append("\n");
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_CADENA);
                linea("la   a0, %s", buffer);
                linea("li   a1, %d", CAPACIDAD_LECTURA_TEXTO);
                linea("jal  ra, %s", RutinasConsola.LEER_CADENA);
                if (l.getDestino() != null) {
                    linea("la   %s, %s", IZQ, buffer);
                    almacenar(IZQ, l.getDestino());
                }
            }
            case "littera" -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_CARACTER);
                linea("jal  ra, %s", RutinasConsola.LEER_CARACTER);
                if (l.getDestino() != null) {
                    almacenar("a0", l.getDestino());
                }
            }
            case "decimalis" -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_DECIMAL);
                linea("jal  ra, %s", RutinasConsola.LEER_DECIMAL);
                if (l.getDestino() != null) {
                    almacenar("a0", l.getDestino());
                }
            }
            default -> {
                ctx.getRutinasUsadas().add(RutinasConsola.LEER_ENTERO);
                linea("jal  ra, %s", RutinasConsola.LEER_ENTERO);
                if (l.getDestino() != null) {
                    almacenar("a0", l.getDestino());
                }
            }
        }
    }
    // CARGAR / GUARDAR

    private void cargar(AccesoMemoria acc, String registro) {
        if (acc instanceof Literal l) {
            cargarLiteral(l, registro);
        } else if (acc instanceof AccesoAtributo1 attr) {
            direccionCampo(attr.getBase(), attr.getCampo(), DIR);
            linea("ld   %s, 0(%s)", registro, DIR);
        } else if (acc instanceof AccesoArreglo arr) {
            cargar(arr.getIndice(), registro);
            direccionBase(arr.getBase(), DIR);
            linea("slli %s, %s, 3", registro, registro);
            linea("add  %s, %s, %s", DIR, DIR, registro);
            linea("ld   %s, 0(%s)", registro, DIR);
        } else {
            Ubicacion u = ubicar(acc);
            boolean esStruct = esStructPorValor(acc);
            if (esStruct) {
                if (cabe(u.offset())) {
                    linea("addi %s, s0, %d   # &%s", registro, u.offset(), u.nombre());
                } else {
                    linea("li   %s, %d", registro, u.offset());
                    linea("add  %s, s0, %s   # &%s", registro, registro, u.nombre());
                }
            } else {
                accesoPila("ld", registro, u);
            }
        }
    }

    private boolean esStructPorValor(AccesoMemoria acc) {
        if (!(acc instanceof AccesoVariable v)) return false;
        TablaSimbolosPig.SimboloVariable s = tabla.buscarVariable(v.getNombre()).orElse(null);
        return s != null && s.esEstructura();
    }

    private void almacenar(String registro, AccesoMemoria destino) {
        if (destino instanceof AccesoArreglo arr) {
            almacenarEnArreglo(registro, arr.getBase(), arr.getIndice());
        } else if (destino instanceof AccesoAtributo1 attr) {
            direccionCampo(attr.getBase(), attr.getCampo(), DIR);
            linea("sd   %s, 0(%s)", registro, DIR);
        } else {
            accesoPila("sd", registro, ubicar(destino));
        }
    }

    private void almacenarEnArreglo(String regValor, AccesoMemoria base, AccesoMemoria indice) {
        cargar(indice, IDX);
        direccionBase(base, DIR);
        linea("slli %s, %s, 3", IDX, IDX);
        linea("add  %s, %s, %s", DIR, DIR, IDX);
        linea("sd   %s, 0(%s)", regValor, DIR);
    }

    private void direccionBase(AccesoMemoria base, String registro) {
        if (!(base instanceof AccesoVariable v)) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: base de arreglo debe ser variable");
        }
        TablaSimbolosPig.SimboloVariable s = simbolo(v);
        if (s.dimensiones() == 0) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: '" + v.getNombre() + "' no es arreglo");
        }
        if (cabe(s.offset())) {
            linea("addi %s, s0, %d   # &%s[0]", registro, s.offset(), v.getNombre());
        } else {
            linea("li   %s, %d", registro, s.offset());
            linea("add  %s, s0, %s   # &%s[0]", registro, registro, v.getNombre());
        }
    }

    private void cargarLiteral(Literal l, String registro) {
        Object valor = l.getValor();
        switch (l.getTipo()) {
            case "numerus" -> linea("li   %s, %s", registro, valor);
            case "bool" -> linea("li   %s, %d", registro, esVerdadero(valor) ? 1 : 0);
            case "littera" -> linea("li   %s, %d", registro, codigoCaracter(valor));
            case "textum" -> linea("la   %s, %s", registro, etiquetaCadena(String.valueOf(valor)));
            case "decimalis" -> {
                long escalado;
                if (valor instanceof Double d) escalado = Math.round(d * 1_000_000L);
                else if (valor instanceof Float f) escalado = Math.round(f * 1_000_000L);
                else if (valor instanceof Number n) escalado = Math.round(n.doubleValue() * 1_000_000L);
                else escalado = Math.round(Double.parseDouble(valor.toString()) * 1_000_000L);
                linea("li   %s, %d", registro, escalado);
            }
            case "null" -> linea("li   %s, 0", registro);
            default -> throw new IllegalArgumentException("Tipo literal: " + l.getTipo());
        }
    }

    private Ubicacion ubicar(AccesoMemoria acc) {
        if (acc instanceof AccesoVariable v) {
            return new Ubicacion(simbolo(v).offset(), v.getNombre());
        }
        if (acc instanceof AccesoTemporal t) {
            return new Ubicacion(tabla.asignarOffsetTemporal(t.getNumero()), "t" + t.getNumero());
        }
        throw new IllegalStateException("EmisorRiscV: no ubicable: " + describir(acc));
    }

    private TablaSimbolosPig.SimboloVariable simbolo(AccesoVariable v) {
        TablaSimbolosPig.SimboloVariable s = tabla.buscarVariable(v.getNombre())
                .orElseThrow(() -> new IllegalStateException(
                        "EmisorRiscV: variable '" + v.getNombre() + "' no en tabla"));
        if (s.offset() > -16) {
            throw new IllegalStateException(
                    "EmisorRiscV: '" + v.getNombre() + "' offset " + s.offset()
                            + " (no se le asignó posición en la pila)");
        }
        return s;
    }

    private static boolean cabe(int offset) { return offset >= -2048 && offset <= 2047; }

    private void accesoPila(String instr, String registro, Ubicacion u) {
        if (cabe(u.offset())) {
            linea("%s   %s, %d(s0)   # %s", instr, registro, u.offset(), u.nombre());
        } else {
            linea("li   %s, %d", AUX, u.offset());
            linea("add  %s, s0, %s", AUX, AUX);
            linea("%s   %s, 0(%s)   # %s", instr, registro, AUX, u.nombre());
        }
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

    private static boolean esVerdadero(Object valor) {
        if (valor instanceof Boolean b) return b;
        if (valor instanceof Number n) return n.longValue() != 0;
        String s = String.valueOf(valor).trim().toLowerCase();
        return s.equals("true") || s.equals("verdadero") || s.equals("verum") || s.equals("1");
    }

    private static int codigoCaracter(Object valor) {
        if (valor instanceof Character c) return c;
        if (valor instanceof Number n) return n.intValue();
        String s = String.valueOf(valor);
        if (s.length() == 1) return s.charAt(0);
        if (s.length() == 2 && s.charAt(0) == '\\') {
            return switch (s.charAt(1)) {
                case 'n' -> 10; case 't' -> 9; case 'r' -> 13; case '0' -> 0;
                default -> s.charAt(1);
            };
        }
        return Integer.parseInt(s);
    }

    //DESCRIBIR

    private static String describir(AccesoMemoria acc) {
        try {
            if (acc == null) return "null";
            if (acc instanceof AccesoVariable v) return "var:" + v.getNombre();
            if (acc instanceof AccesoTemporal t) return "t" + t.getNumero();
            if (acc instanceof Literal l) return "lit:" + l.getValor() + ":" + l.getTipo();
            if (acc instanceof AccesoArreglo a)
                return "arr:" + describir(a.getBase()) + "[" + describir(a.getIndice()) + "]";
            return acc.getClass().getSimpleName();
        } catch (RuntimeException e) { return acc.getClass().getSimpleName(); }
    }

    private static String describir(Cuarteta c) {
        String d;
        try {
            if (c instanceof AsignacionArreglo a)
                d = "AsignacionArreglo base=" + describir(a.getBase()) + " indice=" + describir(a.getIndice()) + " valor=" + describir(a.getValor());
            else if (c instanceof AsignacionVariable a)
                d = "AsignacionVariable " + describir(a.getDestino()) + " = " + describir(a.getValor());
            else if (c instanceof OperacionBinaria o)
                d = "OperacionBinaria " + describir(o.getDestino()) + " = " + describir(o.getIzquierda()) + " " + o.getOperador() + " " + describir(o.getDerecha());
            else if (c instanceof OperacionUnaria o)
                d = "OperacionUnaria " + describir(o.getDestino()) + " = " + o.getOperador() + " " + describir(o.getOperando());
            else if (c instanceof Condicional1 k)
                d = "Condicional1 if " + describir(k.getIzquierda()) + " " + k.getOperador() + " " + describir(k.getDerecha()) + " goto L" + k.getEtiqueta();
            else if (c instanceof Salto s) d = "Salto goto L" + s.getEtiqueta();
            else if (c instanceof DefinicionEtiqueta e) d = "Etiqueta L" + e.getEtiqueta() + ":";
            else if (c instanceof ImprimirPig i) d = "ImprimirPig " + i.getTipo() + " " + describir(i.getValor());
            else if (c instanceof LeerPig l) d = "LeerPig " + l.getTipo() + " -> " + describir(l.getDestino());
            else if (c instanceof Llamada l) {
                StringBuilder args = new StringBuilder();
                for (AccesoMemoria a : l.getArgumentos()) { if (args.length() > 0) args.append(", "); args.append(describir(a)); }
                d = "Llamada " + describir(l.getDestino()) + " = " + l.getNombreFuncion() + "(" + args + ")";
            } else if (c instanceof Retorno1 r) d = "Retorno1 " + describir(r.getValor());
            else if (c instanceof ConversionTipo ct) d = "ConversionTipo " + describir(ct.getDestino()) + " = " + describir(ct.getFuente());
            else if (c instanceof Continuar1 k) d = "Continuar goto L" + k.getEtiqueta();
            else if (c instanceof Romper1 r) d = "Romper goto L" + r.getEtiqueta();
            else d = c.getClass().getSimpleName();
        } catch (RuntimeException e) { d = c.getClass().getSimpleName(); }
        return d.replace("\n", "\\n").replace("\r", "\\r");
    }

    private void emitirAsignacionAtributo(AsignacionAtributo a) {
        cargar(a.getValor(), IZQ);
        direccionCampo(a.getBase(), a.getCampo(), DIR);
        linea("sd   %s, 0(%s)", IZQ, DIR);
    }

    private void direccionCampo(AccesoMemoria base, String campo, String registro) {
        if (!(base instanceof AccesoVariable v)) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: base de atributo debe ser variable");
        }
        TablaSimbolosPig.SimboloVariable s = simbolo(v);

        int offsetCampo = calcularOffsetCampo(s.tipo(), campo);

        if (s.esEstructura()) {
            int offsetTotal = s.offset() + offsetCampo;
            if (cabe(offsetTotal)) {
                linea("addi %s, s0, %d   # &%s.%s", registro, offsetTotal, v.getNombre(), campo);
            } else {
                linea("li   %s, %d", registro, offsetTotal);
                linea("add  %s, s0, %s   # &%s.%s", registro, registro, v.getNombre(), campo);
            }
        } else if (s.esObjeto()) {
            if (cabe(s.offset())) {
                linea("ld   %s, %d(s0)   # %s", registro, s.offset(), v.getNombre());
            } else {
                linea("li   %s, %d", AUX, s.offset());
                linea("add  %s, s0, %s", AUX, AUX);
                linea("ld   %s, 0(%s)", registro, AUX);
            }
            if (offsetCampo != 0) {
                linea("addi %s, %s, %d   # &%s.%s", registro, registro, offsetCampo, v.getNombre(), campo);
            }
        } else {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: '" + v.getNombre() + "' no es estructura ni objeto");
        }
    }

    private int calcularOffsetCampo(String tipo, String campo) {
        // 1. Buscar en estructuras.
        var estOpt = tabla.buscarEstructura(tipo);
        if (estOpt.isPresent()) {
            int offset = 0;
            for (var entry : estOpt.get().atributos().entrySet()) {
                if (entry.getKey().equals(campo)) return offset;
                offset += 8;
            }
        }
        // 2. Buscar en clases (incluye atributos heredados, padre primero).
        if (tabla.buscarClase(tipo).isPresent()) {
            return ctx.getDisposicion().offsetCampo(tipo, campo);
        }
        throw new IllegalStateException("Campo no encontrado: " + campo + " en " + tipo);
    }

    private void linea(String formato, Object... args) {
        ctx.getText().append("    ").append(String.format(formato, args)).append("\n");
    }

    private void etiqueta(String nombre) {
        ctx.getText().append(nombre).append(":\n");
    }

    private void emitirNewObjeto(NewObjeto n) {
        int tam = tamanoDeClase(n.getNombreClase());
        linea("la   %s, heap_ptr", IZQ);
        linea("ld   %s, 0(%s)", DER, IZQ);
        linea("li   %s, %d", RES, tam);
        linea("add  %s, %s, %s", DIR, DER, RES);
        linea("sd   %s, 0(%s)", DIR, IZQ);
        linea("la   %s, heap", AUX);
        linea("add  a0, %s, %s", AUX, DER);
        if (ctx.getVirtuales().tieneTabla(n.getNombreClase())) {
            linea("la   %s, %s", IDX, ctx.getVirtuales().etiquetaTabla(n.getNombreClase()));
            linea("sd   %s, 0(a0)   # tabla de métodos del objeto", IDX);
        }

        List<AccesoMemoria> args = n.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(args.get(i), "a" + (i + 1));
        }
        linea("jal  ra, %s", n.getNombreConstructorC());
        if (n.getDestino() != null) {
            almacenar("a0", n.getDestino());
        }
    }

    private void emitirLlamadaMetodo(LlamadaMetodo1 l) {
        cargar(l.getReceptor(), "a0");
        List<AccesoMemoria> args = l.getArgumentos();
        for (int i = 0; i < args.size(); i++) {
            cargar(args.get(i), "a" + (i + 1));
        }
        String metodo = l.getNombreMetodo();
        if (ctx.getVirtuales().esVirtual(metodo)) {
            linea("ld   t5, 0(a0)   # tabla de métodos del objeto");
            linea("ld   t5, %d(t5)   # %s", ctx.getVirtuales().desplazamiento(metodo), metodo);
            linea("jalr ra, t5, 0");
        } else {
            linea("jal  ra, %s", metodo);
        }
        if (l.getDestino() != null) {
            almacenar("a0", l.getDestino());
        }
    }

    private int tamanoDeClase(String clase) {
        return ctx.getDisposicion().tamano(clase);
    }
}