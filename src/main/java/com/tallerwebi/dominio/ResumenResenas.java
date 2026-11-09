package com.tallerwebi.dominio;

import java.math.BigDecimal;

public class ResumenResenas {

  private final Long empleadorId;
  private final BigDecimal promedio;
  private final long cantidad;

  public ResumenResenas(Long empleadorId, BigDecimal promedio, long cantidad) {
    this.empleadorId = empleadorId;
    this.promedio = promedio;
    this.cantidad = cantidad;
  }

  public Long getEmpleadorId() {
    return empleadorId;
  }

  public BigDecimal getPromedio() {
    return promedio;
  }

  public long getCantidad() {
    return cantidad;
  }
}
