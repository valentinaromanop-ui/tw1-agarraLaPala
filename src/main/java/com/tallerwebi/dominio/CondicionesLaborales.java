package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class CondicionesLaborales {

  @Column
  private String modalidad;

  @Column
  private String seniority;

  @Column
  private String jornada;

  @Column(precision = 19, scale = 2)
  private BigDecimal sueldoMinimo;

  @Column
  private String moneda;

  public String getModalidad() {
    return modalidad;
  }

  public void setModalidad(String modalidad) {
    this.modalidad = modalidad;
  }

  public String getSeniority() {
    return seniority;
  }

  public void setSeniority(String seniority) {
    this.seniority = seniority;
  }

  public String getJornada() {
    return jornada;
  }

  public void setJornada(String jornada) {
    this.jornada = jornada;
  }

  public BigDecimal getSueldoMinimo() {
    return sueldoMinimo;
  }

  public void setSueldoMinimo(BigDecimal sueldoMinimo) {
    this.sueldoMinimo = sueldoMinimo;
  }

  public String getMoneda() {
    return moneda;
  }

  public void setMoneda(String moneda) {
    this.moneda = moneda;
  }
}
