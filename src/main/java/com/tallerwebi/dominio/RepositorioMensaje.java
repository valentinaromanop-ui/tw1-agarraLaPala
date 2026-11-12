package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioMensaje {
  Mensaje guardarMensaje(Mensaje mensaje);

  default Mensaje guardar(Mensaje mensaje) {
    return guardarMensaje(mensaje);
  }

  List<Mensaje> listarPorPostulacion(Long postulacionId);
  List<Mensaje> listarConversacionesDeUsuario(Long usuarioId);
  List<Mensaje> listarNoLeidos(Long usuarioId);
  long contarNoLeidos(Long postulacionId, Long usuarioId);
  void marcarLeidos(Long postulacionId, Long usuarioId);
  void marcarTodosLeidos(Long usuarioId);
}
