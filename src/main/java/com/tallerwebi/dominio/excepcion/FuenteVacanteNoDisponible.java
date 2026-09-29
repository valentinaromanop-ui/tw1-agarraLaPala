package com.tallerwebi.dominio.excepcion;

public class FuenteVacanteNoDisponible extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public FuenteVacanteNoDisponible(String fuente, Throwable causa) {
    super("No pudimos consultar " + fuente + ". Intentá nuevamente más tarde.", causa);
  }
}
