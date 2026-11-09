package com.tallerwebi.dominio;

import java.util.List;
import java.util.Map;

public interface ServicioResena {
  Usuario obtenerEmpleador(Long empleadorId);
  List<Resena> listarPorEmpleador(Long empleadorId);
  Resena buscarPorCandidatoYEmpleador(Long candidatoId, Long empleadorId);
  boolean puedeResenar(Long candidatoId, Long empleadorId);
  ResumenResenas obtenerResumen(Long empleadorId);
  Map<Long, ResumenResenas> obtenerResumenes(List<Long> empleadorIds);
  Resena crear(Long candidatoId, Long empleadorId, int puntaje, String comentario);
  void editar(Long candidatoId, Long empleadorId, Long resenaId, int puntaje, String comentario);
  void eliminar(Long candidatoId, Long empleadorId, Long resenaId);
  void responder(Long empleadorId, Long resenaId, String respuesta);
  void reportar(Long empleadorId, Long resenaId, Long reportanteId);
}
