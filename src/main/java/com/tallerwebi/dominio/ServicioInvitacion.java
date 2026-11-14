package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioInvitacion {
  int PORCENTAJE_MINIMO_COINCIDENCIA = 50;

  List<CandidatoSugeridoDTO> obtenerCandidatosSugeridos(Long ofertaId, Long empleadorId);
  boolean puedeInvitar(Long ofertaId, Long empleadorId);
  Invitacion invitar(Long empleadorId, Long candidatoId, Long ofertaId);
  ResumenInvitacionMasiva invitarMasivamente(
    Long empleadorId,
    Long ofertaId,
    int porcentajeMinimo,
    List<String> skills
  );
  List<Invitacion> listarParaCandidato(Long candidatoId);
  void rechazar(Long candidatoId, Long invitacionId);
  void aceptarSiExiste(Long candidatoId, String idOferta);
}
