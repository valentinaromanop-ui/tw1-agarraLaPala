package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;

@Entity
public class Entrevista {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "postulacion_id", nullable = false, unique = true)
  private Postulacion postulacion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoEntrevista estado = EstadoEntrevista.INVITADA;

  @Column(nullable = false)
  private LocalDateTime fechaInvitacion;

  public Entrevista() {}

  public Entrevista(Postulacion postulacion, LocalDateTime fechaInvitacion) {
    this.postulacion = postulacion;
    this.fechaInvitacion = fechaInvitacion;
  }

  public Long getId() {
    return id;
  }

  public Postulacion getPostulacion() {
    return postulacion;
  }

  public EstadoEntrevista getEstado() {
    return estado;
  }

  public LocalDateTime getFechaInvitacion() {
    return fechaInvitacion;
  }
}
