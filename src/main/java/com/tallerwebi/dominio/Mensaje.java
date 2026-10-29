package com.tallerwebi.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Mensaje {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  private Postulacion postulacion;

  @ManyToOne(optional = false)
  private Usuario emisor;

  @Column(length = 1000, nullable = false)
  private String texto;

  private LocalDateTime fecha = LocalDateTime.now();

  public Postulacion getPostulacion() {
    return postulacion;
  }

  public void setPostulacion(Postulacion postulacion) {
    this.postulacion = postulacion;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getEmisor() {
    return emisor;
  }

  public void setEmisor(Usuario emisor) {
    this.emisor = emisor;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }

  public String getTexto() {
    return texto;
  }

  public void setTexto(String texto) {
    this.texto = texto;
  }
}
