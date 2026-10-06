#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// ============================================================
// STACK Y HEAP
// ============================================================

typedef union {
    int i;
    double d;
    char c;
    char* s;
    void* p;
} Valor;

#define STACK_SIZE 4096
#define HEAP_SIZE 4096

Valor stack[STACK_SIZE];
int BP = 0;
int SP = 0;

void* heap[HEAP_SIZE];
int HP = 0;

int alloc_heap(int size) {
    int dir = HP;
    HP += size;
    return dir;
}

char* concat(char* a, char* b) {
    char* r = malloc(strlen(a) + strlen(b) + 1);
    strcpy(r, a);
    strcat(r, b);
    return r;
}

// Stack de retornos
void* pilaRetorno[100];
int ptrRetorno = 0;

// Valor de retorno
int valorRetorno = 0;

typedef struct Direccion {
    char* calle;
    int numero;
} Direccion;
typedef struct Persona {
    char* nombre;
    int edad;
    Direccion domicilio;
} Persona;
int main() {
    goto L_Pig_main;

    // ===== FUNCIONES Y METODOS =====

    // ===== Funcion esMayorEdad =====
    L_Y_esMayorEdad:;
    Valor BP;
    BP.i = SP;
    Valor t1;
    t1.i = BP + 1;
    stack[t1].i = 0;
    Valor t2;
    t2.i = BP + 0;
    Valor t3;
    t3.i = stack[t2].i;
    if (t3 >= 18) goto Y_L1;
    goto Y_L2;
    Y_L1:;
    Valor t4;
    t4.i = BP + 1;
    stack[t4].i = 1;
    goto Y_L3;
    Y_L2:;
    Valor t5;
    t5.i = BP + 1;
    stack[t5].i = 0;
    Y_L3:;
    Valor t6;
    t6.i = BP + 1;
    Valor t7;
    t7.i = stack[t6].i;
    valorRetorno = t7.i;
    goto L_Y_esMayorEdad_retorno_final;
    L_Y_esMayorEdad_retorno:;
    Valor SP;
    SP.i = BP;
    L_Y_esMayorEdad_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Funcion sumar =====
    L_Y_sumar:;
    BP.i = SP;
    Valor t8;
    t8.i = BP + 2;
    Valor t9;
    t9.i = BP + 0;
    Valor t10;
    t10.i = stack[t9].i;
    Valor t11;
    t11.i = BP + 1;
    Valor t12;
    t12.i = stack[t11].i;
    Valor t13;
    t13.i = t10.i + t12.i;
    stack[t8].i = t13.i;
    Valor t14;
    t14.i = BP + 2;
    Valor t15;
    t15.i = stack[t14].i;
    valorRetorno = t15.i;
    goto L_Y_sumar_retorno_final;
    L_Y_sumar_retorno:;
    SP.i = BP;
    L_Y_sumar_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Constructor Persona =====
    L_Z_Persona_constructor:;
    BP.i = SP;
    t1.i = BP + 0;
    t2.i = stack[t1].i;
    t3.i = BP + 3;
    stack[t3].i = t2.i;
    t4.i = BP + 1;
    t5.i = stack[t4].i;
    t6.i = BP + 4;
    stack[t6].i = t5.i;
    t7.i = BP + 2;
    t8.i = stack[t7].i;
    t9.i = BP + 5;
    stack[t9].i = t8.i;
    goto L_Z_Persona_constructor_retorno;
    L_Z_Persona_constructor_retorno:;
    SP.i = BP;
    L_Z_Persona_constructor_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Metodo getNombre =====
    L_Z_Persona_getNombre:;
    BP.i = SP;
    t10.i = BP + 0;
    t11.i = stack[t10].i;
    valorRetorno = t11.i;
    goto L_Z_Persona_getNombre_retorno_final;
    L_Z_Persona_getNombre_retorno:;
    SP.i = BP;
    L_Z_Persona_getNombre_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Metodo getEdad =====
    L_Z_Persona_getEdad:;
    BP.i = SP;
    t12.i = BP + 1;
    t13.i = stack[t12].i;
    valorRetorno = t13.i;
    goto L_Z_Persona_getEdad_retorno_final;
    L_Z_Persona_getEdad_retorno:;
    SP.i = BP;
    L_Z_Persona_getEdad_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Metodo getAltura =====
    L_Z_Persona_getAltura:;
    BP.i = SP;
    t14.i = BP + 2;
    t15.i = stack[t14].i;
    valorRetorno = t15.i;
    goto L_Z_Persona_getAltura_retorno_final;
    L_Z_Persona_getAltura_retorno:;
    SP.i = BP;
    L_Z_Persona_getAltura_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Metodo saludar =====
    L_Z_Persona_saludar:;
    BP.i = SP;
    Valor t16;
    t16.i = BP + 0;
    Valor t17;
    t17.i = stack[t16].i;
    Valor t18;
    t18.s = concat("Hola, mi nombre es ", t17.s);
    printf("%d\n", t18.i);
    goto L_Z_Persona_saludar_retorno;
    L_Z_Persona_saludar_retorno:;
    SP.i = BP;
    L_Z_Persona_saludar_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Metodo calcularAnioNacimiento =====
    L_Z_Persona_calcularAnioNacimiento:;
    BP.i = SP;
    Valor t19;
    t19.i = BP + 0;
    Valor t20;
    t20.i = stack[t19].i;
    Valor t21;
    t21.i = BP + 2;
    Valor t22;
    t22.i = stack[t21].i;
    Valor t23;
    t23.i = t20.i - t22.i;
    valorRetorno = t23.i;
    goto L_Z_Persona_calcularAnioNacimiento_retorno_final;
    L_Z_Persona_calcularAnioNacimiento_retorno:;
    SP.i = BP;
    L_Z_Persona_calcularAnioNacimiento_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];

    // ===== Metodo esMayorEdad =====
    L_Z_Persona_esMayorEdad:;
    BP.i = SP;
    Valor t24;
    t24.i = BP + 1;
    Valor t25;
    t25.i = stack[t24].i;
    Valor t26;
    t26.i = t25.i >= 18;
    valorRetorno = t26.i;
    goto L_Z_Persona_esMayorEdad_retorno_final;
    L_Z_Persona_esMayorEdad_retorno:;
    SP.i = BP;
    L_Z_Persona_esMayorEdad_retorno_final:;
    goto *pilaRetorno[--ptrRetorno];
    // ===== MAIN =====

    // ===== MAIN =====
    L_Pig_main:;
    BP.i = SP;
    t1.i = BP + 0;
    stack[t1].i = 10;
    t2.i = BP + 1;
    stack[t2].i = 25;
    t3.i = BP + 2;
    stack[t3].s = "Comandante";
    t4.i = BP + 3;
    stack[t4].i = 1;
    printf("Hola comandante!");
    printf("Tu fuerza es:");
    t5.i = BP + 0;
    t6.i = stack[t5].i;
    printf("%d", t6.i);
    t7.i = BP + 1;
    t8.i = stack[t7].i;
    if (t8 >= 18) goto Pig_L1;
    goto Pig_L2;
    Pig_L1:;
    printf("Eres mayor de edad");
    t9.i = BP + 3;
    stack[t9].i = 1;
    goto Pig_L3;
    Pig_L2:;
    Pig_L3:;
    t10.i = BP + 4;
    t11.i = BP + 0;
    t12.i = stack[t11].i;
    stack[SP++].i = t12.i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_1;
    goto L_Y_calcularPoder;
    L_continuar_1:;
    t13.i = valorRetorno;
    stack[t10].i = t13.i;
    printf("Tu poder es:");
    t14.i = BP + 4;
    t15.i = stack[t14].i;
    printf("%d", t15.i);
    t16.i = BP + 5;
    t17.i = BP + 0;
    t18.i = stack[t17].i;
    stack[SP++].i = t18.i;
    t19.i = BP + 1;
    t20.i = stack[t19].i;
    stack[SP++].i = t20.i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_2;
    goto L_Y_sumar;
    L_continuar_2:;
    t21.i = valorRetorno;
    stack[t16].i = t21.i;
    printf("La suma es:");
    t22.i = BP + 5;
    t23.i = stack[t22].i;
    printf("%d", t23.i);
    t24.i = BP + 6;
    t25.i = BP + 1;
    t26.i = stack[t25].i;
    stack[SP++].i = t26.i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_3;
    goto L_Y_esMayorEdad;
    L_continuar_3:;
    Valor t27;
    t27.i = valorRetorno;
    stack[t24].i = t27.i;
    printf("Es mayor de edad?");
    Valor t28;
    t28.i = BP + 6;
    Valor t29;
    t29.i = stack[t28].i;
    printf("%d", t29.i);
    Valor t30;
    t30.i = BP + 7;
    stack[SP++].s = "Carlos";
    stack[SP++].i = 25;
    stack[SP++].i = 1.75;
    Valor t31;
    t31 = alloc_heap(sizeof(Persona));
    stack[t30].i = t31.i;
    printf("Nombre de la persona:");
    Valor t32;
    t32.i = BP + 7;
    Valor t33;
    t33.i = stack[t32].i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_4;
    goto L_Z_t33_getNombre;
    L_continuar_4:;
    Valor t34;
    t34.i = valorRetorno;
    printf("%d", t34.i);
    printf("Edad de la persona:");
    Valor t35;
    t35.i = BP + 7;
    Valor t36;
    t36.i = stack[t35].i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_5;
    goto L_Z_t36_getEdad;
    L_continuar_5:;
    Valor t37;
    t37.i = valorRetorno;
    printf("%d", t37.i);
    Valor t38;
    t38.i = BP + 7;
    Valor t39;
    t39.i = stack[t38].i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_6;
    goto L_Z_t39_saludar;
    L_continuar_6:;
    Valor t40;
    t40.i = valorRetorno;
    Valor t41;
    t41.i = BP + 8;
    Valor t42;
    t42.i = BP + 7;
    Valor t43;
    t43.i = stack[t42].i;
    stack[SP++].i = 2026;
    pilaRetorno[ptrRetorno++] = &&L_continuar_7;
    goto L_Z_t43_calcularAnioNacimiento;
    L_continuar_7:;
    Valor t44;
    t44.i = valorRetorno;
    stack[t41].i = t44.i;
    printf("Anio de nacimiento:");
    Valor t45;
    t45.i = BP + 8;
    Valor t46;
    t46.i = stack[t45].i;
    printf("%d", t46.i);
    Valor t47;
    t47.i = BP + 9;
    Valor t48;
    t48.i = BP + 7;
    Valor t49;
    t49.i = stack[t48].i;
    pilaRetorno[ptrRetorno++] = &&L_continuar_8;
    goto L_Z_t49_esMayorEdad;
    L_continuar_8:;
    Valor t50;
    t50.i = valorRetorno;
    stack[t47].i = t50.i;
    printf("La persona es mayor de edad?");
    Valor t51;
    t51.i = BP + 9;
    Valor t52;
    t52.i = stack[t51].i;
    printf("%d", t52.i);
    L_Pig_main_retorno:;
    return 0;
}

