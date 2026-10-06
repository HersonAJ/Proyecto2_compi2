#include <stdio.h>
#include <stdlib.h>
#include <string.h>

char* concat(char* a, char* b) {
    char* r = malloc(strlen(a) + strlen(b) + 1);
    strcpy(r, a);
    strcat(r, b);
    return r;
}

struct Direccion {
    char* calle;
    int numero;
};

struct Persona {
    char* nombre;
    int edad;
    struct Direccion domicilio;
};

struct PersonaZ {
    char* nombre;
    int edad;
    double altura;
};

int contador = 0;
int activo = 1;
int arreglo[5];

int calcularPoder(int fuerza);
int esMayorEdad(int edad);
int sumar(int a, int b);
void PersonaZ_constructor_String_int_double(struct PersonaZ* this, char* nombreParametro, int edadParametro, double alturaParametro);
char* PersonaZ_getNombre(struct PersonaZ* this);
int PersonaZ_getEdad(struct PersonaZ* this);
double PersonaZ_getAltura(struct PersonaZ* this);
void PersonaZ_saludar(struct PersonaZ* this);
int PersonaZ_calcularAnioNacimiento_int(struct PersonaZ* this, int anioActual);
int PersonaZ_esMayorEdad(struct PersonaZ* this);
int main(void);

int calcularPoder(int fuerza) {
    int resultado;

    int t0;
    int t1;

    t0 = (int) fuerza;
    t1 = t0 * 2;
    resultado = t1;
    return resultado;
}

int esMayorEdad(int edad) {
    int resultado;

    int t0;
    int t1;

    t0 = (int) edad;
    t1 = t0 >= 18;
    if (t1 != 0) goto L0;
    goto L1;
L0:
    resultado = 1;
    goto L1;
    resultado = 0;
L1:
    return resultado;
}

int sumar(int a, int b) {
    int resultado;

    int t0;

    t0 = a + b;
    resultado = t0;
    return resultado;
}

void PersonaZ_constructor_String_int_double(struct PersonaZ* this, char* nombreParametro, int edadParametro, double alturaParametro) {
    this->nombre = nombreParametro;
    this->edad = edadParametro;
    this->altura = alturaParametro;
}

char* PersonaZ_getNombre(struct PersonaZ* this) {
    return this->nombre;
}

int PersonaZ_getEdad(struct PersonaZ* this) {
    return this->edad;
}

double PersonaZ_getAltura(struct PersonaZ* this) {
    return this->altura;
}

void PersonaZ_saludar(struct PersonaZ* this) {
    char* t0;

    t0 = concat("Hola, mi nombre es ", this->nombre);
    printf("%s\n", t0);
}

int PersonaZ_calcularAnioNacimiento_int(struct PersonaZ* this, int anioActual) {
    int t0;

    t0 = anioActual - this->edad;
    return t0;
}

int PersonaZ_esMayorEdad(struct PersonaZ* this) {
    int t0;

    t0 = this->edad >= 18;
    return t0;
}

int main(void) {
    struct PersonaZ* p;
    int j;
    int i;
    int cond;
    int inc;
    int entrada;

    struct PersonaZ* t0;
    char* t1;
    int t2;
    int t3;
    int t4;
    int t5;
    int t6;
    int t7;
    int t8;
    int t9;
    int t10;
    int t11;
    int t12;
    int t13;
    int t14;
    int t15;
    int t16;
    int t17;
    int t18;
    int t19;
    int t20;
    int t21;
    int t22;
    int t23;

    printf("%s", "=== INICIO ===");
    printf("%d", 42);
    printf("%f", 3.14);
    printf("%c", 'A');
    printf("%s", "Hola mundo");
    printf("%d", activo);
    t0 = malloc(sizeof(struct PersonaZ));
    PersonaZ_constructor_String_int_double(t0, "Ana", 30, 1.65);
    p = t0;
    printf("%p", p);
    t1 = PersonaZ_getNombre(p);
    printf("%s", t1);
    t2 = sumar(10, 20);
    printf("%d", t2);
L0:
    t3 = (int) contador;
    t4 = t3 < 3;
    if (t4 == 0) goto L1;
    printf("%d", contador);
    t5 = (int) contador;
    t6 = t5 + 1;
    contador = t6;
    goto L0;
L1:
    j = 0;
L2:
    printf("%d", j);
    t7 = (int) j;
    t8 = t7 + 1;
    j = t8;
L3:
    t9 = (int) j;
    t10 = t9 < 3;
    if (t10 != 0) goto L2;
L4:
    i = 0;
L5:
    t11 = (int) i;
    t12 = t11 < 3;
    if (t12 == 0) goto L7;
    printf("%d", i);
L6:
    i = i + 1;
    goto L5;
L7:
    t13 = (int) contador;
    t14 = t13 >= 3;
    if (t14 == 0) goto L9;
    printf("%s", "Mayor");
    goto L8;
L9:
    printf("%s", "Menor");
L8:
    printf("%d", arreglo[0]);
    arreglo[0] = 100;
    printf("%d", arreglo[0]);
    t15 = 10 > 5;
    t16 = 3 < 8;
    t17 = t15 && t16;
    cond = t17;
    printf("%d", cond);
    inc = 5;
    inc = inc + 1;
    printf("%d", inc);
    t18 = 3 * 4;
    t19 = 2 + t18;
    printf("%d", t19);
    t20 = 2 + 3;
    t21 = t20 * 4;
    printf("%d", t21);
    t22 = PersonaZ_calcularAnioNacimiento_int(p, 2026);
    printf("%d", t22);
    t23 = PersonaZ_esMayorEdad(p);
    printf("%d", t23);
    printf("%s", "Ingresa un numero:");
    scanf("%d", &entrada);
    printf("%s", "=== FIN ===");
    return 0;
}

