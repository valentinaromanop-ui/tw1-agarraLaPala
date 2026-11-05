package com.tallerwebi.dominio;

import java.util.Objects;

public class FotoPerfilPostulanteDTO {

  private final String contentType;
  private final byte[] contenido;

  public FotoPerfilPostulanteDTO(String contentType, byte[] contenido) {
    this.contentType = Objects.requireNonNull(contentType);
    this.contenido = Objects.requireNonNull(contenido).clone();
  }

  public String getContentType() {
    return contentType;
  }

  public byte[] getContenido() {
    return contenido.clone();
  }
}
