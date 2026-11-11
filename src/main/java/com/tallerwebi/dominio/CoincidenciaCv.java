package com.tallerwebi.dominio;

import java.util.List;

public final class CoincidenciaCv {

  private final int porcentaje;
  private final List<String> palabrasCoincidentes;

  public CoincidenciaCv(int porcentaje, List<String> palabrasCoincidentes) {
    this.porcentaje = porcentaje;
    this.palabrasCoincidentes = List.copyOf(palabrasCoincidentes);
  }

  public int getPorcentaje() {
    return porcentaje;
  }

  public List<String> getPalabrasCoincidentes() {
    return palabrasCoincidentes;
  }
}
