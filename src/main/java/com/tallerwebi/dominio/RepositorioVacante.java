package com.tallerwebi.dominio;

import java.util.List;

@FunctionalInterface
public interface RepositorioVacante {
  List<Vacante> buscarPorSkills(List<String> skills);
}
