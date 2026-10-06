package com.tallerwebi.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@SuppressWarnings("PMD.TooManyFields")
public class PerfilPostulante {

  private static final String COLUMNA_PERFIL_ID = "perfil_id";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "usuario_id", nullable = false, unique = true)
  private Usuario usuario;

  private String telefono;
  private String ubicacion;
  private String nivelExperiencia;
  private String tipoBusqueda;
  private String nivelIngles;
  private String linkedin;
  private String github;

  @Column(length = 500)
  private String presentacion;

  @ElementCollection
  @CollectionTable(
    name = "perfil_disponibilidad",
    joinColumns = @JoinColumn(name = COLUMNA_PERFIL_ID)
  )
  @Column(name = "franja_horaria")
  private List<String> disponibilidadHoraria = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "perfil_dias", joinColumns = @JoinColumn(name = COLUMNA_PERFIL_ID))
  @Column(name = "dia")
  private List<String> diasSemana = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "perfil_modalidades", joinColumns = @JoinColumn(name = COLUMNA_PERFIL_ID))
  @Column(name = "modalidad")
  private List<String> modalidades = new ArrayList<>();

  @OneToMany(mappedBy = "postulante")
  private List<PostulanteSkill> postulanteSkills = new ArrayList<>();

  @Transient
  private List<Long> skillIds = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "perfil_idiomas", joinColumns = @JoinColumn(name = COLUMNA_PERFIL_ID))
  @MapKeyColumn(name = "idioma")
  @Column(name = "nivel")
  private Map<String, String> idiomas = new LinkedHashMap<>();

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

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getUbicacion() {
    return ubicacion;
  }

  public void setUbicacion(String ubicacion) {
    this.ubicacion = ubicacion;
  }

  public String getNivelExperiencia() {
    return nivelExperiencia;
  }

  public void setNivelExperiencia(String nivelExperiencia) {
    this.nivelExperiencia = nivelExperiencia;
  }

  public String getTipoBusqueda() {
    return tipoBusqueda;
  }

  public void setTipoBusqueda(String tipoBusqueda) {
    this.tipoBusqueda = tipoBusqueda;
  }

  public String getNivelIngles() {
    return nivelIngles;
  }

  public void setNivelIngles(String nivelIngles) {
    this.nivelIngles = nivelIngles;
  }

  public String getLinkedin() {
    return linkedin;
  }

  public void setLinkedin(String linkedin) {
    this.linkedin = linkedin;
  }

  public String getGithub() {
    return github;
  }

  public void setGithub(String github) {
    this.github = github;
  }

  public String getPresentacion() {
    return presentacion;
  }

  public void setPresentacion(String presentacion) {
    this.presentacion = presentacion;
  }

  public List<String> getDisponibilidadHoraria() {
    return disponibilidadHoraria;
  }

  public void setDisponibilidadHoraria(List<String> disponibilidadHoraria) {
    this.disponibilidadHoraria = copiar(disponibilidadHoraria);
  }

  public List<String> getDiasSemana() {
    return diasSemana;
  }

  public void setDiasSemana(List<String> diasSemana) {
    this.diasSemana = copiar(diasSemana);
  }

  public List<String> getModalidades() {
    return modalidades;
  }

  public void setModalidades(List<String> modalidades) {
    this.modalidades = copiar(modalidades);
  }

  public List<PostulanteSkill> getPostulanteSkills() {
    return postulanteSkills;
  }

  public void setPostulanteSkills(List<PostulanteSkill> postulanteSkills) {
    this.postulanteSkills = postulanteSkills == null ? new ArrayList<>() : postulanteSkills;
  }

  public List<Long> getSkillIds() {
    return skillIds;
  }

  public void setSkillIds(List<Long> skillIds) {
    this.skillIds = skillIds == null ? new ArrayList<>() : new ArrayList<>(skillIds);
  }

  public Map<String, String> getIdiomas() {
    return idiomas;
  }

  public void setIdiomas(Map<String, String> idiomas) {
    this.idiomas = idiomas == null ? new LinkedHashMap<>() : new LinkedHashMap<>(idiomas);
  }

  private List<String> copiar(List<String> valores) {
    return valores == null ? new ArrayList<>() : new ArrayList<>(valores);
  }
}
