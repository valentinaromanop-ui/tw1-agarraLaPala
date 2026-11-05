package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioMensaje {
  List<Mensaje> listarPorPostulacion(Long postulacionId);
  long contarNoLeidos(Long postulacionId, Long usuarioId);
  void marcarLeidos(Long postulacionId, Long usuarioId);
  Mensaje guardar(Mensaje mensaje);
}
