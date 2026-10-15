package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioEmpleoGuardado {
  EmpleoGuardado buscarPorUsuarioYOferta(Long usuarioId, String ofertaId);
  List<EmpleoGuardado> buscarPorUsuarioId(Long usuarioId);
  EmpleoGuardado guardar(EmpleoGuardado empleoGuardado);
  void eliminar(EmpleoGuardado empleoGuardado);
}
