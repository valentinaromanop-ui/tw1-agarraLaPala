package com.tallerwebi.presentacion;

@SuppressWarnings("PMD.TooManyFields")
public class DatosOfertaEmpleador {

  private String titulo;
  private String descripcion;
  private String ubicacion;
  private String modalidad;
  private String horario;
  private String seniority;
  private String jornada;
  private String salario;
  private String moneda;
  private String skills;
  private String limitePostulaciones;

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public void setUbicacion(String ubicacion) {
    this.ubicacion = ubicacion;
  }

  public String getModalidad() {
    return modalidad;
  }

  public void setModalidad(String modalidad) {
    this.modalidad = modalidad;
  }

  public String getHorario() {
    return horario;
  }

  public void setHorario(String horario) {
    this.horario = horario;
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

  public String getSalario() {
    return salario;
  }

  public void setSalario(String salario) {
    this.salario = salario;
  }

  public String getMoneda() {
    return moneda;
  }

  public void setMoneda(String moneda) {
    this.moneda = moneda;
  }

  public String getSkills() {
    return skills;
  }

  public void setSkills(String skills) {
    this.skills = skills;
  }

  public String getLimitePostulaciones() {
    return limitePostulaciones;
  }

  public void setLimitePostulaciones(String limitePostulaciones) {
    this.limitePostulaciones = limitePostulaciones;
  }
}
