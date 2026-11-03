package com.tallerwebi.dominio;

public interface RepositorioConexionGoogle {
  ConexionGoogleCalendar buscar(Long empleadorId);
  void guardar(ConexionGoogleCalendar conexion);
}
