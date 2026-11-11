package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
  name = "Invitacion",
  uniqueConstraints = @UniqueConstraint(columnNames = { "candidato_id", "oferta_id" })
)
public class Invitacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "empleador_id", nullable = false)
  private Usuario empleador;

  @ManyToOne
  @JoinColumn(name = "candidato_id", nullable = false)
  private Usuario candidato;

  @ManyToOne
  @JoinColumn(name = "oferta_id", nullable = false)
  private Vacante oferta;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoInvitacion estado = EstadoInvitacion.PENDIENTE;

  @Column(nullable = false)
  private LocalDateTime fecha = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getEmpleador() {
    return empleador;
  }

  public void setEmpleador(Usuario empleador) {
    this.empleador = empleador;
  }

  public Usuario getCandidato() {
    return candidato;
  }

  public void setCandidato(Usuario candidato) {
    this.candidato = candidato;
  }

  public Vacante getOferta() {
    return oferta;
  }

  public void setOferta(Vacante oferta) {
    this.oferta = oferta;
  }

  public EstadoInvitacion getEstado() {
    return estado;
  }

  public void setEstado(EstadoInvitacion estado) {
    this.estado = estado;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }
}
