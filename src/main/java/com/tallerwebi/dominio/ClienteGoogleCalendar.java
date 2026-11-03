package com.tallerwebi.dominio;

public interface ClienteGoogleCalendar {
  String SCOPES =
    "https://www.googleapis.com/auth/calendar.events.owned https://www.googleapis.com/auth/calendar.freebusy";

  String urlAutorizacion(String state);
  String intercambiarCodigo(String codigo);
  String renovarAcceso(String refreshToken);
}
