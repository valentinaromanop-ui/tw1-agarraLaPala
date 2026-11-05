package com.tallerwebi.dominio;

public interface RepositorioEntrevista {
  void guardar(Entrevista entrevista);
  Entrevista buscarPorPostulacionPorId(Long postulacionId);
}
