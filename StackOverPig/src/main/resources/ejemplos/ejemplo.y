%estructuras

estructura Punto:
	entero x
	entero y
	flotante promedio

estructura Persona:
	entero edad
	cadena nombre
	Punto ubicacion

%funciones

definir suma(entero a, entero b) -> entero:
	entero resultado = a + b
	retornar resultado

definir principal():
	entero x = 10
	entero y = 20
	entero z = suma(x, y)
	imprimir(z)

	si (z > 25) entonces
		imprimir("Mayor a 25")
	sino (z == 25) entonces
		imprimir("Igual a 25")
	contrario
		imprimir("Menor a 25")

	para (entero i = 0; i < 5; i++):
		imprimir(i)

	entero contador = 0
	mientras (contador < 3) hacer
		contador++
		imprimir(contador)