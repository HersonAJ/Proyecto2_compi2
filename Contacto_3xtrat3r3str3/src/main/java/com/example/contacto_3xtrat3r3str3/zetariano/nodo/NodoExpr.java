package com.example.contacto_3xtrat3r3str3.zetariano.nodo;

import com.example.contacto_3xtrat3r3str3.c3d_v2.c.z.*;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.cuartetasZ.*;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.*;
import com.example.contacto_3xtrat3r3str3.zetariano.semantica.TablaSimbolosZ;
import java.util.ArrayList;
import java.util.List;

public sealed interface NodoExpr extends NodoAST permits
        NodoExpr.LiteralEntero,
        NodoExpr.LiteralDecimal,
        NodoExpr.LiteralCadena,
        NodoExpr.LiteralCaracter,
        NodoExpr.LiteralBool,
        NodoExpr.LiteralNulo,
        NodoExpr.ListaLiteral,
        NodoExpr.Identificador,
        NodoExpr.AccesoArray,
        NodoExpr.AccesoAtributo,
        NodoExpr.Binaria,
        NodoExpr.Unaria,
        NodoExpr.IncrementoDecremento,
        NodoExpr.Ternaria,
        NodoExpr.LlamadaFuncion,
        NodoExpr.LlamadaMetodo,
        NodoExpr.InstanciaObjeto,
        NodoExpr.ArregloNuevo {

    TipoNodoExpr tipoNodo();

    AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx);

    // LITERALES
    /** '10'  ->  operando literal entero. */
    record LiteralEntero(int linea, int columna, int valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_ENTERO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            return new LiteralZ(valor, "int");
        }
    }

    /** '3.14'  ->  operando literal double. */
    record LiteralDecimal(int linea, int columna, double valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_DECIMAL; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            return new LiteralZ(valor, "double");
        }
    }

    /** '"hola"'  ->  operando literal String. */
    record LiteralCadena(int linea, int columna, String valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CADENA; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            return new LiteralZ(valor, "String");
        }
    }

    /** 'a'  ->  operando literal char. */
    record LiteralCaracter(int linea, int columna, char valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_CARACTER; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            return new LiteralZ(valor, "char");
        }
    }

    /** 'true' / 'false'  ->  operando literal boolean. */
    record LiteralBool(int linea, int columna, boolean valor) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_BOOL; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            return new LiteralZ(valor, "boolean");
        }
    }

    /** 'null'  ->  operando LiteralNuloZ. */
    record LiteralNulo(int linea, int columna) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LITERAL_NULO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            return new LiteralNuloZ();
        }
    }

    /** '{10, 20, 30}'  ->  solo válido como inicializador de declaración. */
    record ListaLiteral(int linea, int columna, List<NodoExpr> elementos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LISTA_LITERAL; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            throw new IllegalStateException(
                    "ListaLiteral solo puede usarse como inicializador de una declaración "
                            + "(línea " + linea + ")");
        }
    }

    // ACCESOS
    /** 'x'  ->  acceso a atributo  */
    record Identificador(int linea, int columna, String nombre) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.IDENTIFICADOR; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            var atributoOpt = ctx.getTabla().buscarAtributo(nombre);
            if (atributoOpt.isPresent()) {
                var attr = atributoOpt.get();
                AccesoVariable thisAcc = new AccesoVariable("this", "struct " + ctx.getNombreClase() + "*");
                return new AccesoAtributo1(thisAcc, nombre, true, attr.tipo());
            }

            // Variable local o parámetro
            var varOpt = ctx.getTabla().buscarVariable(nombre);
            if (varOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Variable no declarada: '" + nombre + "' (línea " + linea + ")");
            }
            var simbolo = varOpt.get();
            String tipoC = TipoCZ.baseAC(simbolo.tipo(), !TipoCZ.esPrimitivo(simbolo.tipo()));
            return new AccesoVariable(nombre, tipoC);
        }
    }

    /** 'a[i]'  ->  operando AccesoArreglo (con su tipo de elemento inferido). */
    record AccesoArray(int linea, int columna, NodoExpr arreglo, NodoExpr indice) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ARRAY; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            AccesoMemoria base = arreglo.aCodigoIntermedio(ctx);
            AccesoMemoria indiceAcc = indice.aCodigoIntermedio(ctx);
            String tipoBase = base.getTipo();
            String tipoElemento;
            if (tipoBase.endsWith("*")) {
                tipoElemento = tipoBase.substring(0, tipoBase.length() - 1);
            } else {
                tipoElemento = "int";
            }
            return new AccesoArreglo(base, indiceAcc, tipoElemento);
        }
    }

    /** 'p1.edad'  ->  operando AccesoAtributo1 (siempre con '->' en .z). */
    record AccesoAtributo(int linea, int columna, NodoExpr objeto, String atributo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ACCESO_ATRIBUTO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            AccesoMemoria base = objeto.aCodigoIntermedio(ctx);
            String tipoCampo = obtenerTipoCampo(ctx, atributo);
            return new AccesoAtributo1(base, atributo, true, tipoCampo);
        }

        /** Busca el tipo del campo en la clase actual. */
        private String obtenerTipoCampo(ContextoTraduccionZ ctx, String campo) {
            var attr = ctx.getTabla().buscarAtributo(campo);
            if (attr.isPresent()) return attr.get().tipo();
            return "int";
        }
    }

    // OPERACIONES
    /** 'a + b', 'a == b', ...  ->  promociones / strcmp / concat + OperacionBinaria. */
    record Binaria(int linea, int columna, String operador, NodoExpr izquierda, NodoExpr derecha) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.BINARIA; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            AccesoMemoria izq = izquierda.aCodigoIntermedio(ctx);
            AccesoMemoria der = derecha.aCodigoIntermedio(ctx);

            boolean hayString = "String".equals(izq.getTipo()) || "String".equals(der.getTipo());
            // Caso especial: '+' con String -> concatenación
            if ("+".equals(operador) && hayString) {
                int idT = g.getContador().siguienteTemporal("String");
                AccesoTemporal t = new AccesoTemporal(idT, "String");
                g.emitir(new OperacionBinaria(t, izq, "concat", der));
                return t;
            }

            // Caso especial: == y != con String -> strcmp
            boolean esComparacion = "==".equals(operador) || "!=".equals(operador);
            if (hayString && esComparacion) {
                int idCmp = g.getContador().siguienteTemporal("int");
                AccesoTemporal tCmp = new AccesoTemporal(idCmp, "int");
                g.emitir(new OperacionBinaria(tCmp, izq, "strcmp", der));
                int idRes = g.getContador().siguienteTemporal("int");
                AccesoTemporal tRes = new AccesoTemporal(idRes, "int");
                g.emitir(new OperacionBinaria(tRes, tCmp, operador, new LiteralZ(0, "int")));
                return tRes;
            }
            PromocionTiposZ.Resultado prom = PromocionTiposZ.promover(izq.getTipo(), der.getTipo());

            if (prom.conversionIzq() != null) {
                int idConv = g.getContador().siguienteTemporal(prom.conversionIzq());
                AccesoTemporal tConv = new AccesoTemporal(idConv, prom.conversionIzq());
                g.emitir(new ConversionTipo(tConv, TipoCZ.baseValorAC(prom.conversionIzq()), izq));
                izq = tConv;
            }
            if (prom.conversionDer() != null) {
                int idConv = g.getContador().siguienteTemporal(prom.conversionDer());
                AccesoTemporal tConv = new AccesoTemporal(idConv, prom.conversionDer());
                g.emitir(new ConversionTipo(tConv, TipoCZ.baseValorAC(prom.conversionDer()), der));
                der = tConv;
            }

            int idT = g.getContador().siguienteTemporal(prom.tipoResultado());
            AccesoTemporal t = new AccesoTemporal(idT, prom.tipoResultado());
            g.emitir(new OperacionBinaria(t, izq, operador, der));
            return t;
        }
    }

    /** '-x', '!x'  ->  OperacionUnaria con temporal. */
    record Unaria(int linea, int columna, String operador, NodoExpr operando) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.UNARIA; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            AccesoMemoria op = operando.aCodigoIntermedio(ctx);
            int idT = g.getContador().siguienteTemporal(op.getTipo());
            AccesoTemporal t = new AccesoTemporal(idT, op.getTipo());
            g.emitir(new OperacionUnaria(t, operador, op));
            return t;
        }
    }

    /** '++x', '--x', 'x++', 'x--'  ->  x = x ± 1 (como OperacionBinaria). */
    record IncrementoDecremento(int linea, int columna, String operador,
                                NodoExpr operando, boolean prefijo) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INCREMENTO_DECREMENTO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            AccesoMemoria op = operando.aCodigoIntermedio(ctx);
            String opBinario = "++".equals(operador) ? "+" : "-";
            AccesoMemoria uno = new LiteralZ(1, "int");
            g.emitir(new OperacionBinaria(op, op, opBinario, uno));
            return op;
        }
    }

    /** 'cond ? a : b'  ->  if/else con temporal que recibe a o b. */
    record Ternaria(int linea, int columna, NodoExpr condicion,
                    NodoExpr siVerdadero, NodoExpr siFalso) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.TERNARIA; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            var c = g.getContador();

            // 1. Crear el temporal con tipo provisional.
            int idT = c.siguienteTemporal("int");
            AccesoTemporal t = new AccesoTemporal(idT, "int");

            // 2. Evaluar condición.
            AccesoMemoria condAcc = condicion.aCodigoIntermedio(ctx);

            // 3. Etiquetas.
            int lSi = c.siguienteEtiqueta();
            int lNo = c.siguienteEtiqueta();
            int lFin = c.siguienteEtiqueta();

            g.emitir(new Condicional1(condAcc, "!=", new LiteralZ(0, "int"), lSi));
            g.emitir(new Salto(lNo));

            // 4. Bloque "si"
            g.emitir(new DefinicionEtiqueta(lSi));
            AccesoMemoria valorSi = siVerdadero.aCodigoIntermedio(ctx);
            g.emitir(new AsignacionVariable(t, valorSi));
            g.emitir(new Salto(lFin));

            // 5. Bloque "no"
            g.emitir(new DefinicionEtiqueta(lNo));
            AccesoMemoria valorNo = siFalso.aCodigoIntermedio(ctx);
            g.emitir(new AsignacionVariable(t, valorNo));

            // 6. Fin
            g.emitir(new DefinicionEtiqueta(lFin));
            String tipoComun = PromocionTiposZ.promover(valorSi.getTipo(), valorNo.getTipo())
                    .tipoResultado();
            c.registrarTipoTemporal(idT, tipoComun);

            return new AccesoTemporal(idT, tipoComun);
        }
    }

    // LLAMADAS Y CREACION DE OBJETOS
    /** 'calcular(x)'  ->  Llamada con 'this' como primer argumento. */
    record LlamadaFuncion(int linea, int columna, String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_FUNCION; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();

            // 1. Evaluar argumentos.
            List<AccesoMemoria> args = new ArrayList<>();
            for (NodoExpr arg : argumentos) {
                args.add(arg.aCodigoIntermedio(ctx));
            }

            // 2. Resolver la firma del metodo (por cantidad y tipos).
            List<String> tiposArgs = new ArrayList<>();
            for (AccesoMemoria a : args) tiposArgs.add(a.getTipo());
            var firmaOpt = ctx.getTabla().buscarMetodo(nombre, tiposArgs);
            if (firmaOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Método no encontrado: '" + nombre + "' con argumentos " + tiposArgs
                                + " (línea " + linea + ")");
            }
            var firma = firmaOpt.get();

            // 3. Construir nombre único en C.
            String nombreC = construirNombreC(ctx.getNombreClase(), nombre, firma);

            // 4. Determinar si el metodo retorna algo.
            String tipoRetorno = firma.tipoRetorno(); // null si void

            // 5. Emitir la llamada.
            if (tipoRetorno == null) {
                AccesoVariable thisAcc = new AccesoVariable("this",
                        "struct " + ctx.getNombreClase() + "*");
                g.emitir(new Llamada(null, nombreC, juntarThisConArgs(thisAcc, args)));
                return null;
            } else {
                int idT = g.getContador().siguienteTemporal(tipoRetorno);
                AccesoTemporal t = new AccesoTemporal(idT, tipoRetorno);

                AccesoVariable thisAcc = new AccesoVariable("this",
                        "struct " + ctx.getNombreClase() + "*");
                g.emitir(new Llamada(t, nombreC, juntarThisConArgs(thisAcc, args)));
                return t;
            }
        }

        private List<AccesoMemoria> juntarThisConArgs(AccesoMemoria thisAcc, List<AccesoMemoria> args) {
            List<AccesoMemoria> todos = new ArrayList<>();
            todos.add(thisAcc);
            todos.addAll(args);
            return todos;
        }

        /** Nombre C: Clase_metodo_tipo1_tipo2. */
        private String construirNombreC(String clase, String metodo, TablaSimbolosZ.Firma firma) {
            StringBuilder sb = new StringBuilder();
            sb.append(clase).append('_').append(metodo);
            for (TablaSimbolosZ.Parametro p : firma.parametros()) {
                sb.append('_').append(p.tipo());
            }
            return sb.toString();
        }
    }

    /** 'p1.saludar()'  ->  LlamadaMetodo1 con el receptor como primer argumento. */
    record LlamadaMetodo(int linea, int columna, NodoExpr objeto,
                         String nombre, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.LLAMADA_METODO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();

            // 1. Evaluar receptor y argumentos.
            AccesoMemoria receptorAcc = objeto.aCodigoIntermedio(ctx);
            List<AccesoMemoria> args = new ArrayList<>();
            for (NodoExpr arg : argumentos) {
                args.add(arg.aCodigoIntermedio(ctx));
            }

            // 2. Nombre de la clase del receptor.
            String claseReceptor = obtenerNombreClase(receptorAcc);

            // 3. Resolver firma del metodo.
            List<String> tiposArgs = new ArrayList<>();
            for (AccesoMemoria a : args) tiposArgs.add(a.getTipo());

            var firmaOpt = ctx.getTabla().buscarMetodo(nombre, tiposArgs);
            if (firmaOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Método no encontrado: '" + nombre + "' en clase '" + claseReceptor
                                + "' con argumentos " + tiposArgs + " (línea " + linea + ")");
            }
            var firma = firmaOpt.get();

            // 4. Nombre C.
            String nombreC = construirNombreC(claseReceptor, nombre, firma);

            // 5. Emitir.
            String tipoRetorno = firma.tipoRetorno();
            if (tipoRetorno == null) {
                g.emitir(new LlamadaMetodo1(null, receptorAcc, nombreC, args));
                return null;
            } else {
                int idT = g.getContador().siguienteTemporal(tipoRetorno);
                AccesoTemporal t = new AccesoTemporal(idT, tipoRetorno);
                g.emitir(new LlamadaMetodo1(t, receptorAcc, nombreC, args));
                return t;
            }
        }

        /** Extrae 'Nombre' de 'struct Nombre*' o 'struct Nombre'. */
        private String obtenerNombreClase(AccesoMemoria acceso) {
            String tipo = acceso.getTipo();
            if (tipo.startsWith("struct ") && tipo.endsWith("*")) {
                return tipo.substring("struct ".length(), tipo.length() - 1).trim();
            }
            if (tipo.startsWith("struct ")) {
                return tipo.substring("struct ".length()).trim();
            }
            return tipo;
        }

        /** Nombre C: Clase_metodo_tipo1_tipo2. */
        private String construirNombreC(String clase, String metodo, TablaSimbolosZ.Firma firma) {
            StringBuilder sb = new StringBuilder();
            sb.append(clase).append('_').append(metodo);
            for (TablaSimbolosZ.Parametro p : firma.parametros()) {
                sb.append('_').append(p.tipo());
            }
            return sb.toString();
        }
    }

    /** 'new Persona(...)'  ->  NewObjeto (malloc + constructor). */
    record InstanciaObjeto(int linea, int columna, String tipoClase, List<NodoExpr> argumentos) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.INSTANCIA_OBJETO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            // 1. Evaluar argumentos.
            List<AccesoMemoria> args = new ArrayList<>();
            for (NodoExpr arg : argumentos) {
                args.add(arg.aCodigoIntermedio(ctx));
            }
            // 2. Resolver firma del constructor.
            List<String> tiposArgs = new ArrayList<>();
            for (AccesoMemoria a : args) tiposArgs.add(a.getTipo());
            var firmaOpt = ctx.getTabla().buscarConstructor(tipoClase, tiposArgs);
            if (firmaOpt.isEmpty()) {
                throw new IllegalStateException(
                        "Constructor no encontrado: '" + tipoClase + "' con argumentos "
                                + tiposArgs + " (línea " + linea + ")");
            }
            var firma = firmaOpt.get();
            // 3. Nombre C del constructor.
            String nombreConstructorC = construirNombreConstructorC(tipoClase, firma);
            // 4. Temporal del tipo Clase.
            String tipoC = "struct " + tipoClase + "*";
            int idT = g.getContador().siguienteTemporal(tipoC);
            AccesoTemporal t = new AccesoTemporal(idT, tipoC);
            // 5. Emitir.
            g.emitir(new NewObjeto(t, tipoClase, nombreConstructorC, args));
            return t;
        }

        /** Nombre C: Clase_constructor_tipo1_tipo2. */
        private String construirNombreConstructorC(String clase, TablaSimbolosZ.Firma firma) {
            StringBuilder sb = new StringBuilder();
            sb.append(clase).append("_constructor");
            for (TablaSimbolosZ.Parametro p : firma.parametros()) {
                sb.append('_').append(p.tipo());
            }
            return sb.toString();
        }
    }

    /** 'new int[N]'  ->  NewArreglo (malloc).  'new int[A][B]'  ->  NewArregloMulti. */
    record ArregloNuevo(int linea, int columna, String tipoBase, List<NodoExpr> dimensiones) implements NodoExpr {
        @Override public TipoNodoExpr tipoNodo() { return TipoNodoExpr.ARREGLO_NUEVO; }

        @Override
        public AccesoMemoria aCodigoIntermedio(ContextoTraduccionZ ctx) {
            GestorCodigoIntermedio g = ctx.getGestor();
            // Contar dimensiones con tamaño explícito.
            List<AccesoMemoria> tamanos = new ArrayList<>();
            for (NodoExpr dim : dimensiones) {
                if (dim == null) {
                    throw new UnsupportedOperationException(
                            "Dimensiones vacías ('new int[3][]') no soportadas (línea " + linea + ")");
                }
                tamanos.add(dim.aCodigoIntermedio(ctx));
            }

            if (tamanos.isEmpty() || tamanos.size() > 2) {
                throw new UnsupportedOperationException(
                        "Solo arreglos 1D o 2D por ahora (línea " + linea + ")");
            }

            String tipoElementoC;
            if (TipoCZ.esPrimitivo(tipoBase)) {
                tipoElementoC = TipoCZ.baseValorAC(tipoBase);
            } else {
                tipoElementoC = "struct " + tipoBase;
            }
            String tipoTemporal = tipoElementoC + "*".repeat(tamanos.size());
            int idT = g.getContador().siguienteTemporal(tipoTemporal);
            AccesoTemporal t = new AccesoTemporal(idT, tipoTemporal);
            if (tamanos.size() == 1) {
                g.emitir(new NewArreglo(t, tipoElementoC, tamanos.get(0)));
            } else {
                g.emitir(new NewArregloMulti(t, tipoElementoC, tamanos));
            }
            return t;
        }
    }
}