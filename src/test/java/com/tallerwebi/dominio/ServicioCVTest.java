package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class ServiceCVTest {

  @Test
  public void esmiCVpdfesUnFormatoValido() {
    ServicioCVImpl servicio = new ServicioCVImpl();
    boolean resultado = servicio.esFormatoValido("miCV.pdf");
    assertThat(resultado, equalTo(false));
  }
}
