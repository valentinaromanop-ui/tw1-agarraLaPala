package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioCVTest {

  private ServicioCVImpl servicio;

  @BeforeEach
  public void init() {
    this.servicio = new ServicioCVImpl();
  }

  @Test
  public void esmiCVpdfesUnFormatoValido() {
    boolean resultado = servicio.esFormatoValido("miCV.pdf");
    assertThat(resultado, equalTo(true));
  }

  @Test
  public void esMiCvExeUnFormatoInvalido() {
    boolean resultado = servicio.esFormatoValido("miCV.exe");
    assertThat(resultado, equalTo(false));
  }
}
