package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "curriculum_generado")
public class CurriculumGenerado {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "usuario_id", nullable = false, unique = true)
  private Usuario usuario;

  @Column(nullable = false)
  private String nombreArchivoOriginal;

  @Lob
  @Column(nullable = false)
  private byte[] archivoOriginal;

  @Lob
  @Column(nullable = true)
  private byte[] archivoAtsPdf;

  @Lob
  @Column(nullable = false)
  private String textoOriginal;

  @Lob
  @Column(nullable = true)
  private String contenidoAts;

  @Column(nullable = false)
  private LocalDateTime generadoEn;

  @Column(nullable = false)
  private boolean descargaUsada;

  public Long getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public String getNombreArchivoOriginal() {
    return nombreArchivoOriginal;
  }

  public void setNombreArchivoOriginal(String nombreArchivoOriginal) {
    this.nombreArchivoOriginal = nombreArchivoOriginal;
  }

  public byte[] getArchivoOriginal() {
    return archivoOriginal == null ? null : archivoOriginal.clone();
  }

  public void setArchivoOriginal(byte[] archivoOriginal) {
    this.archivoOriginal = Objects.requireNonNull(archivoOriginal).clone();
  }

  public byte[] getArchivoAtsPdf() {
    return archivoAtsPdf == null ? null : archivoAtsPdf.clone();
  }

  public void setArchivoAtsPdf(byte[] archivoAtsPdf) {
    this.archivoAtsPdf = Objects.requireNonNull(archivoAtsPdf).clone();
  }

  public String getTextoOriginal() {
    return textoOriginal;
  }

  public void setTextoOriginal(String textoOriginal) {
    this.textoOriginal = textoOriginal;
  }

  public String getContenidoAts() {
    return contenidoAts;
  }

  public void setContenidoAts(String contenidoAts) {
    this.contenidoAts = contenidoAts;
  }

  public LocalDateTime getGeneradoEn() {
    return generadoEn;
  }

  public void setGeneradoEn(LocalDateTime generadoEn) {
    this.generadoEn = generadoEn;
  }

  public boolean isDescargaUsada() {
    return descargaUsada;
  }

  public void setDescargaUsada(boolean descargaUsada) {
    this.descargaUsada = descargaUsada;
  }
}
