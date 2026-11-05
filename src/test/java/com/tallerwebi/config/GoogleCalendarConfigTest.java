package com.tallerwebi.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class GoogleCalendarConfigTest {

  @Test
  public void deberiaEstarDisponibleConTodasLasPropiedades() {
    MockEnvironment entorno = new MockEnvironment()
      .withProperty("google.calendar.client-id", "cliente")
      .withProperty("google.calendar.client-secret", "secreto")
      .withProperty("google.calendar.redirect-uri", "http://localhost/callback")
      .withProperty(
        "google.calendar.encryption-key",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
      );
    GoogleCalendarConfig configuracion = new GoogleCalendarConfig(entorno);
    assertTrue(configuracion.estaDisponible());
  }

  @Test
  public void noDeberiaEstarDisponibleSinConfiguracion() {
    GoogleCalendarConfig configuracion = new GoogleCalendarConfig(new MockEnvironment());
    assertFalse(configuracion.estaDisponible());
  }
}
