package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioPostulacionImpl implements ServicioPostulacion {

  private static final String FUENTE_LOCAL = "LOCAL";

  private static final String ESTADO_REGISTRADA = "Registrada";

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioVacante repositorioVacante;

  @Autowired
  public ServicioPostulacionImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioPostulacion repositorioPostulacion,
    RepositorioVacante repositorioVacante
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioVacante = repositorioVacante;
  }

  @Override
  public Postulacion crearPostulacion(Long usuarioId, String ofertaId) {
    if (ofertaId == null || ofertaId.trim().isEmpty()) {
      throw new IllegalArgumentException("La oferta es obligatoria.");
    }
    String identificador = ofertaId.trim();
    Postulacion existente = repositorioPostulacion.buscarPorUsuarioYOferta(
      usuarioId,
      identificador
    );
    if (existente != null) {
      return existente;
    }
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(repositorioUsuario.buscarPorId(usuarioId));
    postulacion.setOfertaId(identificador);
    postulacion.setFechaPostulacion(LocalDateTime.now());
    postulacion.setEstado(ESTADO_REGISTRADA);
    return repositorioPostulacion.guardar(postulacion);
  }

  @Override
  public List<Postulacion> listarPostulaciones(Long usuarioId) {
    return repositorioPostulacion.buscarPorUsuarioId(usuarioId);
  }

  @Override
  public Vacante obtenerVacante(String fuente, Long id) {
    if (FUENTE_LOCAL.equals(fuente)) {
      return repositorioVacante.obtenerPorId(id);
    }
    return repositorioVacante.buscarPorFuenteEIdExterno(fuente, id);
  }

  @Override
  public List<String> obtenerOfertasPostuladas(Long usuarioId) {
    List<String> ofertas = new java.util.ArrayList<>();
    if (usuarioId != null) {
      for (Postulacion postulacion : repositorioPostulacion.buscarPorUsuarioId(usuarioId)) {
        ofertas.add(postulacion.getOfertaId());
      }
    }
    return ofertas;
  }

  @Override
  public Postulacion registrarEnVacante(Long usuarioId, VacanteDTO oferta) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (
      usuario == null ||
      !("CANDIDATO".equals(usuario.getRol()) || "POSTULANTE".equals(usuario.getRol()))
    ) {
      throw new IllegalArgumentException("Iniciá sesión como postulante.");
    }
    String clave = oferta.getFuente() + ":" + oferta.getId();
    Postulacion existente = repositorioPostulacion.buscarPorUsuarioYOferta(usuarioId, clave);
    if (existente != null) {
      return existente;
    }
    Vacante vacante = obtenerOfertaParaPostularse(oferta);
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(usuario);
    postulacion.setVacante(vacante);
    postulacion.setOfertaId(clave);
    postulacion.setEstado(ESTADO_REGISTRADA);
    postulacion.setFechaPostulacion(LocalDateTime.now());
    return repositorioPostulacion.guardar(postulacion);
  }

  private Vacante obtenerOfertaParaPostularse(VacanteDTO oferta) {
    if (FUENTE_LOCAL.equals(oferta.getFuente())) {
      return obtenerLocalActiva(oferta.getId());
    }
    if (!"JOBICY".equals(oferta.getFuente()) || oferta.getId() == null) {
      throw new IllegalArgumentException("La oferta no es válida.");
    }
    Vacante guardada = repositorioVacante.buscarPorFuenteEIdExterno(
      oferta.getFuente(),
      oferta.getId()
    );
    if (guardada == null) {
      guardada = copiarOferta(oferta);
      repositorioVacante.guardar(guardada);
    }
    return guardada;
  }

  private Vacante obtenerLocalActiva(Long id) {
    Vacante local = repositorioVacante.obtenerPorId(id);
    if (
      local == null ||
      !Boolean.TRUE.equals(local.getActiva()) ||
      (local.getFuente() != null && !FUENTE_LOCAL.equals(local.getFuente()))
    ) {
      throw new IllegalArgumentException("Esta vacante ya no está disponible.");
    }
    return local;
  }

  private Vacante copiarOferta(VacanteDTO oferta) {
    Vacante copia = new Vacante();
    copia.setFuente(oferta.getFuente());
    copia.setIdExterno(oferta.getId());
    copia.setUrl(oferta.getUrl());
    copia.setTitulo(oferta.getTitulo());
    copia.setEmpresa(oferta.getEmpresa());
    copia.setDescripcion(oferta.getDescripcion());
    copia.setUbicacion(oferta.getUbicacion());
    copia.setModalidad(oferta.getModalidad());
    copia.setJornada(oferta.getJornada());
    copia.setSeniority(oferta.getSeniority());
    copia.setSalario(oferta.getSalario());
    copia.setMoneda(oferta.getMoneda());
    copia.setHorario(oferta.getHorario());
    copia.setFechaPublicacion(oferta.getFechaPublicacion());
    return copia;
  }
}
