package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioEntrevista {
  List<IntervaloHorario> obtenerTurnosDisponibles(Long empleadorId);
  Entrevista invitar(Long vacanteId, Long postulacionId, Long empleadorId);
  Entrevista obtenerInvitacion(Long postulacionId, Long candidatoId);
}
