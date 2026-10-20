package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioCurriculumGenerado {
  CurriculumGenerado buscarPorUsuarioId(Long usuarioId);
  CurriculumGenerado buscarPorUsuarioIdParaActualizar(Long usuarioId);
  CurriculumGenerado guardar(CurriculumGenerado curriculum);
  List<Long> buscarUsuarioIdsConCvOriginal(List<Long> usuarioIds);
}
