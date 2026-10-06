package com.example.stack_over_pig.c3d_v2.cuartetas.cuartetasZ;


import com.example.stack_over_pig.c3d_v2.cuartetas.genericas.Cuarteta;

// 'break' dentro de un switch. Se traduce literalmente a 'break;' en C
public class RomperSwitch extends Cuarteta {

    public RomperSwitch() {}

    @Override
    public void aCodigoC(StringBuilder sb) {
        sb.append("    break;\n");
    }
}
