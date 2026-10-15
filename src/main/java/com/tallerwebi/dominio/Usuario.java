package com.tallerwebi.dominio;

import jakarta.persistence.*;
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
  private String nombre;
  private String apellido;
  private String telefono;
  private String ubicacion;

  @Lob
  @Column(name = "foto_perfil")
  private byte[] fotoPerfil;

  private String fotoPerfilTipo;

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

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
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

  public byte[] getFotoPerfil() {
    return fotoPerfil == null ? null : fotoPerfil.clone();
  }

  public void setFotoPerfil(byte[] fotoPerfil) {
    this.fotoPerfil = fotoPerfil == null ? fotoPerfil : fotoPerfil.clone();
  } // clone crea una copia del arreglo  Así, quien llama a getFotoPerfil() no puede modificar directamente los datos guardados en Usuari

  public String getFotoPerfilTipo() {
    return fotoPerfilTipo;
  }

  public void setFotoPerfilTipo(String fotoPerfilTipo) {
    this.fotoPerfilTipo = fotoPerfilTipo;
  }

  public boolean tieneFotoPerfil() {
    return fotoPerfil != null && fotoPerfil.length > 0;
  }
}
