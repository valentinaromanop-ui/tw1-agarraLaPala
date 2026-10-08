package com.tallerwebi.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BusquedaVacanteDTO {

  private String modalidad;
  private String seniority;
  private String jornada;
  private BigDecimal sueldoMinimo;
  private String moneda;

  private List<String> skills = new ArrayList<>();

  public List<String> getSkills() {
    return skills;
  }

  public void setSkills(List<String> skills) {
    this.skills = skills;
  }

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
