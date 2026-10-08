package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioVacante {
  void guardar(Vacante vacante);

  boolean existePorIdExterno(String idExterno);

  List<Vacante> buscarPorCategoria(String categoria);

  Vacante obtenerPorId(Long id);

  Vacante buscarPorFuenteEIdExterno(String fuente, Long idExterno);

  List<Vacante> buscarPorSkills(List<String> skills);

  List<Vacante> buscarPorEmpleador(Long empleadorId);

  List<Vacante> buscarOfertasPublicadasPorEmpleadores();
}
