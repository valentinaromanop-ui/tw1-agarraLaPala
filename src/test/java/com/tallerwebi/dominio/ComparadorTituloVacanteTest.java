package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;

public class ComparadorTituloVacanteTest {

  @Test
  public void compararTitulosIgnoraMayusculas() {
    ComparadorTituloVacante comparador = new ComparadorTituloVacante();

    assertThat(comparador.compare("Analista", "desarrollador"), lessThan(0));
    assertThat(comparador.compare("DEVOPS", "DevOps"), equalTo(0));
  }
}
