package com.tallerwebi.dominio;

import java.util.List;

public class CandidatoSugeridoDTO {

  private final Long candidatoId;
  private final String nombreCompleto;
  private final String email;
  private final List<String> habilidades;
  private final double porcentajeCoincidencia;
  private final EstadoInvitacion estadoInvitacion;

  public CandidatoSugeridoDTO(
    Long candidatoId,
    String nombreCompleto,
    String email,
    List<String> habilidades,
    double porcentajeCoincidencia,
    EstadoInvitacion estadoInvitacion
  ) {
    this.candidatoId = candidatoId;
    this.nombreCompleto = nombreCompleto;
    this.email = email;
    this.habilidades = habilidades;
    this.porcentajeCoincidencia = porcentajeCoincidencia;
    this.estadoInvitacion = estadoInvitacion;
  }

  public Long getCandidatoId() {
    return candidatoId;
  }

  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public String getEmail() {
    return email;
  }

  public List<String> getHabilidades() {
    return habilidades;
  }

  public double getPorcentajeCoincidencia() {
    return porcentajeCoincidencia;
  }

  public EstadoInvitacion getEstadoInvitacion() {
    return estadoInvitacion;
  }
}
