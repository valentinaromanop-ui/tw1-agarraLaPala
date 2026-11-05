package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
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

  @ManyToOne
  private Usuario receptor;

  @Column(length = 2000, nullable = false)
  private String texto;

  private LocalDateTime fecha = LocalDateTime.now();

  @Column(nullable = false)
  private boolean leido;

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

  public Usuario getEmisor() {
    return emisor;
  }

  public void setEmisor(Usuario emisor) {
    this.emisor = emisor;
  }

  public Usuario getReceptor() {
    return receptor;
  }

  public void setReceptor(Usuario receptor) {
    this.receptor = receptor;
  }

  public String getTexto() {
    return texto;
  }

  public void setTexto(String texto) {
    this.texto = texto;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }

  public boolean isLeido() {
    return leido;
  }

  public Boolean getLeido() {
    return leido;
  }

  public void setLeido(Boolean leido) {
    this.leido = Boolean.TRUE.equals(leido);
  }

  @Transient
  public Usuario getRemitente() {
    return emisor;
  }

  public void setRemitente(Usuario remitente) {
    this.emisor = remitente;
  }

  @Transient
  public String getContenido() {
    return texto;
  }

  public void setContenido(String contenido) {
    this.texto = contenido;
  }

  @Transient
  public LocalDateTime getFechaEnvio() {
    return fecha;
  }

  public void setFechaEnvio(LocalDateTime fechaEnvio) {
    this.fecha = fechaEnvio;
  }
}
