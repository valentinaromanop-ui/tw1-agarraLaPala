package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
@SuppressWarnings("PMD.CyclomaticComplexity")
public class ServicioInvitacionImpl implements ServicioInvitacion {

  private static final String FUENTE_LOCAL = "LOCAL";

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;
  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioInvitacion repositorioInvitacion;
  private final ServicioPerfil servicioPerfil;
  private final ServicioNotificacionOferta servicioNotificacionOferta;

  @Autowired
  public ServicioInvitacionImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante,
    RepositorioPostulacion repositorioPostulacion,
    RepositorioInvitacion repositorioInvitacion,
    ServicioPerfil servicioPerfil,
    ServicioNotificacionOferta servicioNotificacionOferta
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioInvitacion = repositorioInvitacion;
    this.servicioPerfil = servicioPerfil;
    this.servicioNotificacionOferta = servicioNotificacionOferta;
  }

  @Override
  public List<CandidatoSugeridoDTO> obtenerCandidatosSugeridos(Long ofertaId, Long empleadorId) {
    Vacante oferta = obtenerOfertaPropia(ofertaId, empleadorId);
    Set<String> skillsOferta = CalculadorCoincidenciaSkills.obtenerSkillsOferta(oferta);
    if (skillsOferta.isEmpty()) {
      return List.of();
    }

    Map<Long, Invitacion> invitacionesPorCandidato = new HashMap<>();
    for (Invitacion invitacion : repositorioInvitacion.buscarPorOferta(ofertaId)) {
      invitacionesPorCandidato.put(invitacion.getCandidato().getId(), invitacion);
    }

    List<CandidatoSugeridoDTO> sugeridos = new ArrayList<>();
    for (Usuario candidato : repositorioUsuario.buscarPostulantes()) {
      if (!esCandidato(candidato) || !esVisible(candidato.getId())) {
        continue;
      }
      if (
        repositorioPostulacion.buscarPorUsuarioYOferta(
          candidato.getId(),
          FUENTE_LOCAL + ":" + ofertaId
        ) !=
        null
      ) {
        continue;
      }

      List<String> habilidades = servicioPerfil.obtenerHabilidades(candidato.getId());
      double porcentaje = CalculadorCoincidenciaSkills.porcentaje(habilidades, skillsOferta);
      if (porcentaje >= PORCENTAJE_MINIMO_COINCIDENCIA) {
        sugeridos.add(
          new CandidatoSugeridoDTO(
            candidato.getId(),
            nombreCompleto(candidato),
            candidato.getEmail(),
            habilidades,
            porcentaje,
            estadoInvitacion(invitacionesPorCandidato.get(candidato.getId()))
          )
        );
      }
    }
    sugeridos.sort(
      Comparator
        .comparingDouble(CandidatoSugeridoDTO::getPorcentajeCoincidencia)
        .reversed()
        .thenComparing(CandidatoSugeridoDTO::getNombreCompleto, String.CASE_INSENSITIVE_ORDER)
    );
    return sugeridos;
  }

  @Override
  public boolean puedeInvitar(Long ofertaId, Long empleadorId) {
    Vacante oferta = obtenerOfertaPropia(ofertaId, empleadorId);
    return ofertaEstaAbierta(oferta) && !cupoCompleto(oferta);
  }

  @Override
  public Invitacion invitar(Long empleadorId, Long candidatoId, Long ofertaId) {
    Usuario empleador = obtenerEmpleadorValido(empleadorId);
    Vacante oferta = obtenerOfertaPropia(empleador, ofertaId);
    validarOfertaInvitable(oferta);
    Usuario candidato = obtenerCandidatoValido(candidatoId);
    validarCandidatoInvitable(candidatoId, ofertaId, oferta);

    Invitacion invitacion = new Invitacion();
    invitacion.setEmpleador(empleador);
    invitacion.setCandidato(candidato);
    invitacion.setOferta(oferta);
    invitacion.setEstado(EstadoInvitacion.PENDIENTE);
    invitacion.setFecha(LocalDateTime.now());
    return guardarYNotificar(invitacion, empleador, candidato, oferta);
  }

  @Override
  public ResumenInvitacionMasiva invitarMasivamente(
    Long empleadorId,
    Long ofertaId,
    int porcentajeMinimo,
    List<String> skills
  ) {
    Usuario empleador = obtenerEmpleadorValido(empleadorId);
    Vacante oferta = obtenerOfertaParaInvitacionMasiva(empleador, ofertaId);
    validarOfertaInvitable(oferta);

    int umbral = Math.max(PORCENTAJE_MINIMO_COINCIDENCIA, Math.min(100, porcentajeMinimo));
    Set<String> skillsOferta = CalculadorCoincidenciaSkills.obtenerSkillsOferta(oferta);
    Set<String> skillsRequeridas = normalizarSkills(skills);
    ConteoInvitaciones conteo = new ConteoInvitaciones();
    for (Usuario candidato : repositorioUsuario.buscarPostulantes()) {
      conteo.registrar(
        invitarSiCumple(
          candidato,
          ofertaId,
          oferta,
          empleador,
          skillsOferta,
          skillsRequeridas,
          umbral
        )
      );
    }
    return conteo.comoResumen();
  }

  private Vacante obtenerOfertaParaInvitacionMasiva(Usuario empleador, Long ofertaId) {
    Vacante oferta = ofertaId == null
      ? null
      : repositorioVacante.obtenerPorIdParaActualizar(ofertaId);
    if (
      oferta == null ||
      !empleador.getId().equals(oferta.getEmpleadorId()) ||
      !FUENTE_LOCAL.equalsIgnoreCase(oferta.getFuente())
    ) {
      throw new IllegalArgumentException("La oferta no pertenece a este empleador.");
    }
    return oferta;
  }

  private Set<String> normalizarSkills(List<String> skills) {
    Set<String> skillsNormalizadas = new HashSet<>();
    if (skills != null) {
      for (String skill : skills) {
        String normalizada = CalculadorCoincidenciaSkills.normalizar(skill);
        if (!normalizada.isEmpty()) {
          skillsNormalizadas.add(normalizada);
        }
      }
    }
    return skillsNormalizadas;
  }

  private ResultadoInvitacion invitarSiCumple(
    Usuario candidato,
    Long ofertaId,
    Vacante oferta,
    Usuario empleador,
    Set<String> skillsOferta,
    Set<String> skillsRequeridas,
    int umbral
  ) {
    if (!esCandidato(candidato)) {
      return ResultadoInvitacion.NO_CUMPLE;
    }
    List<String> habilidades = servicioPerfil.obtenerHabilidades(candidato.getId());
    if (
      CalculadorCoincidenciaSkills.porcentaje(habilidades, skillsOferta) < umbral ||
      !tieneSkillsRequeridas(habilidades, skillsRequeridas)
    ) {
      return ResultadoInvitacion.NO_CUMPLE;
    }
    if (!esVisible(candidato.getId())) {
      return ResultadoInvitacion.NO_VISIBLE;
    }
    if (
      repositorioPostulacion.buscarPorUsuarioYOferta(
        candidato.getId(),
        FUENTE_LOCAL + ":" + ofertaId
      ) !=
      null
    ) {
      return ResultadoInvitacion.POSTULADO;
    }
    if (repositorioInvitacion.buscarPorCandidatoYOferta(candidato.getId(), ofertaId) != null) {
      return ResultadoInvitacion.YA_INVITADO;
    }
    Invitacion invitacion = new Invitacion();
    invitacion.setEmpleador(empleador);
    invitacion.setCandidato(candidato);
    invitacion.setOferta(oferta);
    invitacion.setEstado(EstadoInvitacion.PENDIENTE);
    invitacion.setFecha(LocalDateTime.now());
    guardarYNotificar(invitacion, empleador, candidato, oferta);
    return ResultadoInvitacion.INVITADO;
  }

  @Override
  public List<Invitacion> listarParaCandidato(Long candidatoId) {
    Usuario candidato = candidatoId == null ? null : repositorioUsuario.buscarPorId(candidatoId);
    if (!esCandidato(candidato)) {
      throw new IllegalArgumentException("Iniciá sesión como candidato.");
    }
    return repositorioInvitacion.buscarPorCandidato(candidatoId);
  }

  @Override
  public void rechazar(Long candidatoId, Long invitacionId) {
    Usuario candidato = candidatoId == null ? null : repositorioUsuario.buscarPorId(candidatoId);
    if (!esCandidato(candidato)) {
      throw new IllegalArgumentException("Iniciá sesión como candidato.");
    }
    Invitacion invitacion = repositorioInvitacion.buscarPorIdYCandidato(invitacionId, candidatoId);
    if (invitacion == null) {
      throw new IllegalArgumentException("La invitación no existe.");
    }
    if (invitacion.getEstado() == EstadoInvitacion.PENDIENTE) {
      invitacion.setEstado(EstadoInvitacion.RECHAZADA);
    }
  }

  @Override
  public void aceptarSiExiste(Long candidatoId, String idOferta) {
    if (candidatoId == null || idOferta == null) {
      return;
    }
    String identificador = idOferta.trim().toUpperCase(Locale.ROOT);
    if (!identificador.startsWith(FUENTE_LOCAL + ":")) {
      return;
    }
    try {
      Long ofertaId = Long.valueOf(identificador.substring((FUENTE_LOCAL + ":").length()));
      Invitacion invitacion = repositorioInvitacion.buscarPorCandidatoYOferta(
        candidatoId,
        ofertaId
      );
      if (invitacion != null) {
        invitacion.setEstado(EstadoInvitacion.ACEPTADA);
      }
    } catch (NumberFormatException excepcion) {
      throw new IllegalArgumentException("La oferta no es válida.", excepcion);
    }
  }

  private Vacante obtenerOfertaPropia(Long ofertaId, Long empleadorId) {
    Usuario empleador = empleadorId == null ? null : repositorioUsuario.buscarPorId(empleadorId);
    return obtenerOfertaPropia(empleador, ofertaId);
  }

  private Vacante obtenerOfertaPropia(Usuario empleador, Long ofertaId) {
    Vacante oferta = ofertaId == null ? null : repositorioVacante.obtenerPorId(ofertaId);
    if (
      !esEmpleador(empleador) ||
      oferta == null ||
      !empleador.getId().equals(oferta.getEmpleadorId()) ||
      !FUENTE_LOCAL.equalsIgnoreCase(oferta.getFuente())
    ) {
      throw new IllegalArgumentException("La oferta no pertenece a este empleador.");
    }
    return oferta;
  }

  private Usuario obtenerEmpleadorValido(Long empleadorId) {
    Usuario empleador = empleadorId == null ? null : repositorioUsuario.buscarPorId(empleadorId);
    if (!esEmpleador(empleador)) {
      throw new IllegalArgumentException("Iniciá sesión como empleador.");
    }
    return empleador;
  }

  private Usuario obtenerCandidatoValido(Long candidatoId) {
    Usuario candidato = candidatoId == null ? null : repositorioUsuario.buscarPorId(candidatoId);
    if (!esCandidato(candidato)) {
      throw new IllegalArgumentException("El usuario indicado no es un candidato.");
    }
    return candidato;
  }

  private void validarOfertaInvitable(Vacante oferta) {
    if (!ofertaEstaAbierta(oferta)) {
      throw new OfertaCerradaException();
    }
    if (cupoCompleto(oferta)) {
      throw new CupoPostulacionesCompletoException();
    }
  }

  private void validarCandidatoInvitable(Long candidatoId, Long ofertaId, Vacante oferta) {
    if (!esVisible(candidatoId)) {
      throw new IllegalArgumentException("Este candidato no está visible para empleadores.");
    }
    if (
      repositorioPostulacion.buscarPorUsuarioYOferta(candidatoId, FUENTE_LOCAL + ":" + ofertaId) !=
      null
    ) {
      throw new IllegalArgumentException("No se puede invitar a quien ya se postuló.");
    }
    if (repositorioInvitacion.buscarPorCandidatoYOferta(candidatoId, ofertaId) != null) {
      throw new IllegalArgumentException("Ya existe una invitación para este candidato y oferta.");
    }
    Set<String> skillsOferta = CalculadorCoincidenciaSkills.obtenerSkillsOferta(oferta);
    if (
      CalculadorCoincidenciaSkills.porcentaje(
        servicioPerfil.obtenerHabilidades(candidatoId),
        skillsOferta
      ) <
      PORCENTAJE_MINIMO_COINCIDENCIA
    ) {
      throw new IllegalArgumentException(
        "El candidato debe coincidir al menos un " +
        PORCENTAJE_MINIMO_COINCIDENCIA +
        "% con las habilidades de la oferta."
      );
    }
  }

  private boolean tieneSkillsRequeridas(List<String> habilidades, Set<String> requeridas) {
    if (requeridas.isEmpty()) {
      return true;
    }
    Set<String> normalizadas = new HashSet<>();
    if (habilidades != null) {
      for (String habilidad : habilidades) {
        normalizadas.add(CalculadorCoincidenciaSkills.normalizar(habilidad));
      }
    }
    return normalizadas.containsAll(requeridas);
  }

  private Invitacion guardarYNotificar(
    Invitacion invitacion,
    Usuario empleador,
    Usuario candidato,
    Vacante oferta
  ) {
    Invitacion guardada = repositorioInvitacion.guardar(invitacion);
    notificarInvitacion(empleador, candidato, oferta);
    return guardada;
  }

  private void notificarInvitacion(Usuario empleador, Usuario candidato, Vacante oferta) {
    String empresa = nombreEmpresa(empleador, oferta);
    servicioNotificacionOferta.notificar(
      candidato,
      "La empresa " + empresa + " te invitó a postularte a " + oferta.getTitulo(),
      "/vacantes/detalle?fuente=LOCAL&id=" + oferta.getId()
    );
  }

  private boolean ofertaEstaAbierta(Vacante oferta) {
    return oferta.getEstado() != EstadoVacante.CERRADA && !Boolean.FALSE.equals(oferta.getActiva());
  }

  private boolean cupoCompleto(Vacante oferta) {
    Integer limite = oferta.getLimitePostulaciones();
    return limite != null && repositorioPostulacion.contarPorVacante(oferta.getId()) >= limite;
  }

  private boolean esVisible(Long candidatoId) {
    PerfilPostulante perfil = servicioPerfil.obtenerPerfil(candidatoId);
    return perfil == null || perfil.isVisibleParaEmpleadores();
  }

  private boolean esEmpleador(Usuario usuario) {
    return usuario != null && "EMPLEADOR".equalsIgnoreCase(usuario.getRol());
  }

  private boolean esCandidato(Usuario usuario) {
    return (
      usuario != null &&
      ("CANDIDATO".equalsIgnoreCase(usuario.getRol()) ||
        "POSTULANTE".equalsIgnoreCase(usuario.getRol()))
    );
  }

  private String nombreEmpresa(Usuario empleador, Vacante oferta) {
    String empresa = empleador.getEmpresa();
    if (empresa == null || empresa.isBlank()) {
      empresa = oferta.getEmpresa();
    }
    if (empresa == null || empresa.isBlank()) {
      throw new IllegalArgumentException("El empleador debe tener un nombre de empresa.");
    }
    return empresa.trim();
  }

  private String nombreCompleto(Usuario candidato) {
    String nombre = candidato.getNombre() == null ? "" : candidato.getNombre();
    String apellido = candidato.getApellido() == null ? "" : candidato.getApellido();
    String nombreCompleto = (nombre + " " + apellido).trim();
    if (nombreCompleto.isEmpty()) {
      return candidato.getEmail() == null ? "" : candidato.getEmail();
    }
    return nombreCompleto;
  }

  private EstadoInvitacion estadoInvitacion(Invitacion invitacion) {
    return invitacion == null ? null : invitacion.getEstado();
  }

  private enum ResultadoInvitacion {
    INVITADO,
    POSTULADO,
    YA_INVITADO,
    NO_VISIBLE,
    NO_CUMPLE,
  }

  private static class ConteoInvitaciones {

    private int invitados;
    private int postulados;
    private int yaInvitados;
    private int noVisibles;

    private void registrar(ResultadoInvitacion resultado) {
      switch (resultado) {
        case INVITADO -> {
          invitados += 1;
        }
        case POSTULADO -> {
          postulados += 1;
        }
        case YA_INVITADO -> {
          yaInvitados += 1;
        }
        case NO_VISIBLE -> {
          noVisibles += 1;
        }
        case NO_CUMPLE -> {}
      }
    }

    private ResumenInvitacionMasiva comoResumen() {
      return new ResumenInvitacionMasiva(invitados, postulados, yaInvitados, noVisibles);
    }
  }
}
