package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;

public class PostulanteOfertaDTO {

  private final String email;
  private final List<String> habilidades;
  private final LocalDateTime fechaPostulacion;

  public PostulanteOfertaDTO(
    String email,
    List<String> habilidades,
    LocalDateTime fechaPostulacion
  ) {
    this.email = email;
    this.habilidades = List.copyOf(habilidades);
    this.fechaPostulacion = fechaPostulacion;
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
}
