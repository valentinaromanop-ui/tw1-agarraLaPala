package com.tallerwebi.dominio;

import java.util.List;

public class MetricasPostulacion {

  private final int total;
  private final List<PorcentajeHabilidad> habilidades;

  public MetricasPostulacion(int total, List<PorcentajeHabilidad> habilidades) {
    this.total = total;
    this.habilidades = habilidades;
  }

  public int getTotal() {
    return total;
  }

  public List<PorcentajeHabilidad> getHabilidades() {
    return habilidades;
  }
}
