package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioPostulacion {
  Postulacion guardar(Postulacion postulacion);
  Postulacion buscarPorUsuarioYOferta(Long usuarioId, String ofertaId);
  List<Postulacion> buscarPorUsuarioId(Long usuarioId);
}
