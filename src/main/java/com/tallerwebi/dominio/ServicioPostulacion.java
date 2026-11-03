package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioPostulacion {
  Postulacion crearPostulacion(Long usuarioId, String ofertaId);
  Postulacion registrarPostulacion(Long usuarioId, Postulacion postulacion);
  Postulacion registrarEnVacante(Long usuarioId, VacanteDTO oferta);
  Vacante obtenerVacante(String fuente, Long id);
  List<String> obtenerOfertasPostuladas(Long usuarioId);
  List<Postulacion> listarPostulaciones(Long usuarioId);
  long contarPorOferta(Long vacanteId);
  List<PostulanteOfertaDTO> obtenerPostulantes(Long vacanteId, Long empleadorId);
  Postulacion cambiarEstado(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId,
    EstadoPostulacion nuevoEstado
  );
  ArchivoCvPostulanteDTO obtenerCvPostulante(Long vacanteId, Long postulacionId, Long empleadorId);
  PerfilPostulanteDTO obtenerPerfilPostulante(Long vacanteId, Long postulacionId, Long empleadorId);
  FotoPerfilPostulanteDTO obtenerFotoPerfilPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  );
}
