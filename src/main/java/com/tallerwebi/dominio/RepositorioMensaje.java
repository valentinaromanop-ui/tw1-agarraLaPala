package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioMensaje {
  Mensaje guardarMensaje(Mensaje mensaje);
  Mensaje guardar(Mensaje mensaje);
  List<Mensaje> listarPorPostulacion(Long postulacionId);
  List<Mensaje> listarNoLeidos(Long usuarioId);
  long contarNoLeidos(Long postulacionId, Long usuarioId);
  void marcarLeidos(Long postulacionId, Long usuarioId);
}
