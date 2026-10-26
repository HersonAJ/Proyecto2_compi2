package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionArreglo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.AsignacionAtributo;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Continuar1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasY.Romper1;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ.AccesoAtributo1;
import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.*;
import com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasPig.*;
import com.example.stack_over_pig.piglatin.semantica.TablaSimbolosPig;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Recorre las cuartetas y emite ensamblador RISC-V (RV64) siguiendo el esquema visto en clase:
 * <p>
 * 0(fp)    s0 anterior          (fp = s0)
 * -8(fp)    ra
 * -16(fp)    primera variable local, luego las demás y los temporales del C3D (8 bytes por slot)
 * <p>
 * - odo vive en la pila con offsets negativos respecto a fp. Cada cuarteta carga de la pila a
 * registros, opera y vuelve a guardar, así que ningún valor sobrevive entre cuartetas.
 * - Registros fijos (sin pool cíclico): t0 izquierda, t1 derecha, t2 resultado, t3/t4 direcciones
 * de arreglos, t6 solo para offsets fuera del rango de 12 bits.
 * - Memoria estática: el marco se calcula una sola vez; no hay malloc ni alloca.
 * - Entrada/salida sin librerías de C: ver {@link RutinasConsola} (ecall de Linux).
 * - Los offsets de variables los asigna el GeneradorCuartetasPig en la TablaSimbolosPig; los de los
 * temporales se piden aquí a la tabla cuando se usan por primera vez.
 */
public class EmisorRiscV {

    // ---------- registros fijos ----------
    private static final String IZQ = "t0";
    private static final String DER = "t1";
    private static final String RES = "t2";
    private static final String DIR = "t3";   // dirección calculada de un elemento de arreglo
    private static final String IDX = "t4";   // índice al guardar en un arreglo
    private static final String AUX = "t6";   // solo para offsets que no caben en 12 bits

    /**
     * false: ramas directas como en clase (beq/bne/blt/bge, alcance de +-4 KB).
     * true:  rama inversa + j, para programas muy grandes (alcance de +-1 MB).
     * Cambiar a true si el ensamblador se queja de "relocation truncated" en una rama.
     */
    private static final boolean RAMAS_LARGAS = false;

    /**
     * true: al empezar, imprime en la consola (System.out) la lista numerada de cuartetas recibidas, aunque luego falle.
     */
    private static final boolean IMPRIMIR_CUARTETAS_EN_CONSOLA = true;

    /**
     * true: antes de cada cuarteta se escribe un comentario con su descripción (útil para comparar con el C3D).
     */
    private static final boolean COMENTAR_CUARTETAS = true;

    private static final int BYTES_SLOT = 8;     // RV64: odo slot es de 8 bytes
    private static final int CAPACIDAD_LECTURA_TEXTO = 256;

    private final StringBuilder data = new StringBuilder();   // cadenas literales
    private final StringBuilder bss = new StringBuilder();    // buffers para leer texto
    private final StringBuilder text = new StringBuilder();   // cuerpo de main

    private final TablaSimbolosPig tabla;

    private final Map<String, String> cadenas = new LinkedHashMap<>();   // texto escapado -> etiqueta
    private final Set<String> rutinasUsadas = new LinkedHashSet<>();
    private int contadorBuffers = 0;
    private int contadorSaltos = 0;

    public EmisorRiscV(TablaSimbolosPig tabla) {
        this.tabla = tabla;
    }

    private record Ubicacion(int offset, String nombre) {
    }

    // =====================================================================
    //  PROGRAMA COMPLETO
    // =====================================================================

    public String emitir(List<Cuarteta> cuartetas) {
        if (IMPRIMIR_CUARTETAS_EN_CONSOLA) {
            System.out.println("===== CUARTETAS RECIBIDAS POR EmisorRiscV (" + cuartetas.size() + ") =====");
            int k = 0;
            for (Cuarteta c : cuartetas) {
                System.out.println("[" + (++k) + "] " + describir(c));
            }
            System.out.println("===== FIN DE CUARTETAS =====");
        }

        int numero = 0;
        for (Cuarteta c : cuartetas) {
            numero++;
            if (COMENTAR_CUARTETAS) {
                text.append("    # [").append(numero).append("] ").append(describir(c)).append("\n");
            }
            try {
                emitirCuarteta(c);
            } catch (RuntimeException e) {
                throw new IllegalStateException("EmisorRiscV: falló en la cuarteta #" + numero
                        + " -> " + describir(c) + " | " + e.getMessage(), e);
            }
        }

        // El marco se conoce hasta aquí: ya se pidieron los slots de todos los temporales.
        int framesize = ((tabla.getSiguienteOffset() + 15) / 16) * 16;

        StringBuilder out = new StringBuilder();
        out.append("# Generado por stack_over_pig - RISC-V RV64, ecall con el ABI de Linux (write=64, read=63, exit=93)\n");
        if (data.length() > 0) {
            out.append(".data\n").append(data).append("\n");
        }
        if (bss.length() > 0) {
            out.append(".bss\n").append(bss).append("\n");
        }

        out.append(".text\n");
        out.append(".globl _start\n");
        out.append("_start:\n");
        out.append("    jal  ra, main\n");
        out.append("    li   a0, 0\n");
        out.append("    li   a7, 93\n");
        out.append("    ecall\n\n");

        out.append("main:\n");
        out.append(prologo(framesize)).append("\n");
        out.append(text);
        out.append("__epilogo_main:\n");
        out.append(epilogo(framesize));

        if (!rutinasUsadas.isEmpty()) {
            out.append("\n# ---------- rutinas de entrada/salida ----------\n");
            out.append(RutinasConsola.generar(rutinasUsadas));
        }
        return out.toString();
    }

    private static String prologo(int framesize) {
        StringBuilder sb = new StringBuilder();
        if (framesize <= 2048) {
            sb.append("    addi sp, sp, -").append(framesize).append("\n");
            sb.append("    sd   s0, ").append(framesize - 8).append("(sp)\n");
            sb.append("    sd   ra, ").append(framesize - 16).append("(sp)\n");
            sb.append("    addi s0, sp, ").append(framesize - 8).append("\n");
        } else {
            // marco grande: los inmediatos de 12 bits no alcanzan
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

    // =====================================================================
    //  CUARTETAS
    // =====================================================================

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
        else if (c instanceof Retorno1 r) emitirRetorno(r);
        else if (c instanceof ConversionTipo ct) emitirConversion(ct);
        else if (c instanceof ImprimirPig i) emitirImprimir(i);
        else if (c instanceof LeerPig l) emitirLeer(l);
        else if (c instanceof Continuar1 cont) emitirSalto(cont.getEtiqueta());
        else if (c instanceof Romper1 romp) emitirSalto(romp.getEtiqueta());
        else throw new UnsupportedOperationException(
                    "EmisorRiscV: cuarteta no soportada: " + c.getClass().getSimpleName());
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
            case "<=" -> {                                    // a <= b  ==  !(b < a)
                linea("slt  %s, %s, %s", RES, DER, IZQ);
                linea("xori %s, %s, 1", RES, RES);
            }
            case ">=" -> {                                    // a >= b  ==  !(a < b)
                linea("slt  %s, %s, %s", RES, IZQ, DER);
                linea("xori %s, %s, 1", RES, RES);
            }
            case "==" -> {                                    // a == b  ==  (a - b) == 0
                linea("sub  %s, %s, %s", RES, IZQ, DER);
                linea("sltiu %s, %s, 1", RES, RES);
            }
            case "!=" -> {
                linea("sub  %s, %s, %s", RES, IZQ, DER);
                linea("sltu %s, zero, %s", RES, RES);
            }
            case "&&" -> {                                    // normaliza a 0/1 antes de combinar
                linea("sltu %s, zero, %s", IZQ, IZQ);
                linea("sltu %s, zero, %s", DER, DER);
                linea("and  %s, %s, %s", RES, IZQ, DER);
            }
            case "||" -> {
                linea("or   %s, %s, %s", RES, IZQ, DER);
                linea("sltu %s, zero, %s", RES, RES);
            }
            default -> throw new UnsupportedOperationException(
                    "EmisorRiscV: operador binario no soportado: " + o.getOperador());
        }
        almacenar(RES, o.getDestino());
    }

    private void emitirUnaria(OperacionUnaria o) {
        cargar(o.getOperando(), IZQ);
        switch (o.getOperador()) {
            case "-" -> linea("sub  %s, zero, %s", RES, IZQ);
            case "!" -> linea("sltiu %s, %s, 1", RES, IZQ);
            case "+" -> linea("mv   %s, %s", RES, IZQ);
            default -> throw new UnsupportedOperationException(
                    "EmisorRiscV: operador unario no soportado: " + o.getOperador());
        }
        almacenar(RES, o.getDestino());
    }

    private void emitirCondicional(Condicional1 c) {
        cargar(c.getIzquierda(), IZQ);
        cargar(c.getDerecha(), DER);
        String destino = "L" + c.getEtiqueta();

        // Solo se usan beq/bne/blt/bge (reales en cualquier ensamblador); > y <= invierten operandos.
        switch (c.getOperador()) {
            case "==" -> rama("beq", IZQ, DER, destino);
            case "!=" -> rama("bne", IZQ, DER, destino);
            case "<" -> rama("blt", IZQ, DER, destino);
            case ">=" -> rama("bge", IZQ, DER, destino);
            case ">" -> rama("blt", DER, IZQ, destino);
            case "<=" -> rama("bge", DER, IZQ, destino);
            default -> throw new UnsupportedOperationException(
                    "EmisorRiscV: comparador no soportado: " + c.getOperador());
        }
    }

    private void rama(String instruccion, String r1, String r2, String destino) {
        if (!RAMAS_LARGAS) {
            linea("%s  %s, %s, %s", instruccion, r1, r2, destino);
            return;
        }
        String inversa = switch (instruccion) {
            case "beq" -> "bne";
            case "bne" -> "beq";
            case "blt" -> "bge";
            default -> "blt";   // bge
        };
        String salto = "__salto_" + (contadorSaltos++);
        linea("%s  %s, %s, %s", inversa, r1, r2, salto);
        linea("j    %s", destino);
        etiqueta(salto);
    }

    private void emitirSalto(int etiqueta) {
        linea("j    L%d", etiqueta);
    }

    private void emitirLlamada(Llamada l) {
        List<AccesoMemoria> args = l.getArgumentos();
        if (args.size() > 8) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: más de 8 argumentos (los demás van a la pila) todavía no está implementado");
        }
        for (int i = 0; i < args.size(); i++) {
            cargar(args.get(i), "a" + i);
        }
        linea("jal  ra, %s", l.getNombreFuncion());
        if (l.getDestino() != null) {
            almacenar("a0", l.getDestino());
        }
    }

    private void emitirRetorno(Retorno1 r) {
        if (r.getValor() != null) {
            cargar(r.getValor(), "a0");
        }
        linea("j    __epilogo_main");
    }

    private void emitirConversion(ConversionTipo c) {
        // : conversiones con punto flotante (fcvt.*). Por ahora solo enteros: copia directa.
        cargar(c.getFuente(), IZQ);
        almacenar(IZQ, c.getDestino());
    }

    // =====================================================================
    //  ENTRADA / SALIDA
    // =====================================================================

    private void emitirImprimir(ImprimirPig i) {
        String rutina = switch (i.getTipo()) {
            case "textum" -> RutinasConsola.IMPRIMIR_CADENA;
            case "littera" -> RutinasConsola.IMPRIMIR_CARACTER;
            case "decimalis" -> throw new UnsupportedOperationException(
                    "EmisorRiscV: escribir decimalis todavía no está implementado (punto flotante)");
            default -> RutinasConsola.IMPRIMIR_ENTERO;      // numerus y bool
        };
        rutinasUsadas.add(rutina);
        cargar(i.getValor(), "a0");
        linea("jal  ra, %s", rutina);
    }

    private void emitirLeer(LeerPig l) {
        switch (l.getTipo()) {
            case "textum" -> {
                // Memoria estática: cada lectura de texto tiene su propio buffer en .bss;
                // la variable guarda la dirección de ese buffer.
                String buffer = "buf_lectura_" + (contadorBuffers++);
                bss.append("    ").append(buffer).append(": .space ").append(CAPACIDAD_LECTURA_TEXTO).append("\n");
                rutinasUsadas.add(RutinasConsola.LEER_CADENA);
                linea("la   a0, %s", buffer);
                linea("li   a1, %d", CAPACIDAD_LECTURA_TEXTO);
                linea("jal  ra, %s", RutinasConsola.LEER_CADENA);
                linea("la   %s, %s", IZQ, buffer);
                almacenar(IZQ, l.getDestino());
            }
            case "littera" -> {
                rutinasUsadas.add(RutinasConsola.LEER_CARACTER);
                linea("jal  ra, %s", RutinasConsola.LEER_CARACTER);
                almacenar("a0", l.getDestino());
            }
            case "decimalis" -> throw new UnsupportedOperationException(
                    "EmisorRiscV: leer decimalis todavía no está implementado (punto flotante)");
            default -> {                                     // numerus y bool
                rutinasUsadas.add(RutinasConsola.LEER_ENTERO);
                linea("jal  ra, %s", RutinasConsola.LEER_ENTERO);
                almacenar("a0", l.getDestino());
            }
        }
    }

    // =====================================================================
    //  CARGAR / GUARDAR
    // =====================================================================

    /**
     * Deja el valor de 'acc' en 'registro'. Solo toca 'registro' y los auxiliares DIR/AUX.
     */
    private void cargar(AccesoMemoria acc, String registro) {

        if (acc instanceof Literal l) {
            cargarLiteral(l, registro);
        }
     else if (acc instanceof AccesoAtributo1 attr) {
        direccionCampo(attr.getBase(), attr.getCampo(), DIR);
        linea("ld   %s, 0(%s)", registro, DIR);
    }

        else if (acc instanceof AccesoArreglo arr) {
            // elemento = *(base + indice * 8). El índice se evalúa primero, en el mismo registro destino.
            cargar(arr.getIndice(), registro);
            direccionBase(arr.getBase(), DIR);
            linea("slli %s, %s, 3", registro, registro);
            linea("add  %s, %s, %s", DIR, DIR, registro);
            linea("ld   %s, 0(%s)", registro, DIR);
        } else {
            Ubicacion u = ubicar(acc);
            accesoPila("ld", registro, u);
        }
    }

    private void almacenar(String registro, AccesoMemoria destino) {
        if (destino instanceof AccesoArreglo arr) {
            almacenarEnArreglo(registro, arr.getBase(), arr.getIndice());
        } else {
            accesoPila("sd", registro, ubicar(destino));
        }
    }

    private void almacenarEnArreglo(String registroValor, AccesoMemoria base, AccesoMemoria indice) {
        cargar(indice, IDX);
        direccionBase(base, DIR);
        linea("slli %s, %s, 3", IDX, IDX);
        linea("add  %s, %s, %s", DIR, DIR, IDX);
        linea("sd   %s, 0(%s)", registroValor, DIR);
    }

    /**
     * Deja en 'registro' la dirección del elemento 0 del arreglo.
     * Un arreglo local ocupa slots consecutivos hacia arriba desde su offset (fp + offset).
     */
    private void direccionBase(AccesoMemoria base, String registro) {
        if (!(base instanceof AccesoVariable v)) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: la base de un arreglo debe ser una variable (arreglos multidimensionales pendientes)");
        }
        TablaSimbolosPig.SimboloVariable s = simbolo(v);
        if (s.dimensiones() == 0) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: '" + v.getNombre() + "' no es un arreglo");
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
            case "decimalis" -> throw new UnsupportedOperationException(
                    "EmisorRiscV: literales decimalis todavía no están implementados (punto flotante)");
            default -> throw new IllegalArgumentException(
                    "EmisorRiscV: tipo de literal desconocido: " + l.getTipo());
        }
    }

    // ---------- ubicación en la pila ----------

    private Ubicacion ubicar(AccesoMemoria acc) {
        if (acc instanceof AccesoVariable v) {
            return new Ubicacion(simbolo(v).offset(), v.getNombre());
        }
        if (acc instanceof AccesoTemporal t) {
            // asignarOffsetTemporal es idempotente: crea el slot la primera vez y lo reutiliza después
            return new Ubicacion(tabla.asignarOffsetTemporal(t.getNumero()), "t" + t.getNumero());
        }
        throw new IllegalStateException("EmisorRiscV: no se puede ubicar en la pila: " + describir(acc));
    }

    private TablaSimbolosPig.SimboloVariable simbolo(AccesoVariable v) {
        TablaSimbolosPig.SimboloVariable s = tabla.buscarVariable(v.getNombre())
                .orElseThrow(() -> new IllegalStateException(
                        "EmisorRiscV: la variable '" + v.getNombre() + "' no está en la tabla de símbolos"));
        // Los locales empiezan en -16(fp); 0 o positivo significa que nadie le asignó posición.
        if (s.offset() > -16) {
            throw new IllegalStateException(
                    "EmisorRiscV: la variable '" + v.getNombre() + "' tiene offset " + s.offset()
                            + " (no se le asignó posición en la pila; revisar la declaración en GeneradorCuartetasPig)");
        }
        return s;
    }

    private static boolean cabe(int offset) {
        return offset >= -2048 && offset <= 2047;
    }

    /**
     * ld / sd contra la pila; si el offset no cabe en 12 bits calcula la dirección en AUX.
     */
    private void accesoPila(String instruccion, String registro, Ubicacion u) {
        if (cabe(u.offset())) {
            linea("%s   %s, %d(s0)   # %s", instruccion, registro, u.offset(), u.nombre());
        } else {
            linea("li   %s, %d", AUX, u.offset());
            linea("add  %s, s0, %s", AUX, AUX);
            linea("%s   %s, 0(%s)   # %s", instruccion, registro, AUX, u.nombre());
        }
    }

    // ---------- literales ----------

    private String etiquetaCadena(String valor) {
        String escapada = escaparCadena(valor);
        String existente = cadenas.get(escapada);
        if (existente != null) return existente;
        String etiqueta = "str_" + cadenas.size();
        cadenas.put(escapada, etiqueta);
        data.append("    ").append(etiqueta).append(": .string \"").append(escapada).append("\"\n");
        return etiqueta;
    }

    /**
     * Prepara un texto para ponerlo entre comillas en .string.
     * Las secuencias con barra (\n, \t, \") se respetan tal cual; los saltos de línea reales,
     * las comillas sueltas y los caracteres no ASCII (se escriben como bytes UTF-8) se escapan.
     */
    private static String escaparCadena(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '\\') {
                if (i < s.length()) {
                    sb.append('\\').append(s.charAt(i));
                    i++;
                } else {
                    sb.append("\\\\");
                }
            } else if (cp == '"') {
                sb.append("\\\"");
            } else if (cp == '\n') {
                sb.append("\\n");
            } else if (cp == '\t') {
                sb.append("\\t");
            } else if (cp == '\r') {
                sb.append("\\r");
            } else if (cp >= 32 && cp < 127) {
                sb.append((char) cp);
            } else {
                for (byte b : new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8)) {
                    sb.append(String.format("\\%03o", b & 0xFF));
                }
            }
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
                case 'n' -> 10;
                case 't' -> 9;
                case 'r' -> 13;
                case '0' -> 0;
                default -> s.charAt(1);          // \\, \', \"
            };
        }
        return Integer.parseInt(s);
    }

    // =====================================================================
    //  DESCRIPCIONES (comentarios y mensajes de error)
    // =====================================================================

    private static String describir(AccesoMemoria acc) {
        try {
            if (acc == null) return "null";
            if (acc instanceof AccesoVariable v) return "var:" + v.getNombre();
            if (acc instanceof AccesoTemporal t) return "t" + t.getNumero();
            if (acc instanceof Literal l) return "lit:" + l.getValor() + ":" + l.getTipo();
            if (acc instanceof AccesoArreglo a)
                return "arr:" + describir(a.getBase()) + "[" + describir(a.getIndice()) + "]";
            return acc.getClass().getSimpleName();
        } catch (RuntimeException e) {
            return acc.getClass().getSimpleName();
        }
    }

    private static String describir(Cuarteta c) {
        String d;
        try {
            if (c instanceof AsignacionArreglo a) {
                d = "AsignacionArreglo base=" + describir(a.getBase()) + " indice=" + describir(a.getIndice())
                        + " valor=" + describir(a.getValor());
            } else if (c instanceof AsignacionVariable a) {
                d = "AsignacionVariable " + describir(a.getDestino()) + " = " + describir(a.getValor());
            } else if (c instanceof OperacionBinaria o) {
                d = "OperacionBinaria " + describir(o.getDestino()) + " = " + describir(o.getIzquierda())
                        + " " + o.getOperador() + " " + describir(o.getDerecha());
            } else if (c instanceof OperacionUnaria o) {
                d = "OperacionUnaria " + describir(o.getDestino()) + " = " + o.getOperador() + " " + describir(o.getOperando());
            } else if (c instanceof Condicional1 k) {
                d = "Condicional1 if " + describir(k.getIzquierda()) + " " + k.getOperador() + " "
                        + describir(k.getDerecha()) + " goto L" + k.getEtiqueta();
            } else if (c instanceof Salto s) {
                d = "Salto goto L" + s.getEtiqueta();
            } else if (c instanceof DefinicionEtiqueta e) {
                d = "Etiqueta L" + e.getEtiqueta() + ":";
            } else if (c instanceof ImprimirPig i) {
                d = "ImprimirPig " + i.getTipo() + " " + describir(i.getValor());
            } else if (c instanceof LeerPig l) {
                d = "LeerPig " + l.getTipo() + " -> " + describir(l.getDestino());
            } else if (c instanceof Llamada l) {
                StringBuilder args = new StringBuilder();
                for (AccesoMemoria a : l.getArgumentos()) {
                    if (args.length() > 0) args.append(", ");
                    args.append(describir(a));
                }
                d = "Llamada " + describir(l.getDestino()) + " = " + l.getNombreFuncion() + "(" + args + ")";
            } else if (c instanceof Retorno1 r) {
                d = "Retorno1 " + describir(r.getValor());
            } else if (c instanceof ConversionTipo ct) {
                d = "ConversionTipo " + describir(ct.getDestino()) + " = " + describir(ct.getFuente());
            } else if (c instanceof Continuar1 k) {
                d = "Continuar goto L" + k.getEtiqueta();
            } else if (c instanceof Romper1 r) {
                d = "Romper goto L" + r.getEtiqueta();
            } else {
                d = c.getClass().getSimpleName();
            }
        } catch (RuntimeException e) {
            d = c.getClass().getSimpleName();
        }
        return d.replace("\n", "\\n").replace("\r", "\\r");
    }

    private void emitirAsignacionAtributo(AsignacionAtributo a) {
        cargar(a.getValor(), IZQ);
        direccionCampo(a.getBase(), a.getCampo(), DIR);
        linea("sd   %s, 0(%s)", IZQ, DIR);
    }

    private void direccionCampo(AccesoMemoria base, String campo, String registro) {
        // 1. Obtener la dirección base del struct
        if (!(base instanceof AccesoVariable v)) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: la base de un atributo debe ser una variable");
        }
        TablaSimbolosPig.SimboloVariable s = simbolo(v);
        if (!s.esEstructura()) {
            throw new UnsupportedOperationException(
                    "EmisorRiscV: '" + v.getNombre() + "' no es una estructura");
        }

        // 2. Calcular offset del campo
        int offsetCampo = calcularOffsetCampo(s.tipo(), campo);

        // 3. Dirección = s0 + s.offset() + offsetCampo
        int offsetTotal = s.offset() + offsetCampo;
        if (cabe(offsetTotal)) {
            linea("addi %s, s0, %d   # &%s.%s", registro, offsetTotal, v.getNombre(), campo);
        } else {
            linea("li   %s, %d", registro, offsetTotal);
            linea("add  %s, s0, %s   # &%s.%s", registro, registro, v.getNombre(), campo);
        }
    }

    private int calcularOffsetCampo(String tipoStruct, String campo) {
        var estOpt = tabla.buscarEstructura(tipoStruct);
        if (estOpt.isEmpty()) {
            throw new IllegalStateException("Estructura no encontrada: " + tipoStruct);
        }
        int offset = 0;
        for (var entry : estOpt.get().atributos().entrySet()) {
            if (entry.getKey().equals(campo)) return offset;
            offset -= 8;  // RV64: cada campo ocupa 8 bytes
        }
        throw new IllegalStateException("Campo no encontrado: " + campo + " en " + tipoStruct);
    }

    // =====================================================================
    //  SALIDA DE TEXTO
    // =====================================================================

    private void linea(String formato, Object... args) {
        text.append("    ").append(String.format(formato, args)).append("\n");
    }

    private void etiqueta(String nombre) {
        text.append(nombre).append(":\n");
    }
}