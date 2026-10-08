package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioPostulacion {
  Postulacion crearPostulacion(Long usuarioId, String ofertaId);
  Postulacion registrarEnVacante(Long usuarioId, VacanteDTO oferta);
  Vacante obtenerVacante(String fuente, Long id);
  List<String> obtenerOfertasPostuladas(Long usuarioId);
  List<Postulacion> listarPostulaciones(Long usuarioId);
}
