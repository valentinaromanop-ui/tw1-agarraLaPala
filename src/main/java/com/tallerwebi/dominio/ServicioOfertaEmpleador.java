package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioOfertaEmpleador {
  Usuario obtenerEmpleador(Long usuarioId);

  void publicar(Vacante vacante);

  List<Vacante> listarPorEmpleador(Long empleadorId);

  List<Vacante> listarOfertasPublicadas();
}
