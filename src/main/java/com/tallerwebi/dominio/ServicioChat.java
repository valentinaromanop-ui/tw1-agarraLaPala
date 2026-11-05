package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioChat {
  List<Postulacion> listarConversaciones(Long usuarioId);
  List<Mensaje> listarMensajes(Long usuarioId, Long postulacionId);
  Mensaje enviar(Long usuarioId, Long postulacionId, String contenido);
  void marcarLeidos(Long usuarioId, Long postulacionId);
  long contarNoLeidos(Long usuarioId);
}
