package com.tallerwebi.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@SuppressWarnings("PMD.TooManyFields")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String rol;

  private String empresa;
  private String legajo;
  private String telefono;
  private String ubicacion;

  @ElementCollection
  @CollectionTable(name = "usuario_habilidades", joinColumns = @JoinColumn(name = "usuario_id"))
  @Column(name = "habilidad", nullable = false)
  private List<String> habilidades = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "usuario_idiomas", joinColumns = @JoinColumn(name = "usuario_id"))
  @MapKeyColumn(name = "idioma")
  @Column(name = "nivel")
  private Map<String, String> idiomas = new LinkedHashMap<>();

  private Boolean activo = false;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRol() {
    return rol;
  }

  public void setRol(String rol) {
    this.rol = rol;
  }

  public String getEmpresa() {
    return empresa;
  }

  public void setEmpresa(String empresa) {
    this.empresa = empresa;
  }

  public String getLegajo() {
    return legajo;
  }

  public void setLegajo(String legajo) {
    this.legajo = legajo;
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

  public List<String> getHabilidades() {
    return habilidades;
  }

  public void setHabilidades(List<String> habilidades) {
    this.habilidades = habilidades == null ? new ArrayList<>() : new ArrayList<>(habilidades);
  }

  public Map<String, String> getIdiomas() {
    return idiomas;
  }

  public void setIdiomas(Map<String, String> idiomas) {
    this.idiomas = idiomas == null ? new LinkedHashMap<>() : new LinkedHashMap<>(idiomas);
  }

  public Boolean getActivo() {
    return activo;
  }

  public void setActivo(Boolean activo) {
    this.activo = activo;
  }

  public void activar() {
    activo = true;
  }
}
