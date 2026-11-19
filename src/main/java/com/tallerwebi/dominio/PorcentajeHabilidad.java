package com.tallerwebi.dominio;

public class PorcentajeHabilidad {

  private final String nombre;
  private final int porcentaje;

  public PorcentajeHabilidad(String nombre, int porcentaje) {
    this.nombre = nombre;
    this.porcentaje = porcentaje;
  }

  public String getNombre() {
    return nombre;
  }

  public int getPorcentaje() {
    return porcentaje;
  }
}
