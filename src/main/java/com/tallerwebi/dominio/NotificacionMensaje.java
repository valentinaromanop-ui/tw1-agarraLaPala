package com.tallerwebi.dominio;

import java.util.Locale;

public class NotificacionMensaje {

  public NotificacionMensaje(Long postulacionId, String emisorNombre, String texto) {
    this.postulacionId = postulacionId;
    this.emisorNombre = emisorNombre;
    this.texto = texto;
  }

  private final Long postulacionId;
  private final String emisorNombre;
  private final String texto;

  public String getTexto() {
    return texto;
  }

  public String getEmisorNombre() {
    return emisorNombre;
  }

  public Long getPostulacionId() {
    return postulacionId;
  }

  public String getInicial() {
    return emisorNombre == null || emisorNombre.isBlank()
      ? "?"
      : emisorNombre.substring(0, 1).toUpperCase(Locale.ROOT);
  }
}
