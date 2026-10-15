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

  @Column(nullable = true)
  private String nombreArchivoOriginal;

  @Lob
  @Column(nullable = true)
  private byte[] archivoOriginal;

  @Lob
  @Column(nullable = true)
  private byte[] archivoAtsPdf;

  @Lob
  @Column(nullable = true)
  private String textoOriginal;

  @Lob
  @Column(nullable = true)
  private String contenidoAts;

  @Column(nullable = false)
  private LocalDateTime generadoEn;

  @Column(nullable = false)
  private boolean descargaUsada;

  @Column(nullable = false)
  private boolean atsGratisUsado;

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

  @SuppressWarnings("PMD.NullAssignment")
  public void eliminarOriginal() {
    nombreArchivoOriginal = null;
    archivoOriginal = null;
    textoOriginal = null;
  }

  public byte[] getArchivoAtsPdf() {
    return archivoAtsPdf == null ? null : archivoAtsPdf.clone();
  }

  public void setArchivoAtsPdf(byte[] archivoAtsPdf) {
    this.archivoAtsPdf = Objects.requireNonNull(archivoAtsPdf).clone();
  }

  @SuppressWarnings("PMD.NullAssignment")
  public void eliminarAts() {
    archivoAtsPdf = null;
    contenidoAts = null;
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

  public boolean isAtsGratisUsado() {
    return atsGratisUsado;
  }

  public void setAtsGratisUsado(boolean atsGratisUsado) {
    this.atsGratisUsado = atsGratisUsado;
  }
}
