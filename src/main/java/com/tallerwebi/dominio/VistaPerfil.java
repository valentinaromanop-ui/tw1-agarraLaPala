package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class VistaPerfil {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "postulante_id", nullable = false)
  private Usuario postulante;

  @ManyToOne
  @JoinColumn(name = "empleador_id", nullable = false)
  private Usuario empleador;

  private String empresa;

  @Column(name = "oferta_id")
  private Long ofertaId;

  private String ofertaTitulo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TipoVistaPerfil tipo;

  @Column(nullable = false)
  private LocalDateTime fecha;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getPostulante() {
    return postulante;
  }

  public void setPostulante(Usuario postulante) {
    this.postulante = postulante;
  }

  public Usuario getEmpleador() {
    return empleador;
  }

  public void setEmpleador(Usuario empleador) {
    this.empleador = empleador;
  }

  public String getEmpresa() {
    return empresa;
  }

  public void setEmpresa(String empresa) {
    this.empresa = empresa;
  }

  public Long getOfertaId() {
    return ofertaId;
  }

  public void setOfertaId(Long ofertaId) {
    this.ofertaId = ofertaId;
  }

  public String getOfertaTitulo() {
    return ofertaTitulo;
  }

  public void setOfertaTitulo(String ofertaTitulo) {
    this.ofertaTitulo = ofertaTitulo;
  }

  public TipoVistaPerfil getTipo() {
    return tipo;
  }

  public void setTipo(TipoVistaPerfil tipo) {
    this.tipo = tipo;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }
}
