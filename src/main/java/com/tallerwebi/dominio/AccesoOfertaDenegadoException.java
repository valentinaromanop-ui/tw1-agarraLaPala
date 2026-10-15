package com.tallerwebi.dominio;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesoOfertaDenegadoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AccesoOfertaDenegadoException() {
    super("No tenés permiso para acceder a esta oferta.");
  }
}
