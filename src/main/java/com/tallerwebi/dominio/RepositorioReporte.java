package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;

public interface RepositorioReporte {
  void guardar(Reporte reporte);

  Reporte obtener(Long id);

  List<Reporte> listarPendientes();

  boolean existe(TipoReporte tipo, Long objetoId, Long reportanteId);

  long contarDesde(Long reportanteId, LocalDateTime desde);

  long contarResueltosContra(Long usuarioId);
}
