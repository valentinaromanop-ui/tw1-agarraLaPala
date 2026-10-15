package com.tallerwebi.dominio;

public class CupoPostulacionesCompletoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public CupoPostulacionesCompletoException() {
    super("Esta oferta alcanzó el límite de postulaciones.");
  }
}
