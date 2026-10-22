package com.tallerwebi.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class VacanteNoEncontradaException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public VacanteNoEncontradaException() {
    super("La vacante no existe o ya no está disponible.");
  }
}
