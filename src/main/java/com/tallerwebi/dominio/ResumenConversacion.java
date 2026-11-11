package com.tallerwebi.dominio;

import java.time.LocalDateTime;

public class ResumenConversacion {

  private final Long postulacionId;
  private final String otroParticipante;
  private final String puesto;
  private final String ultimoMensaje;
  private final LocalDateTime fechaUltimoMensaje;
  private final long noLeidos;

  public ResumenConversacion(
    Long postulacionId,
    String otroParticipante,
    String puesto,
    String ultimoMensaje,
    LocalDateTime fechaUltimoMensaje,
    long noLeidos
  ) {
    this.postulacionId = postulacionId;
    this.otroParticipante = otroParticipante;
    this.puesto = puesto;
    this.ultimoMensaje = ultimoMensaje;
    this.fechaUltimoMensaje = fechaUltimoMensaje;
    this.noLeidos = noLeidos;
  }

  public Long getPostulacionId() {
    return postulacionId;
  }

  public String getOtroParticipante() {
    return otroParticipante;
  }

  public String getPuesto() {
    return puesto;
  }

  public String getUltimoMensaje() {
    return ultimoMensaje;
  }

  public LocalDateTime getFechaUltimoMensaje() {
    return fechaUltimoMensaje;
  }

  public long getNoLeidos() {
    return noLeidos;
  }
}
