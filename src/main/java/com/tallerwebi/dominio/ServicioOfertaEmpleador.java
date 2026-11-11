package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioOfertaEmpleador {
  Usuario obtenerEmpleador(Long usuarioId);

  void publicar(Vacante vacante);

  List<Vacante> listarPorEmpleador(Long empleadorId);

  List<Vacante> listarOfertasPublicadas();

  void cerrar(Long vacanteId, Long empleadorId);

  ResumenEmpleador obtenerResumen(Long empleadorId);

  void registrarVista(Long vacanteId);

  void registrarClickPostular(Long vacanteId);

  long obtenerVistas(Long vacanteId);

  long obtenerClicksPostular(Long vacanteId);

  long obtenerPostulaciones(Long vacanteId);
}
