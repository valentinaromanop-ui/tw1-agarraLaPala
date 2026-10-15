package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
  name = "EmpleoGuardado",
  uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "id_oferta" })
)
public class EmpleoGuardado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "id_oferta", nullable = false, length = 1000)
  private String idOferta;

  @Column(nullable = false)
  private String titulo;

  private String empresa;
  private String ubicacion;
  private String modalidad;

  @Column(length = 1000)
  private String url;

  @Column(name = "fecha_guardado", nullable = false)
  private LocalDateTime fechaGuardado;

  public Long getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public String getIdOferta() {
    return idOferta;
  }

  public void setIdOferta(String idOferta) {
    this.idOferta = idOferta;
  }

  public LocalDateTime getFechaGuardado() {
    return fechaGuardado;
  }

  public void setFechaGuardado(LocalDateTime fechaGuardado) {
    this.fechaGuardado = fechaGuardado;
  }

  // CPD-OFF: accessors mirror the persisted offer snapshot by design.
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
  // CPD-ON
}
