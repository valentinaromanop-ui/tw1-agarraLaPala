package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioPostulacionImpl implements ServicioPostulacion {

  private static final String FUENTE_LOCAL = "LOCAL";
  private static final String FUENTE_JOBICY = "JOBICY";
  private static final String OFERTA_NO_VALIDA = "La oferta no es válida.";

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioVacante repositorioVacante;
  private final ServicioPostulantesOferta servicioPostulantesOferta;

  @Autowired
  public ServicioPostulacionImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioPostulacion repositorioPostulacion,
    RepositorioVacante repositorioVacante,
    ServicioPostulantesOferta servicioPostulantesOferta
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioVacante = repositorioVacante;
    this.servicioPostulantesOferta = servicioPostulantesOferta;
  }

  @Override
  public Postulacion crearPostulacion(Long usuarioId, String ofertaId) {
    if (ofertaId == null || ofertaId.trim().isEmpty()) {
      throw new IllegalArgumentException("La oferta es obligatoria.");
    }
    String identificador = ofertaId.trim();
    VacanteDTO oferta = ofertaDesdeIdentificador(identificador);
    if (oferta != null) {
      return registrarEnVacante(usuarioId, oferta);
    }
    Postulacion existente = repositorioPostulacion.buscarPorUsuarioYOferta(
      usuarioId,
      identificador
    );
    if (existente != null) {
      return existente;
    }
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(repositorioUsuario.buscarPorId(usuarioId));
    postulacion.setIdOferta(identificador);
    return guardarPostulacion(usuarioId, postulacion, false);
  }

  @Override
  public Postulacion registrarPostulacion(Long usuarioId, Postulacion postulacion) {
    return guardarPostulacion(usuarioId, postulacion, true);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private Postulacion guardarPostulacion(
    Long usuarioId,
    Postulacion postulacion,
    boolean rechazarDuplicada
  ) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (
      usuario == null ||
      !("CANDIDATO".equalsIgnoreCase(usuario.getRol()) ||
        "POSTULANTE".equalsIgnoreCase(usuario.getRol()))
    ) {
      throw new IllegalArgumentException("Iniciá sesión como postulante.");
    }
    if (postulacion.getIdOferta() == null || postulacion.getIdOferta().trim().isEmpty()) {
      throw new IllegalArgumentException("La oferta es obligatoria.");
    }
    Postulacion existente = repositorioPostulacion.buscarPorUsuarioYOferta(
      usuarioId,
      postulacion.getIdOferta().trim()
    );
    if (existente != null) {
      if (rechazarDuplicada) {
        throw new IllegalArgumentException("Ya te postulaste a esta oferta");
      }
      return existente;
    }
    postulacion.setUsuario(usuario);
    postulacion.setIdOferta(postulacion.getIdOferta().trim());
    postulacion.setFechaPostulacion(LocalDateTime.now());
    postulacion.setEstado(EstadoPostulacion.ENVIADA);
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
        if (postulacion.getOfertaId() != null) {
          ofertas.add(postulacion.getOfertaId().trim());
        }
      }
    }
    return ofertas;
  }

  @Override
  public Postulacion registrarEnVacante(Long usuarioId, VacanteDTO oferta) {
    Usuario usuario = validarCandidato(usuarioId);
    validarOferta(oferta);
    String clave = oferta.getFuente() + ":" + oferta.getId();
    Postulacion existente = repositorioPostulacion.buscarPorUsuarioYOferta(usuarioId, clave);
    if (existente != null) {
      return existente;
    }
    Vacante vacante = obtenerOfertaParaPostularse(oferta);
    validarCupoPostulaciones(vacante);
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(usuario);
    postulacion.setIdOferta(clave);
    postulacion.setVacante(vacante);
    copiarDatos(oferta, postulacion);
    return guardarPostulacion(usuarioId, postulacion, false);
  }

  private Usuario validarCandidato(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (
      usuario == null ||
      !("CANDIDATO".equalsIgnoreCase(usuario.getRol()) ||
        "POSTULANTE".equalsIgnoreCase(usuario.getRol()))
    ) {
      throw new IllegalArgumentException("Iniciá sesión como postulante.");
    }
    return usuario;
  }

  private void validarOferta(VacanteDTO oferta) {
    if (oferta == null || oferta.getId() == null || oferta.getFuente() == null) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA);
    }
  }

  private void validarCupoPostulaciones(Vacante vacante) {
    if (vacante.getEmpleadorId() == null || vacante.getLimitePostulaciones() == null) {
      return;
    }
    if (
      repositorioPostulacion.contarPorVacante(vacante.getId()) >= vacante.getLimitePostulaciones()
    ) {
      throw new CupoPostulacionesCompletoException();
    }
  }

  private VacanteDTO ofertaDesdeIdentificador(String identificador) {
    int separador = identificador.indexOf(':');
    if (separador <= 0 || separador == identificador.length() - 1) {
      return null;
    }
    String fuente = identificador.substring(0, separador).toUpperCase(java.util.Locale.ROOT);
    if (!FUENTE_LOCAL.equals(fuente) && !FUENTE_JOBICY.equals(fuente)) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA);
    }
    try {
      VacanteDTO oferta = new VacanteDTO();
      oferta.setFuente(fuente);
      oferta.setId(Long.valueOf(identificador.substring(separador + 1)));
      return oferta;
    } catch (NumberFormatException excepcion) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA, excepcion);
    }
  }

  private void copiarDatos(VacanteDTO oferta, Postulacion postulacion) {
    postulacion.setTitulo(oferta.getTitulo());
    postulacion.setEmpresa(oferta.getEmpresa());
    postulacion.setUbicacion(oferta.getUbicacion());
    postulacion.setModalidad(oferta.getModalidad());
    postulacion.setUrl(oferta.getUrl());
  }

  @Override
  public long contarPorOferta(Long vacanteId) {
    return repositorioPostulacion.contarPorVacante(vacanteId);
  }

  @Override
  public List<PostulanteOfertaDTO> obtenerPostulantes(Long vacanteId, Long empleadorId) {
    return servicioPostulantesOferta.obtenerPostulantes(vacanteId, empleadorId);
  }

  @Override
  public ArchivoCvPostulanteDTO obtenerCvPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    return servicioPostulantesOferta.obtenerCvPostulante(vacanteId, postulacionId, empleadorId);
  }

  @Override
  public PerfilPostulanteDTO obtenerPerfilPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    return servicioPostulantesOferta.obtenerPerfilPostulante(vacanteId, postulacionId, empleadorId);
  }

  @Override
  public FotoPerfilPostulanteDTO obtenerFotoPerfilPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    return servicioPostulantesOferta.obtenerFotoPerfilPostulante(
      vacanteId,
      postulacionId,
      empleadorId
    );
  }

  private Vacante obtenerOfertaParaPostularse(VacanteDTO oferta) {
    if (FUENTE_LOCAL.equals(oferta.getFuente())) {
      return obtenerLocalActiva(oferta.getId());
    }
    if (!FUENTE_JOBICY.equals(oferta.getFuente()) || oferta.getId() == null) {
      throw new IllegalArgumentException(OFERTA_NO_VALIDA);
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
    if (local == null || (local.getFuente() != null && !FUENTE_LOCAL.equals(local.getFuente()))) {
      throw new VacanteNoEncontradaException();
    }
    if (!Boolean.TRUE.equals(local.getActiva())) {
      throw new IllegalArgumentException("Esta vacante ya no está disponible.");
    }
    if (local.getEstado() == EstadoVacante.CERRADA) {
      throw new OfertaCerradaException();
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
