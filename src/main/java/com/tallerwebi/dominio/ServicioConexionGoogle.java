package com.tallerwebi.dominio;

public interface ServicioConexionGoogle {
  boolean estaDisponible();
  boolean conectado(Long empleadorId);
  String iniciar(Long empleadorId, String state);
  void conectar(Long empleadorId, String codigo);
  String obtenerTokenAcceso(Long empleadorId);
}
