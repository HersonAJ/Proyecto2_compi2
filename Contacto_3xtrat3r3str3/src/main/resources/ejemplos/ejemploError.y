%estructuras

estructura Punto:
	entero x
	entero y

%funciones

definir suma(entero a, entero b) -> entero:
	entero resultado = a + b
	retornar resultado

definir principal():
	entero x = "hola"
	entero y = 20
	entero y = 30
	imprimir(z)
	romper

	si (x) entonces
		imprimir("no deberia")
	contrario
		imprimir("esto tampoco")

	entero w = suma(1, 2, 3)
	Punto p
	imprimir(p.inexistente)

	retornar 5

definir prueba():
	entero i = 0
	mientras (i < 5) hacer
		i++
		romper
		imprimir("nunca se ejecuta")

definir sinRetorno() -> entero:
	entero x = 5
	imprimir(x)


definir probarArreglo():
	entero numeros[3] = {1, 2, 3}
	entero otros[3] = {4, 5, 6}
	numeros = otros


definir probarEstructura():
	Punto p1 = {10, 20, 30}
	Punto p2 = {"hola", 20}


definir probarIndice():
	entero numeros[3] = {1, 2, 3}
	imprimir(numeros[5])
	imprimir(numeros[-1])
	imprimir(numeros[1])

definir probarAsignacionFuncion():
	suma = 5