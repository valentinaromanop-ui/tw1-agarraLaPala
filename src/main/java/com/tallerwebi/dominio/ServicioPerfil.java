package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioPerfil {
  PerfilPostulante obtenerPerfil(Long usuarioId);
  List<Skill> obtenerSkills();
  void guardarPerfil(Long usuarioId, PerfilPostulante datos);
  List<String> obtenerHabilidades(Long usuarioId);

  Usuario obtenerUsuario(Long usuarioId);
  void guardarFotoPerfil(Long usuarioId, byte[] bytes, String contentType);
  void eliminarFotoPerfil(Long usuarioId);
}
