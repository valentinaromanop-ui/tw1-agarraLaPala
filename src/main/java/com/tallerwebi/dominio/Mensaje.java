package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class Mensaje {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "postulacion_id", nullable = false)
  private Postulacion postulacion;

  @ManyToOne
  @JoinColumn(name = "remitente_id", nullable = false)
  private Usuario remitente;

  @Column(nullable = false, length = 2000)
  private String contenido;

  @Column(nullable = false)
  private LocalDateTime fechaEnvio;

  @Column(nullable = false)
  private Boolean leido = false;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Postulacion getPostulacion() {
    return postulacion;
  }

  public void setPostulacion(Postulacion postulacion) {
    this.postulacion = postulacion;
  }

  public Usuario getRemitente() {
    return remitente;
  }

  public void setRemitente(Usuario remitente) {
    this.remitente = remitente;
  }

  public String getContenido() {
    return contenido;
  }

  public void setContenido(String contenido) {
    this.contenido = contenido;
  }

  public LocalDateTime getFechaEnvio() {
    return fechaEnvio;
  }

  public void setFechaEnvio(LocalDateTime fechaEnvio) {
    this.fechaEnvio = fechaEnvio;
  }

  public Boolean getLeido() {
    return leido;
  }

  public void setLeido(Boolean leido) {
    this.leido = leido;
  }
}
