package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioMensaje {
  Mensaje guardarMensaje(Mensaje mensaje);
  List<Mensaje> listarPorPostulacion(Long id);
}
