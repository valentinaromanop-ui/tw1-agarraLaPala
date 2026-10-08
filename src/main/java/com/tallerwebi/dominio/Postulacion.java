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
  name = "Postulacion",
  uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "oferta_id" })
)
public class Postulacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "oferta_id", nullable = false, length = 1000)
  private String ofertaId;

  @Column(name = "fecha_postulacion", nullable = false)
  private LocalDateTime fechaPostulacion;

  @Column(nullable = false)
  private String estado;

  @ManyToOne
  @JoinColumn(name = "vacante_id")
  private Vacante vacante;

  public Vacante getVacante() {
    return vacante;
  }

  public void setVacante(Vacante vacante) {
    this.vacante = vacante;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public String getOfertaId() {
    return ofertaId;
  }

  public void setOfertaId(String ofertaId) {
    this.ofertaId = ofertaId;
  }

  public LocalDateTime getFechaPostulacion() {
    return fechaPostulacion;
  }

  public void setFechaPostulacion(LocalDateTime fechaPostulacion) {
    this.fechaPostulacion = fechaPostulacion;
  }

  public String getEstado() {
    return estado;
  }

  public void setEstado(String estado) {
    this.estado = estado;
  }
}
