package com.tallerwebi.dominio;

public class VacanteNoEncontradaException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VacanteNoEncontradaException() {
    super("La vacante no existe o ya no está disponible.");
  }
}
