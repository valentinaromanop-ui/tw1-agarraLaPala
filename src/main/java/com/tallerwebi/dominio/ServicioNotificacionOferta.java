package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioNotificacionOferta {
  void notificar(Usuario usuario, String mensaje, String enlace);
  List<NotificacionOferta> listar(Long usuarioId);
  List<NotificacionOferta> listarNoLeidas(Long usuarioId);
  long contarNoLeidas(Long usuarioId);
  String marcarLeidaYObtenerEnlace(Long notificacionId, Long usuarioId);
}
