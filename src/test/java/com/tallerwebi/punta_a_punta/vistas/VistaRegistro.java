package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;

public class VistaRegistro extends VistaWeb {

  public VistaRegistro(Page page) {
    super(page);
  }

  public void darClickEnRegistroCandidato() {
    this.darClickEnElElemento("#btn-registro-candidato");
  }

  public void escribirEMAIL(String email) {
    this.escribirEnElElemento("#email", email);
  }

  public void escribirClave(String clave) {
    this.escribirEnElElemento("#password", clave);
  }

  public void repetirClave(String clave) {
    this.escribirEnElElemento("#confirmarPassword", clave);
  }

  public void darClickEnRegistrar() {
    this.darClickEnElElemento("#btn-registrar");
  }
}
