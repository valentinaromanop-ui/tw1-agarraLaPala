package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioNotificacionOferta {
  void notificar(Usuario usuario, String mensaje, String enlace);
  List<NotificacionOferta> listarNoLeidas(Long usuarioId);
  String marcarLeidaYObtenerEnlace(Long notificacionId, Long usuarioId);
}
