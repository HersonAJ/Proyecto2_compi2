package com.example.stack_over_pig.riscv;

import com.example.stack_over_pig.piglatin.nodo.NodoExpr;
import com.example.stack_over_pig.piglatin.nodo.NodoSentencia;

public interface VisitanteGeneradorCuartetas {
    // Sentencias
    void visitar(NodoSentencia.DeclaracionVariable d);
    void visitar(NodoSentencia.Asignacion a);
    void visitar(NodoSentencia.Condicional c);
    void visitar(NodoSentencia.CicloDum c);
    void visitar(NodoSentencia.CicloFacere c);
    void visitar(NodoSentencia.CicloPer c);
    void visitar(NodoSentencia.Escritura e);
    void visitar(NodoSentencia.Lectura l);
    void visitar(NodoSentencia.IncrementoDecremento i);
    void visitar(NodoSentencia.InterrupcionCiclo i);
    void visitar(NodoSentencia.LlamadaFuncionSentencia l);

    // Expresiones
    void visitar(NodoExpr.LiteralEntero l);
    void visitar(NodoExpr.LiteralDecimal l);
    void visitar(NodoExpr.LiteralTexto l);
    void visitar(NodoExpr.LiteralCaracter l);
    void visitar(NodoExpr.LiteralBool l);
    void visitar(NodoExpr.Identificador i);
    void visitar(NodoExpr.Binaria b);
    void visitar(NodoExpr.Unaria u);
    void visitar(NodoExpr.LlamadaFuncion l);
    void visitar(NodoSentencia.DeclaracionArreglo d);
    void visitar(NodoExpr.AccesoArray a);
}
