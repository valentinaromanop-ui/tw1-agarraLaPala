package com.tallerwebi.dominio.excepcion;

public class ConexionGoogleException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConexionGoogleException() {
    super("No se pudo conectar con Google Calendar. Volvé a intentar la conexión.");
  }
}
