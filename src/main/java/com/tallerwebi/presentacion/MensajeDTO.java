package com.tallerwebi.presentacion;

public class MensajeDTO {

  private Long id;

  private String texto;
  private String fecha;
  private Long emisorId;

  public MensajeDTO(Long id, Long emisorId, String texto, String fecha) {
    this.id = id;
    this.emisorId = emisorId;
    this.texto = texto;
    this.fecha = fecha;
  }

  public Long getId() {
    return id;
  }

  public String getFecha() {
    return fecha;
  }

  public void setFecha(String fecha) {
    this.fecha = fecha;
  }

  public String getTexto() {
    return texto;
  }

  public void setTexto(String texto) {
    this.texto = texto;
  }

  public Long getEmisorId() {
    return emisorId;
  }

  public void setEmisorId(Long emisorId) {
    this.emisorId = emisorId;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
