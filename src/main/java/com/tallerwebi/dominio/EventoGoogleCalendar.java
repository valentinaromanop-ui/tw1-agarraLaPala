package com.tallerwebi.dominio;

public class EventoGoogleCalendar {

  private final String id;
  private final String urlMeet;

  public EventoGoogleCalendar(String id, String urlMeet) {
    this.id = id;
    this.urlMeet = urlMeet;
  }

  public String getId() {
    return id;
  }

  public String getUrlMeet() {
    return urlMeet;
  }
}
