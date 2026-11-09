package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioResena {
  List<Resena> buscarPorEmpleador(Long empleadorId);
  List<Resena> buscarPorEmpleadores(List<Long> empleadorIds);
  Resena buscarPorCandidatoYEmpleador(Long candidatoId, Long empleadorId);
  Resena buscarPorId(Long resenaId);
  Resena buscarPorIdParaActualizar(Long resenaId);
  Resena guardar(Resena resena);
  void eliminar(Resena resena);
  void guardarReporte(ReporteResena reporte);
}
