package com.tallerwebi.dominio;

import java.time.Instant;

public class IntervaloHorario {

  private final Instant inicio;
  private final Instant fin;

  public IntervaloHorario(Instant inicio, Instant fin) {
    if (inicio == null || fin == null || !inicio.isBefore(fin)) {
      throw new IllegalArgumentException("El inicio debe ser anterior al fin.");
    }
    this.inicio = inicio;
    this.fin = fin;
  }

  public Instant getInicio() {
    return inicio;
  }

  public Instant getFin() {
    return fin;
  }

  public boolean seSuperponeCon(IntervaloHorario otro) {
    return inicio.isBefore(otro.fin) && fin.isAfter(otro.inicio);
  }
}
