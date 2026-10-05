package com.tallerwebi.dominio;

public interface RepositorioPerfilPostulante {
  PerfilPostulante buscarPorUsuarioId(Long usuarioId);
  PerfilPostulante guardar(PerfilPostulante perfil);
  void eliminarSkills(Long perfilId);
  void guardarSkill(PostulanteSkill postulanteSkill);
}
