package com.tallerwebi.dominio;

import java.time.Instant;
import java.util.List;

public interface ClienteGoogleCalendar {
  String SCOPES =
    "https://www.googleapis.com/auth/calendar.events.owned https://www.googleapis.com/auth/calendar.freebusy";

  String urlAutorizacion(String state);
  String intercambiarCodigo(String codigo);
  String renovarAcceso(String refreshToken);
  List<IntervaloHorario> consultarPeriodosOcupados(
    String tokenAcceso,
    Instant desde,
    Instant hasta
  );
  EventoGoogleCalendar crearEventoConMeet(
    String tokenAcceso,
    String eventoId,
    String titulo,
    String emailCandidato,
    IntervaloHorario horario
  );
  String obtenerEnlaceMeet(String tokenAcceso, String eventoId);
}
