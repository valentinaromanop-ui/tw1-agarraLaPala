package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioOfertaEmpleadorImpl implements ServicioOfertaEmpleador {

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;
  private final RepositorioPostulacion repositorioPostulacion;

  @Autowired
  public ServicioOfertaEmpleadorImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante,
    RepositorioPostulacion repositorioPostulacion
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
    this.repositorioPostulacion = repositorioPostulacion;
  }

  @Override
  public Usuario obtenerEmpleador(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (usuario == null || !"EMPLEADOR".equalsIgnoreCase(usuario.getRol())) {
      return null;
    }
    return usuario;
  }

  @Override
  public void publicar(Vacante vacante) {
    repositorioVacante.guardar(vacante);
  }

  @Override
  public List<Vacante> listarPorEmpleador(Long empleadorId) {
    return repositorioVacante.buscarPorEmpleador(empleadorId);
  }

  @Override
  public List<Vacante> listarOfertasPublicadas() {
    return repositorioVacante.buscarOfertasPublicadasPorEmpleadores();
  }

  @Override
  public void cerrar(Long vacanteId, Long empleadorId) {
    Vacante vacante = repositorioVacante.obtenerPorIdParaActualizar(vacanteId);
    if (vacante == null) {
      throw new VacanteNoEncontradaException();
    }
    if (empleadorId == null || !empleadorId.equals(vacante.getEmpleadorId())) {
      throw new AccesoOfertaDenegadoException();
    }
    if (vacante.getEstado() == EstadoVacante.CERRADA) {
      throw new OfertaCerradaException();
    }
    vacante.setEstado(EstadoVacante.CERRADA);
  }

  @Override
  public ResumenEmpleador obtenerResumen(Long empleadorId) {
    long activas = 0;
    long cerradas = 0;
    long postulaciones = 0;
    for (Vacante vacante : repositorioVacante.buscarPorEmpleador(empleadorId)) {
      if (vacante.getEstado() == EstadoVacante.ABIERTA) {
        activas++;
      } else {
        cerradas++;
      }
      postulaciones += repositorioPostulacion.contarPorVacante(vacante.getId());
    }
    return new ResumenEmpleador(activas, cerradas, postulaciones);
  }
}
