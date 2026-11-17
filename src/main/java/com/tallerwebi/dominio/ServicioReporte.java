package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioReporte {
  void reportar(
    TipoReporte tipo,
    Long objetoId,
    Long usuarioReportadoId,
    Long reportanteId,
    String motivo,
    String detalle
  );

  void registrarAutomatico(
    TipoReporte tipo,
    Long objetoId,
    Long usuarioReportadoId,
    String motivo,
    String detalle
  );

  List<Reporte> listarPendientes();

  void bloquearContenido(Long reporteId);

  void bloquearUsuario(Long reporteId);

  void descartar(Long reporteId);
}
