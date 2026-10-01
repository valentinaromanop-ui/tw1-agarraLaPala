package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioVacante {

  void guardar(Vacante vacante);

  boolean existePorIdExterno(String idExterno);

  List<Vacante> buscarPorCategoria(String categoria);

  Vacante obtenerPorId(Long id);
}