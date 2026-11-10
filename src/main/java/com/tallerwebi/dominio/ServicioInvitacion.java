package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioInvitacion {
  List<CandidatoSugeridoDTO> obtenerCandidatosSugeridos(Long ofertaId, Long empleadorId);
  boolean puedeInvitar(Long ofertaId, Long empleadorId);
  Invitacion invitar(Long empleadorId, Long candidatoId, Long ofertaId);
  List<Invitacion> listarParaCandidato(Long candidatoId);
  void rechazar(Long candidatoId, Long invitacionId);
  void aceptarSiExiste(Long candidatoId, String idOferta);
}
