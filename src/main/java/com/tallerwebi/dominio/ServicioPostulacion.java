package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioPostulacion {
  Postulacion crearPostulacion(Long usuarioId, String ofertaId);
  List<Postulacion> listarPostulaciones(Long usuarioId);
}
