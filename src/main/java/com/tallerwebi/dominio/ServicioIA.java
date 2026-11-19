package com.tallerwebi.dominio;

/** Contrato para los servicios de inteligencia artificial. */
public interface ServicioIA {
  String generarCvAts(String textoCv, String habilidadesUsuario);
  String generarTexto(String prompt);
}
