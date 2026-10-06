package com.example.contacto_3xtrat3r3str3.c3d_v2.c;

import java.util.List;
import com.example.contacto_3xtrat3r3str3.c3d_v2.cuartetas.genericas.Cuarteta;

/**
 * Una función ya traducida a C:
 *   - tipo de retorno en C
 *   - nombre
 *   - parámetros
 *   - cuádruplas del cuerpo
 *   - tipos C de los temporales (para declararlos correctamente)
 */
public class FuncionC {

    private final String tipoRetornoC;
    private final String nombre;
    private final List<ParametroC> parametros;
    private final List<VariableLocalC> variablesLocales;   // NUEVO
    private final List<Cuarteta> cuartetas;
    private final List<String> tiposTemporales;
    private final List<EstructuraC> estructurasLocales;

    public FuncionC(String tipoRetornoC,
                    String nombre,
                    List<ParametroC> parametros,
                    List<VariableLocalC> variablesLocales,
                    List<EstructuraC> estructurasLocales,
                    List<Cuarteta> cuartetas,
                    List<String> tiposTemporales) {
        this.tipoRetornoC = tipoRetornoC;
        this.nombre = nombre;
        this.parametros = parametros;
        this.variablesLocales = variablesLocales;
        this.estructurasLocales = estructurasLocales != null ? estructurasLocales : new java.util.ArrayList<>();
        this.cuartetas = cuartetas;
        this.tiposTemporales = tiposTemporales;
    }

    public String getTipoRetornoC()             { return tipoRetornoC; }
    public String getNombre()                   { return nombre; }
    public List<ParametroC> getParametros()     { return parametros; }
    public List<VariableLocalC> getVariablesLocales() { return variablesLocales; }
    public List<Cuarteta> getCuartetas()        { return cuartetas; }
    public List<String> getTiposTemporales()    { return tiposTemporales; }

    public void escribirCabecera(StringBuilder sb) {
        sb.append(tipoRetornoC).append(' ').append(nombre).append('(');
        if (parametros.isEmpty()) {
            sb.append("void");
        } else {
            for (int i = 0; i < parametros.size(); i++) {
                if (i > 0) sb.append(", ");
                parametros.get(i).aCodigoC(sb);
            }
        }
        sb.append(')');
    }

    public List<EstructuraC> getEstructurasLocales() { return estructurasLocales; }
}