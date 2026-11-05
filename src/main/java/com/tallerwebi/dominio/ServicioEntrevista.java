package com.tallerwebi.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public interface ServicioEntrevista {
  ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");
  List<IntervaloHorario> obtenerTurnosDisponibles(Long empleadorId);
  Entrevista invitar(Long vacanteId, Long postulacionId, Long empleadorId);
  Entrevista obtenerInvitacion(Long postulacionId, Long candidatoId);
  Map<LocalDate, List<IntervaloHorario>> obtenerTurnosParaInvitacion(
    Long postulacionId,
    Long candidatoId
  );
  Entrevista confirmar(Long postulacionId, Long candidatoId, Instant inicio);
  Entrevista actualizarEnlaceMeet(Long postulacionId, Long candidatoId);
}
