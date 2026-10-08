package com.tallerwebi.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("PMD.TooManyFields")
public class VacanteDTO {

  private Long id;
  private String titulo;
  private String empresa;
  private String descripcion;
  private List<String> skills;
  private String ubicacion;
  private String modalidad;
  private String url;
  private LocalDateTime fechaPublicacion;
  private String fuente;
  private BigDecimal salario;
  private String horario;
  private CondicionesLaborales condiciones = new CondicionesLaborales();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getEmpresa() {
    return empresa;
  }

  public void setEmpresa(String empresa) {
    this.empresa = empresa;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  public List<String> getSkills() {
    return skills;
  }

  public void setSkills(List<String> skills) {
    this.skills = skills;
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

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public LocalDateTime getFechaPublicacion() {
    return fechaPublicacion;
  }

  public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
    this.fechaPublicacion = fechaPublicacion;
  }

  public String getFuente() {
    return fuente;
  }

  public void setFuente(String fuente) {
    this.fuente = fuente;
  }

  public BigDecimal getSalario() {
    return salario;
  }

  public void setSalario(BigDecimal salario) {
    this.salario = salario;
  }

  public String getHorario() {
    return horario;
  }

  public void setHorario(String horario) {
    this.horario = horario;
  }

  public CondicionesLaborales getCondiciones() {
    if (condiciones == null) {
      condiciones = new CondicionesLaborales();
    }
    return condiciones;
  }
}
