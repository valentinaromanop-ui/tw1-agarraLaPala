package com.tallerwebi.presentacion;

public class DatosRegistroEmpleador {

  private String empresa;
  private String legajo;
  private String email;
  private String password;
  private String confirmarPassword;

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
}
