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
import java.time.Instant;
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

  private Long empleadorId;
  private Instant inicio;
  private Instant fin;
  private String googleEventId;

  @Column(length = 1000)
  private String urlMeet;

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

  public Long getEmpleadorId() {
    return empleadorId;
  }

  public void setEmpleadorId(Long empleadorId) {
    this.empleadorId = empleadorId;
  }

  public Instant getInicio() {
    return inicio;
  }

  public Instant getFin() {
    return fin;
  }

  public String getGoogleEventId() {
    return googleEventId;
  }

  public String getUrlMeet() {
    return urlMeet;
  }

  public void agendar(IntervaloHorario horario, EventoGoogleCalendar evento) {
    inicio = horario.getInicio();
    fin = horario.getFin();
    googleEventId = evento.getId();
    urlMeet = evento.getUrlMeet();
    estado = EstadoEntrevista.AGENDADA;
  }

  public void actualizarUrlMeet(String urlMeet) {
    this.urlMeet = urlMeet;
  }

  public void cambiarHorario(IntervaloHorario horario) {
    inicio = horario.getInicio();
    fin = horario.getFin();
  }

  public void cancelar() {
    estado = EstadoEntrevista.CANCELADA;
  }
}
