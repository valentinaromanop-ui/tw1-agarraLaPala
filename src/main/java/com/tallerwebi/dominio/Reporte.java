package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Reporte {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  private TipoReporte tipo;

  private Long objetoId;
  private Long usuarioReportadoId;
  private Long reportanteId;
  private String motivo;

  @Column(length = 500)
  private String detalle;

  @Enumerated(EnumType.STRING)
  private EstadoReporte estado = EstadoReporte.PENDIENTE;

  private LocalDateTime fecha = LocalDateTime.now();

  public Long getId() {
    return id;
  }

  public TipoReporte getTipo() {
    return tipo;
  }

  public void setTipo(TipoReporte tipo) {
    this.tipo = tipo;
  }

  public Long getObjetoId() {
    return objetoId;
  }

  public void setObjetoId(Long objetoId) {
    this.objetoId = objetoId;
  }

  public Long getUsuarioReportadoId() {
    return usuarioReportadoId;
  }

  public void setUsuarioReportadoId(Long usuarioReportadoId) {
    this.usuarioReportadoId = usuarioReportadoId;
  }

  public Long getReportanteId() {
    return reportanteId;
  }

  public void setReportanteId(Long reportanteId) {
    this.reportanteId = reportanteId;
  }

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String motivo) {
    this.motivo = motivo;
  }

  public String getDetalle() {
    return detalle;
  }

  public void setDetalle(String detalle) {
    this.detalle = detalle;
  }

  public EstadoReporte getEstado() {
    return estado;
  }

  public void setEstado(EstadoReporte estado) {
    this.estado = estado;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }
}
