package com.tallerwebi.dominio;

public class OfertaCerradaException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  public OfertaCerradaException() {
    super("Esta oferta está cerrada y ya no acepta postulaciones.");
  }
}
