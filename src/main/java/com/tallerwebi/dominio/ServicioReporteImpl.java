package com.tallerwebi.dominio;

import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioReporteImpl implements ServicioReporte {

  public static final int LIMITE_REPORTES_DIARIOS = 5;
  public static final int LIMITE_BLOQUEO_AUTOMATICO = 3;
  private static final String ROL_ADMIN = "ADMIN";

  private final RepositorioReporte repositorioReporte;
  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;

  @Autowired
  public ServicioReporteImpl(
    RepositorioReporte repositorioReporte,
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante
  ) {
    this.repositorioReporte = repositorioReporte;
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
  }

  @Override
  public void reportar(
    TipoReporte tipo,
    Long objetoId,
    Long usuarioReportadoId,
    Long reportanteId,
    String motivo,
    String detalle
  ) {
    if (reportanteId.equals(usuarioReportadoId)) {
      return;
    }
    if (repositorioReporte.existe(tipo, objetoId, reportanteId)) {
      return;
    }
    long reportesHoy = repositorioReporte.contarDesde(reportanteId, LocalDate.now().atStartOfDay());
    if (reportesHoy >= LIMITE_REPORTES_DIARIOS) {
      throw new LimiteReportesAlcanzadoException(
        "Alcanzaste el límite de " + LIMITE_REPORTES_DIARIOS + " reportes por día."
      );
    }
    guardar(tipo, objetoId, usuarioReportadoId, reportanteId, motivo, detalle);
  }

  @Override
  public void registrarAutomatico(
    TipoReporte tipo,
    Long objetoId,
    Long usuarioReportadoId,
    String motivo,
    String detalle
  ) {
    guardar(tipo, objetoId, usuarioReportadoId, null, motivo, detalle);
  }

  @Override
  public List<Reporte> listarPendientes() {
    return repositorioReporte.listarPendientes();
  }

  @Override
  public void bloquearContenido(Long reporteId) {
    Reporte reporte = obtenerReporte(reporteId);
    reporte.setEstado(EstadoReporte.RESUELTO);
    if (TipoReporte.OFERTA.equals(reporte.getTipo())) {
      cambiarEstadoOferta(reporte.getObjetoId(), false);
    }
    bloquearSiSuperaLimite(reporte.getUsuarioReportadoId());
  }

  @Override
  public void bloquearUsuario(Long reporteId) {
    Reporte reporte = obtenerReporte(reporteId);
    reporte.setEstado(EstadoReporte.RESUELTO);
    bloquearUsuarioPorId(reporte.getUsuarioReportadoId());
  }

  @Override
  public void descartar(Long reporteId) {
    Reporte reporte = obtenerReporte(reporteId);
    reporte.setEstado(EstadoReporte.DESCARTADO);
    boolean ocultaAutomatica =
      TipoReporte.OFERTA.equals(reporte.getTipo()) && reporte.getReportanteId() == null;
    if (ocultaAutomatica) {
      cambiarEstadoOferta(reporte.getObjetoId(), true);
    }
  }

  private void guardar(
    TipoReporte tipo,
    Long objetoId,
    Long usuarioReportadoId,
    Long reportanteId,
    String motivo,
    String detalle
  ) {
    Reporte reporte = new Reporte();
    reporte.setTipo(tipo);
    reporte.setObjetoId(objetoId);
    reporte.setUsuarioReportadoId(usuarioReportadoId);
    reporte.setReportanteId(reportanteId);
    reporte.setMotivo(motivo);
    reporte.setDetalle(detalle);
    repositorioReporte.guardar(reporte);
  }

  private Reporte obtenerReporte(Long reporteId) {
    Reporte reporte = repositorioReporte.obtener(reporteId);
    if (reporte == null) {
      throw new IllegalArgumentException("El reporte no existe.");
    }
    return reporte;
  }

  private void cambiarEstadoOferta(Long vacanteId, boolean activa) {
    Vacante vacante = repositorioVacante.obtenerPorId(vacanteId);
    if (vacante != null) {
      vacante.setActiva(activa);
    }
  }

  private void bloquearSiSuperaLimite(Long usuarioId) {
    if (repositorioReporte.contarResueltosContra(usuarioId) >= LIMITE_BLOQUEO_AUTOMATICO) {
      bloquearUsuarioPorId(usuarioId);
    }
  }

  private void bloquearUsuarioPorId(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (usuario == null || ROL_ADMIN.equalsIgnoreCase(usuario.getRol())) {
      return;
    }
    usuario.setBloqueado(true);
    for (Vacante vacante : repositorioVacante.buscarPorEmpleador(usuarioId)) {
      vacante.setActiva(false);
    }
  }
}
