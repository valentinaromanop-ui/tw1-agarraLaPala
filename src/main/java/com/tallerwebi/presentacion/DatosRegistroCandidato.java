package com.tallerwebi.presentacion;

import java.util.ArrayList;
import java.util.List;

public class DatosRegistroCandidato {

  private String nombre;
  private String apellido;
  private String email;
  private String password;
  private String confirmarPassword;
  private List<String> habilidades = new ArrayList<>();

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

  public String getConfirmarPassword() {
    return confirmarPassword;
  }

  public void setConfirmarPassword(String confirmarPassword) {
    this.confirmarPassword = confirmarPassword;
  }

  public List<String> getHabilidades() {
    return habilidades;
  }

  public void setHabilidades(List<String> habilidades) {
    this.habilidades = habilidades;
  }
}
