package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioSkill {
  List<Skill> buscarTodos();
  List<Skill> buscarPorNombre(String nombre);
}
