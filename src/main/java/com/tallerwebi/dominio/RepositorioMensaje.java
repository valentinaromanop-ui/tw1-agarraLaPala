package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioMensaje {
  Mensaje guardarMensaje(Mensaje mensaje);
  List<Mensaje> listarPorPostulacion(Long id);
  List<Mensaje> listarConversacionesDeUsuario(Long usuarioId);
  List<Mensaje> listarNoLeidos(Long usuarioId);
  void marcarLeidos(Long postulacionId, Long usuarioId);
}
