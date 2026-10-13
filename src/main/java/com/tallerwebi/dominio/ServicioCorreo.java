package com.tallerwebi.dominio;

@FunctionalInterface
public interface ServicioCorreo {
  void enviar(Correo correo);
}
