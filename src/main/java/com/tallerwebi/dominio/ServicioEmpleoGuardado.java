package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioEmpleoGuardado {
  EmpleoGuardado guardar(Long usuarioId, EmpleoGuardado datos);
  void quitar(Long usuarioId, String ofertaId);
  List<EmpleoGuardado> listar(Long usuarioId);
  List<String> listarIds(Long usuarioId);
}
