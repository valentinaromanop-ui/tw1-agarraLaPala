package com.tallerwebi.dominio;

public interface RepositorioCurriculumGenerado {
  CurriculumGenerado buscarPorUsuarioId(Long usuarioId);
  CurriculumGenerado buscarPorUsuarioIdParaActualizar(Long usuarioId);
  CurriculumGenerado guardar(CurriculumGenerado curriculum);
}
