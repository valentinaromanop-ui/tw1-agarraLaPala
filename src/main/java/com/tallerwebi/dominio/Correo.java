package com.tallerwebi.dominio;

import java.util.List;

public class Correo {

  private final List<String> destinatarios;
  private final String asunto;
  private final String mensaje;
  private final boolean html;

  public Correo(List<String> destinatarios, String asunto, String mensaje) {
    this(destinatarios, asunto, mensaje, false);
  }

  public Correo(List<String> destinatarios, String asunto, String mensaje, boolean html) {
    this.destinatarios = List.copyOf(destinatarios);
    this.asunto = asunto;
    this.mensaje = mensaje;
    this.html = html;
  }

  public boolean isHtml() {
    return html;
  }

  public List<String> getDestinatarios() {
    return destinatarios;
  }

  public String getAsunto() {
    return asunto;
  }

  public String getMensaje() {
    return mensaje;
  }
}
