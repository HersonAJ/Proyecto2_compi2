import dentro.Funciones2.y
import Persona.z

VARIABILES>
esto fuerza : numerus 10;
esto edad : numerus 25;
esto nombre : textum "Comandante";
esto activo : bool verum;

MAIOR>
>> "Hola comandante!";
>> "Tu fuerza es:";
>> fuerza;

si (edad >= 18) {
    >> "Eres mayor de edad";
    activo = verum;
} finis;

esto poder : numerus calcularPoder(fuerza);
>> "Tu poder es:";
>> poder;

esto suma : numerus sumar(fuerza, edad);
>> "La suma es:";
>> suma;

esto esMayor : bool esMayorEdad(edad);
>> "Es mayor de edad?";
>> esMayor;

esto persona : novus Persona("Carlos", 25, 1.75);
>> "Nombre de la persona:";
>> persona.getNombre();

>> "Edad de la persona:";
>> persona.getEdad();

persona.saludar();

esto anioNac : numerus persona.calcularAnioNacimiento(2026);
>> "Anio de nacimiento:";
>> anioNac;

esto esMayorObj : bool persona.esMayorEdad();
>> "La persona es mayor de edad?";
>> esMayorObj;

FINIS;