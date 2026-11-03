package com.tallerwebi.infraestructura;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ConfiguracionGoogleCalendar {

  private final Environment entorno;

  @Autowired
  public ConfiguracionGoogleCalendar(Environment entorno) {
    this.entorno = entorno;
  }

  public String getClientId() {
    return entorno.getProperty("google.calendar.client-id", "");
  }

  public String getClientSecret() {
    return entorno.getProperty("google.calendar.client-secret", "");
  }

  public String getRedirectUri() {
    return entorno.getProperty("google.calendar.redirect-uri", "");
  }

  public byte[] getClave() {
    return Base64.getDecoder().decode(entorno.getProperty("google.calendar.encryption-key", ""));
  }

  public boolean estaDisponible() {
    try {
      return (
        !getClientId().isBlank() &&
        !getClientSecret().isBlank() &&
        !getRedirectUri().isBlank() &&
        getClave().length == 32
      );
    } catch (IllegalArgumentException excepcion) {
      return false;
    }
  }
}
