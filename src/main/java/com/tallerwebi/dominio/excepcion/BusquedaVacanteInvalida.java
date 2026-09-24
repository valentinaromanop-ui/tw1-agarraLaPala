package com.tallerwebi.dominio.excepcion;

public class BusquedaVacanteInvalida extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public BusquedaVacanteInvalida() {
    super("Ingresá al menos una skill para buscar vacantes.");
  }
}
