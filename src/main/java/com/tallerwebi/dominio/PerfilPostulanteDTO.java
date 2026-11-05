package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PerfilPostulanteDTO {

  private final String nombreCompleto;
  private final String email;
  private final String iniciales;
  private final boolean tieneFoto;
  private final boolean tieneCv;
  private final List<String> habilidades;
  private final String presentacion;
  private final String telefono;
  private final String ubicacion;
  private final String nivelExperiencia;
  private final String tipoBusqueda;
  private final String nivelIngles;
  private final String linkedin;
  private final String github;
  private final List<String> disponibilidadHoraria;
  private final List<String> diasSemana;
  private final List<String> modalidades;
  private final Map<String, String> idiomas;
  private final String tituloOferta;
  private final LocalDateTime fechaPostulacion;
  private final EstadoPostulacion estado;

  public PerfilPostulanteDTO(
    String nombreCompleto,
    String email,
    String iniciales,
    boolean tieneFoto,
    boolean tieneCv,
    List<String> habilidades,
    String presentacion,
    String telefono,
    String ubicacion,
    String nivelExperiencia,
    String tipoBusqueda,
    String nivelIngles,
    String linkedin,
    String github,
    List<String> disponibilidadHoraria,
    List<String> diasSemana,
    List<String> modalidades,
    Map<String, String> idiomas,
    String tituloOferta,
    LocalDateTime fechaPostulacion,
    EstadoPostulacion estado
  ) {
    this.nombreCompleto = nombreCompleto;
    this.email = email;
    this.iniciales = iniciales;
    this.tieneFoto = tieneFoto;
    this.tieneCv = tieneCv;
    this.habilidades = copia(habilidades);
    this.presentacion = presentacion;
    this.telefono = telefono;
    this.ubicacion = ubicacion;
    this.nivelExperiencia = nivelExperiencia;
    this.tipoBusqueda = tipoBusqueda;
    this.nivelIngles = nivelIngles;
    this.linkedin = linkedin;
    this.github = github;
    this.disponibilidadHoraria = copia(disponibilidadHoraria);
    this.diasSemana = copia(diasSemana);
    this.modalidades = copia(modalidades);
    this.idiomas =
      idiomas == null
        ? Map.of()
        : Collections.unmodifiableMap(
          idiomas
            .entrySet()
            .stream()
            .filter(entry ->
              entry.getKey() != null &&
              !entry.getKey().isBlank() &&
              entry.getValue() != null &&
              !entry.getValue().isBlank()
            )
            .collect(
              Collectors.toMap(
                entry -> entry.getKey().trim(),
                entry -> entry.getValue().trim(),
                (primero, segundo) -> primero,
                LinkedHashMap::new
              )
            )
        );
    this.tituloOferta = tituloOferta;
    this.fechaPostulacion = fechaPostulacion;
    this.estado = estado;
  }

  public String getNombreCompleto() {
    return nombreCompleto;
  }

  public String getEmail() {
    return email;
  }

  public String getIniciales() {
    return iniciales;
  }

  public boolean isTieneFoto() {
    return tieneFoto;
  }

  public boolean isTieneCv() {
    return tieneCv;
  }

  public List<String> getHabilidades() {
    return habilidades;
  }

  public String getPresentacion() {
    return presentacion;
  }

  public String getTelefono() {
    return telefono;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public String getNivelExperiencia() {
    return nivelExperiencia;
  }

  public String getTipoBusqueda() {
    return tipoBusqueda;
  }

  public String getNivelIngles() {
    return nivelIngles;
  }

  public String getLinkedin() {
    return linkedin;
  }

  public String getGithub() {
    return github;
  }

  public List<String> getDisponibilidadHoraria() {
    return disponibilidadHoraria;
  }

  public List<String> getDiasSemana() {
    return diasSemana;
  }

  public List<String> getModalidades() {
    return modalidades;
  }

  public Map<String, String> getIdiomas() {
    return idiomas;
  }

  public String getTituloOferta() {
    return tituloOferta;
  }

  public LocalDateTime getFechaPostulacion() {
    return fechaPostulacion;
  }

  public EstadoPostulacion getEstado() {
    return estado;
  }

  private List<String> copia(List<String> valores) {
    if (valores == null) {
      return List.of();
    }
    return valores
      .stream()
      .filter(valor -> valor != null && !valor.isBlank())
      .map(String::trim)
      .toList();
  }
}
