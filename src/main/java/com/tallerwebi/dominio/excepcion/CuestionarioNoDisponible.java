package com.tallerwebi.dominio.excepcion;

public class CuestionarioNoDisponible extends Exception {

  private static final long serialVersionUID = 1L;

  public CuestionarioNoDisponible(String mensaje) {
    super(mensaje);
  }

  public CuestionarioNoDisponible(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
