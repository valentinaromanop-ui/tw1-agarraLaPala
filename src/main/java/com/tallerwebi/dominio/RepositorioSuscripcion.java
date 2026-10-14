package com.tallerwebi.dominio;

import java.time.LocalDateTime;

public interface RepositorioSuscripcion {
  Suscripcion buscarPorUsuarioId(Long usuarioId);
  Suscripcion guardar(Suscripcion suscripcion);
  boolean estaActiva(Long usuarioId, LocalDateTime ahora);
}
