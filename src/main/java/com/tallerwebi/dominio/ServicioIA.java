package com.tallerwebi.dominio;

public interface ServicioIA {
  String generarCvAts(String textoCv, String habilidadesUsuario);
  String generarTexto(String prompt);
}
