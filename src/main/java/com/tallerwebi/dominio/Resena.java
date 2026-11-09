package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
  name = "Resena",
  uniqueConstraints = @UniqueConstraint(columnNames = { "candidato_id", "empleador_id" })
)
public class Resena {

  public static final int MAX_LARGO_COMENTARIO = 2000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "candidato_id", nullable = false)
  private Usuario candidato;

  @ManyToOne
  @JoinColumn(name = "empleador_id", nullable = false)
  private Usuario empleador;

  @Column(nullable = false)
  private int puntaje;

  @Column(nullable = false, length = MAX_LARGO_COMENTARIO)
  private String comentario;

  @Column(nullable = false)
  private LocalDateTime fecha;

  @Column(length = MAX_LARGO_COMENTARIO)
  private String respuestaEmpleador;

  private LocalDateTime fechaRespuesta;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getCandidato() {
    return candidato;
  }

  public void setCandidato(Usuario candidato) {
    this.candidato = candidato;
  }

  public Usuario getEmpleador() {
    return empleador;
  }

  public void setEmpleador(Usuario empleador) {
    this.empleador = empleador;
  }

  public int getPuntaje() {
    return puntaje;
  }

  public void setPuntaje(int puntaje) {
    this.puntaje = puntaje;
  }

  public String getComentario() {
    return comentario;
  }

  public void setComentario(String comentario) {
    this.comentario = comentario;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
  }

  public String getRespuestaEmpleador() {
    return respuestaEmpleador;
  }

  public void setRespuestaEmpleador(String respuestaEmpleador) {
    this.respuestaEmpleador = respuestaEmpleador;
  }

  public LocalDateTime getFechaRespuesta() {
    return fechaRespuesta;
  }

  public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
    this.fechaRespuesta = fechaRespuesta;
  }
}
