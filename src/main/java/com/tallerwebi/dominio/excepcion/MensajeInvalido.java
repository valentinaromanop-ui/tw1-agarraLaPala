package com.tallerwebi.dominio.excepcion;

public class MensajeInvalido extends Exception {

  private static final long serialVersionUID = 1L;

  public MensajeInvalido(String mensaje) {
    super(mensaje);
  }
}
