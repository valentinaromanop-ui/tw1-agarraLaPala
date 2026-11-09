package com.tallerwebi.dominio;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class ReporteResena {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "resena_id", nullable = false)
  private Resena resena;

  @ManyToOne
  @JoinColumn(name = "reportante_id")
  private Usuario reportante;

  private LocalDateTime fecha;

  public Long getId() {
    return id;
  }

  public Resena getResena() {
    return resena;
  }

  public void setResena(Resena resena) {
    this.resena = resena;
  }

  public Usuario getReportante() {
    return reportante;
  }

  public void setReportante(Usuario reportante) {
    this.reportante = reportante;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }
}
