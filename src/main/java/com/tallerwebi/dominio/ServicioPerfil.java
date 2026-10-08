package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioPerfil {
  PerfilPostulante obtenerPerfil(Long usuarioId);
  List<Skill> obtenerSkills();
  void guardarPerfil(Long usuarioId, PerfilPostulante datos);
}
