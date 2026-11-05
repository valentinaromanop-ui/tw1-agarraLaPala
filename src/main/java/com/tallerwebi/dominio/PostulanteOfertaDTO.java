package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;

public class PostulanteOfertaDTO {

  private final Long postulacionId;
  private final String nombreCompleto;
  private final String email;
  private final List<String> habilidades;
  private final LocalDateTime fechaPostulacion;
  private final boolean tieneCv;
  private final boolean premium;
  private final EstadoPostulacion estado;

  public PostulanteOfertaDTO(
    Long postulacionId,
    String nombreCompleto,
    String email,
    List<String> habilidades,
    LocalDateTime fechaPostulacion,
    boolean tieneCv,
    boolean premium,
    EstadoPostulacion estado
  ) {
    this.postulacionId = postulacionId;
    this.nombreCompleto = nombreCompleto;
    this.email = email;
    this.habilidades = List.copyOf(habilidades);
    this.fechaPostulacion = fechaPostulacion;
    this.tieneCv = tieneCv;
    this.premium = premium;
    this.estado = estado;
  }

  public Long getPostulacionId() {
    return postulacionId;
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

  public LocalDateTime getFechaPostulacion() {
    return fechaPostulacion;
  }

  public boolean isTieneCv() {
    return tieneCv;
  }

  public boolean isPremium() {
    return premium;
  }

  public EstadoPostulacion getEstado() {
    return estado;
  }
}
