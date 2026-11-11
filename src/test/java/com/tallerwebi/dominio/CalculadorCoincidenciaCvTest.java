package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CalculadorCoincidenciaCvTest {

  @Test
  public void extraeKeywordsSinTildesNiPalabrasComunes() {
    Set<String> resultado = CalculadorCoincidenciaCv.extraerPalabrasClave(
      "Experiencia en Java y desarrollo de aplicaciones para análisis"
    );

    assertThat(resultado, containsInAnyOrder("desarrollo", "java", "aplicaciones", "analisis"));
  }

  @Test
  public void calculaPorcentajeYPalabrasCoincidentes() {
    VacanteDTO vacante = new VacanteDTO();
    vacante.setTitulo("Desarrollador Java");
    vacante.setDescripcion("Backend con Spring");
    vacante.setSkills(List.of("SQL"));

    CoincidenciaCv resultado = CalculadorCoincidenciaCv.calcular("Java Spring Python", vacante);

    assertThat(resultado.getPorcentaje(), equalTo(67));
    assertThat(resultado.getPalabrasCoincidentes(), contains("java", "spring"));
  }
}
