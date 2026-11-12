package com.tallerwebi.dominio;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service("servicioEntrevista")
public class ServicioEntrevistaImpl implements ServicioEntrevista {

  private static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
  private static final LocalTime HORA_FIN = LocalTime.of(18, 0);
  private static final Duration DURACION_TURNO = Duration.ofMinutes(30);
  private static final int DIAS_DISPONIBLES = 14;

  private final ServicioConexionGoogle servicioConexionGoogle;
  private final ClienteGoogleCalendar clienteGoogleCalendar;
  private final Clock reloj;
  private final RepositorioEntrevista repositorioEntrevista;
  private final RepositorioUsuario repositorioUsuario;
  private final ServicioPostulantesOferta servicioPostulantesOferta;
  private final ApplicationEventPublisher eventos;
  private final String urlBase;

  @Autowired
  public ServicioEntrevistaImpl(
    ServicioConexionGoogle servicioConexionGoogle,
    ClienteGoogleCalendar clienteGoogleCalendar,
    RepositorioEntrevista repositorioEntrevista,
    ServicioPostulantesOferta servicioPostulantesOferta,
    RepositorioUsuario repositorioUsuario,
    ApplicationEventPublisher eventos,
    @Value("${app.url:http://localhost:8080}") String urlBase
  ) {
    this(
      servicioConexionGoogle,
      clienteGoogleCalendar,
      repositorioEntrevista,
      servicioPostulantesOferta,
      repositorioUsuario,
      eventos,
      urlBase,
      Clock.systemUTC()
    );
  }

  public ServicioEntrevistaImpl(
    ServicioConexionGoogle servicioConexionGoogle,
    ClienteGoogleCalendar clienteGoogleCalendar,
    RepositorioEntrevista repositorioEntrevista,
    ServicioPostulantesOferta servicioPostulantesOferta,
    RepositorioUsuario repositorioUsuario,
    ApplicationEventPublisher eventos,
    String urlBase,
    Clock reloj
  ) {
    this.servicioConexionGoogle = servicioConexionGoogle;
    this.clienteGoogleCalendar = clienteGoogleCalendar;
    this.repositorioEntrevista = repositorioEntrevista;
    this.servicioPostulantesOferta = servicioPostulantesOferta;
    this.repositorioUsuario = repositorioUsuario;
    this.eventos = eventos;
    this.urlBase = urlBase;
    this.reloj = reloj;
  }

  @Override
  @Transactional
  public Entrevista invitar(Long vacanteId, Long postulacionId, Long empleadorId) {
    Postulacion postulacion = servicioPostulantesOferta.buscarPostulacionPropia(
      vacanteId,
      postulacionId,
      empleadorId
    );
    if (postulacion.getEstado() == EstadoPostulacion.RECHAZADA) {
      throw new IllegalArgumentException("No se puede citar una postulación rechazada.");
    }
    if (repositorioEntrevista.buscarPorPostulacionPorId(postulacionId) != null) {
      throw new IllegalArgumentException("Ya se envió una invitación a esta postulación.");
    }
    if (!servicioConexionGoogle.conectado(empleadorId)) {
      throw new IllegalArgumentException("Conectá Google Calendar desde tu perfil antes de citar.");
    }
    Entrevista entrevista = new Entrevista(
      postulacion,
      LocalDateTime.ofInstant(reloj.instant(), ZONA_HORARIA)
    );
    entrevista.setEmpleadorId(empleadorId);
    repositorioEntrevista.guardar(entrevista);
    String mensaje =
      "Te invitaron a una entrevista por tu postulación en Agarra la Pala." +
      "\nIngresá con tu cuenta de postulante para consultar la invitación:" +
      "\n" +
      urlBase +
      "/entrevistas/" +
      postulacionId;

    eventos.publishEvent(
      new Correo(List.of(postulacion.getUsuario().getEmail()), "Invitación a entrevista", mensaje)
    );
    return entrevista;
  }

  @Override
  @Transactional
  public Entrevista obtenerInvitacion(Long postulacionId, Long candidatoId) {
    Entrevista entrevista = repositorioEntrevista.buscarPorPostulacionPorId(postulacionId);
    return validarInvitacion(entrevista, candidatoId);
  }

  private Entrevista validarInvitacion(Entrevista entrevista, Long candidatoId) {
    if (entrevista == null) {
      throw new IllegalArgumentException("No encontramos una invitación para esta postulación.");
    }
    if (
      candidatoId == null || !candidatoId.equals(entrevista.getPostulacion().getUsuario().getId())
    ) {
      throw new AccesoOfertaDenegadoException();
    }
    return entrevista;
  }

  @Override
  @Transactional
  public Map<LocalDate, List<IntervaloHorario>> obtenerTurnosParaInvitacion(
    Long postulacionId,
    Long candidatoId
  ) {
    Entrevista entrevista = obtenerInvitacion(postulacionId, candidatoId);
    if (entrevista.getEstado() == EstadoEntrevista.AGENDADA) {
      return Map.of();
    }
    validarPostulacionVigente(entrevista);
    Long empleadorId = obtenerEmpleadorId(entrevista);
    String token = servicioConexionGoogle.obtenerTokenAcceso(empleadorId);
    return calcularTurnosPorDia(empleadorId, token);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Entrevista confirmar(Long postulacionId, Long candidatoId, Instant inicio) {
    Entrevista entrevista = validarInvitacion(
      repositorioEntrevista.buscarPorPostulacionParaActualizar(postulacionId),
      candidatoId
    );
    if (entrevista.getEstado() == EstadoEntrevista.AGENDADA) {
      return entrevista;
    }
    validarPostulacionVigente(entrevista);
    Long empleadorId = obtenerEmpleadorId(entrevista);

    // Espera a que termine cualquier otra reserva del mismo empleador.
    repositorioUsuario.buscarPorIdParaActualizar(empleadorId);
    String token = servicioConexionGoogle.obtenerTokenAcceso(empleadorId);
    List<IntervaloHorario> turnosDisponibles = calcularTurnosDisponibles(empleadorId, token);
    IntervaloHorario horario = buscarTurno(inicio, turnosDisponibles);

    Postulacion postulacion = entrevista.getPostulacion();
    String titulo = postulacion.getTitulo();
    EventoGoogleCalendar evento = clienteGoogleCalendar.crearEventoConMeet(
      token,
      identificadorEvento(entrevista),
      titulo == null ? "Entrevista laboral" : "Entrevista: " + titulo,
      postulacion.getUsuario().getEmail(),
      horario
    );
    entrevista.setEmpleadorId(empleadorId);
    entrevista.agendar(horario, evento);
    return entrevista;
  }

  @Override
  @Transactional
  public Entrevista actualizarEnlaceMeet(Long postulacionId, Long candidatoId) {
    Entrevista entrevista = obtenerInvitacion(postulacionId, candidatoId);
    if (entrevista.getEstado() == EstadoEntrevista.AGENDADA && entrevista.getUrlMeet() == null) {
      String token = servicioConexionGoogle.obtenerTokenAcceso(obtenerEmpleadorId(entrevista));
      String enlace = clienteGoogleCalendar.obtenerEnlaceMeet(token, entrevista.getGoogleEventId());
      entrevista.actualizarUrlMeet(enlace);
    }
    return entrevista;
  }

  @Override
  @Transactional
  public Entrevista obtenerParaEmpleador(Long vacanteId, Long postulacionId, Long empleadorId) {
    servicioPostulantesOferta.buscarPostulacionPropia(vacanteId, postulacionId, empleadorId);
    Entrevista entrevista = repositorioEntrevista.buscarPorPostulacionPorId(postulacionId);
    if (entrevista == null) {
      throw new IllegalArgumentException(
        "Todavía no se envió una invitación para esta postulación."
      );
    }
    return entrevista;
  }

  @Override
  @Transactional
  public Map<LocalDate, List<IntervaloHorario>> obtenerTurnosParaEmpleador(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    Entrevista entrevista = obtenerParaEmpleador(vacanteId, postulacionId, empleadorId);
    validarAgendada(entrevista);
    Long organizadorId = obtenerEmpleadorId(entrevista);
    String token = servicioConexionGoogle.obtenerTokenAcceso(organizadorId);
    return calcularTurnosPorDia(organizadorId, token);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void modificar(Long vacanteId, Long postulacionId, Long empleadorId, Instant inicio) {
    obtenerParaEmpleador(vacanteId, postulacionId, empleadorId);
    Entrevista entrevista = repositorioEntrevista.buscarPorPostulacionParaActualizar(postulacionId);
    validarAgendada(entrevista);
    validarPostulacionVigente(entrevista);
    if (entrevista.getInicio().equals(inicio)) {
      return;
    }
    Long organizadorId = obtenerEmpleadorId(entrevista);
    repositorioUsuario.buscarPorIdParaActualizar(organizadorId);
    String token = servicioConexionGoogle.obtenerTokenAcceso(organizadorId);
    IntervaloHorario horario = buscarTurno(inicio, calcularTurnosDisponibles(organizadorId, token));
    clienteGoogleCalendar.modificarHorario(token, entrevista.getGoogleEventId(), horario);
    entrevista.cambiarHorario(horario);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void cancelar(Long vacanteId, Long postulacionId, Long empleadorId) {
    obtenerParaEmpleador(vacanteId, postulacionId, empleadorId);
    Entrevista entrevista = repositorioEntrevista.buscarPorPostulacionParaActualizar(postulacionId);
    if (entrevista.getEstado() == EstadoEntrevista.CANCELADA) {
      return;
    }
    validarAgendada(entrevista);
    Long organizadorId = obtenerEmpleadorId(entrevista);
    repositorioUsuario.buscarPorIdParaActualizar(organizadorId);
    clienteGoogleCalendar.eliminarEvento(
      servicioConexionGoogle.obtenerTokenAcceso(organizadorId),
      entrevista.getGoogleEventId()
    );
    entrevista.cancelar();
  }

  private void validarAgendada(Entrevista entrevista) {
    if (entrevista.getEstado() != EstadoEntrevista.AGENDADA) {
      throw new IllegalArgumentException(
        "Solo se pueden modificar o cancelar entrevistas agendadas."
      );
    }
  }

  private Long obtenerEmpleadorId(Entrevista entrevista) {
    if (entrevista.getEmpleadorId() != null) {
      return entrevista.getEmpleadorId();
    }
    Vacante vacante = servicioPostulantesOferta.obtenerVacanteDePostulacion(
      entrevista.getPostulacion()
    );
    return vacante.getEmpleadorId();
  }

  private void validarPostulacionVigente(Entrevista entrevista) {
    if (entrevista.getEstado() == EstadoEntrevista.CANCELADA) {
      throw new IllegalArgumentException("La entrevista fue cancelada.");
    }
    if (entrevista.getPostulacion().getEstado() == EstadoPostulacion.RECHAZADA) {
      throw new IllegalArgumentException(
        "La postulación fue rechazada y ya no permite reservar una entrevista."
      );
    }
  }

  private IntervaloHorario buscarTurno(Instant inicio, List<IntervaloHorario> turnos) {
    for (IntervaloHorario turno : turnos) {
      if (turno.getInicio().equals(inicio)) {
        return turno;
      }
    }
    throw new IllegalArgumentException("El horario no está disponible. Elegí otra opción.");
  }

  private String identificadorEvento(Entrevista entrevista) {
    // Es estable para la misma invitacion: un reintento no crea otro evento en Google.
    String identificador = entrevista.getId() + "-" + entrevista.getFechaInvitacion();
    return (
      "entrevista" +
      UUID
        .nameUUIDFromBytes(identificador.getBytes(StandardCharsets.UTF_8))
        .toString()
        .replace("-", "")
    );
  }

  @Override
  @Transactional
  public List<IntervaloHorario> obtenerTurnosDisponibles(Long empleadorId) {
    String token = servicioConexionGoogle.obtenerTokenAcceso(empleadorId);
    return calcularTurnosDisponibles(empleadorId, token);
  }

  private List<IntervaloHorario> calcularTurnosDisponibles(Long empleadorId, String token) {
    return calcularTurnosPorDia(empleadorId, token)
      .values()
      .stream()
      .flatMap(List::stream)
      .toList();
  }

  private Map<LocalDate, List<IntervaloHorario>> calcularTurnosPorDia(
    Long empleadorId,
    String token
  ) {
    Instant ahora = reloj.instant();
    LocalDate primerDia = ahora.atZone(ZONA_HORARIA).toLocalDate();
    LocalDate limite = primerDia.plusDays(DIAS_DISPONIBLES);
    Instant hasta = limite.atStartOfDay(ZONA_HORARIA).toInstant();
    List<IntervaloHorario> ocupados = new ArrayList<>(
      clienteGoogleCalendar.consultarPeriodosOcupados(token, ahora, hasta)
    );
    for (Entrevista agendada : repositorioEntrevista.buscarAgendadas(empleadorId, ahora, hasta)) {
      ocupados.add(new IntervaloHorario(agendada.getInicio(), agendada.getFin()));
    }

    Map<LocalDate, List<IntervaloHorario>> disponibles = new LinkedHashMap<>();
    for (LocalDate dia = primerDia; dia.isBefore(limite); dia = dia.plusDays(1)) {
      if (esDiaLaboral(dia)) {
        List<IntervaloHorario> turnosDelDia = new ArrayList<>();
        disponibles.put(dia, turnosDelDia);
        for (IntervaloHorario turno : generarTurnosDelDia(dia)) {
          if (turno.getInicio().isAfter(ahora) && estaLibre(turno, ocupados)) {
            turnosDelDia.add(turno);
          }
        }
      }
    }
    return disponibles;
  }

  private boolean esDiaLaboral(LocalDate dia) {
    return dia.getDayOfWeek() != DayOfWeek.SATURDAY && dia.getDayOfWeek() != DayOfWeek.SUNDAY;
  }

  private List<IntervaloHorario> generarTurnosDelDia(LocalDate dia) {
    List<IntervaloHorario> turnos = new ArrayList<>();
    for (LocalTime hora = HORA_INICIO; hora.isBefore(HORA_FIN); hora = hora.plus(DURACION_TURNO)) {
      Instant inicio = dia.atTime(hora).atZone(ZONA_HORARIA).toInstant();
      turnos.add(new IntervaloHorario(inicio, inicio.plus(DURACION_TURNO)));
    }
    return turnos;
  }

  private boolean estaLibre(IntervaloHorario turno, List<IntervaloHorario> ocupados) {
    for (IntervaloHorario ocupado : ocupados) {
      if (turno.seSuperponeCon(ocupado)) {
        return false;
      }
    }
    return true;
  }
}
