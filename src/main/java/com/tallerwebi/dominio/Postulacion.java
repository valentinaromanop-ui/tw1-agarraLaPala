package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
  name = "Postulacion",
  uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "id_oferta" })
)
@SuppressWarnings("PMD.TooManyFields")
public class Postulacion {

  private static final String ESTADO_LEGADO_REGISTRADA = "REGISTRADA";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(name = "id_oferta", nullable = false, length = 1000)
  private String idOferta;

  @Column
  private String titulo;

  private String empresa;
  private String ubicacion;
  private String modalidad;

  @Column(length = 1000)
  private String url;

  @Column(name = "fecha_postulacion", nullable = false)
  private LocalDateTime fechaPostulacion;

  @Column(nullable = false)
  private String estado = EstadoPostulacion.ENVIADA.name();

  @Transient
  private Vacante vacante;

  public Vacante getVacante() {
    return vacante;
  }

  public void setVacante(Vacante vacante) {
    this.vacante = vacante;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public String getOfertaId() {
    return idOferta;
  }

  public void setOfertaId(String ofertaId) {
    this.idOferta = ofertaId;
  }

  public String getIdOferta() {
    return idOferta;
  }

  public void setIdOferta(String idOferta) {
    this.idOferta = idOferta;
  }

  public String getTitulo() {
    return titulo;
  }

  public void setTitulo(String titulo) {
    this.titulo = titulo;
  }

  public String getEmpresa() {
    return empresa;
  }

  public void setEmpresa(String empresa) {
    this.empresa = empresa;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public void setUbicacion(String ubicacion) {
    this.ubicacion = ubicacion;
  }

  public String getModalidad() {
    return modalidad;
  }

  public void setModalidad(String modalidad) {
    this.modalidad = modalidad;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public LocalDateTime getFechaPostulacion() {
    return fechaPostulacion;
  }

  public void setFechaPostulacion(LocalDateTime fechaPostulacion) {
    this.fechaPostulacion = fechaPostulacion;
  }

  public EstadoPostulacion getEstado() {
    if (ESTADO_LEGADO_REGISTRADA.equalsIgnoreCase(estado)) {
      return EstadoPostulacion.ENVIADA;
    }
    return EstadoPostulacion.valueOf(estado.toUpperCase(Locale.ROOT));
  }

  public void setEstado(EstadoPostulacion estado) {
    if (estado != null) {
      this.estado = estado.name();
    }
  }

  public void setEstado(String estado) {
    if (estado == null) {
      return;
    }
    String estadoNormalizado = estado.trim();
    this.estado =
      ESTADO_LEGADO_REGISTRADA.equalsIgnoreCase(estadoNormalizado)
        ? EstadoPostulacion.ENVIADA.name()
        : EstadoPostulacion.valueOf(estadoNormalizado.toUpperCase(Locale.ROOT)).name();
  }
}
