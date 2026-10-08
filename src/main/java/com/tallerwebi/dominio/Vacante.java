package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@SuppressWarnings("PMD.TooManyFields")
public class Vacante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column
  private String titulo;

  @Column
  private String empresa;

  @Column(length = 500)
  private String descripcion;

  @Column
  private String ubicacion;

  @Column
  private String modalidad;

  @Column(precision = 12, scale = 2)
  private BigDecimal salario;

  @Column
  private String horario;

  @Column(name = "empleador_id")
  private Long empleadorId;

  @Column(name = "fecha_publicacion")
  private LocalDateTime fechaPublicacion;

  @Column
  private Boolean activa = true;

  @OneToMany(mappedBy = "vacante")
  private Set<VacanteSkill> vacanteSkills = new HashSet<>();

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

  public Long getEmpleadorId() {
    return empleadorId;
  }

  public void setEmpleadorId(Long empleadorId) {
    this.empleadorId = empleadorId;
  }

  public LocalDateTime getFechaPublicacion() {
    return fechaPublicacion;
  }

  public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
    this.fechaPublicacion = fechaPublicacion;
  }

  public Boolean getActiva() {
    return activa;
  }

  public void setActiva(Boolean activa) {
    this.activa = activa;
  }

  public Set<VacanteSkill> getVacanteSkills() {
    return vacanteSkills;
  }

  public void setVacanteSkills(Set<VacanteSkill> vacanteSkills) {
    this.vacanteSkills = vacanteSkills;
  }
}
