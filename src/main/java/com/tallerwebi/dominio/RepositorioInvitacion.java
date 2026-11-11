package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioInvitacion {
  Invitacion guardar(Invitacion invitacion);
  Invitacion buscarPorCandidatoYOferta(Long candidatoId, Long ofertaId);
  List<Invitacion> buscarPorOferta(Long ofertaId);
  List<Invitacion> buscarPorCandidato(Long candidatoId);
  Invitacion buscarPorIdYCandidato(Long invitacionId, Long candidatoId);
}
