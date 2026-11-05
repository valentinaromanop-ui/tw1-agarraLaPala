package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service("servicioEntrevista")
public class ServicioEntrevistaImpl implements ServicioEntrevista {

  private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
  private static final LocalTime HORA_FIN = LocalTime.of(18, 0);
  private static final Duration DURACION_TURNO = Duration.ofMinutes(30);
  private static final int DIAS_DISPONIBLES = 14;

  private final ServicioConexionGoogle servicioConexionGoogle;
  private final ClienteGoogleCalendar clienteGoogleCalendar;
  private final Clock reloj;
  private final RepositorioEntrevista repositorioEntrevista;
  private final ServicioPostulantesOferta servicioPostulantesOferta;
  private final ApplicationEventPublisher eventos;
  private final String urlBase;

  @Autowired
  public ServicioEntrevistaImpl(
    ServicioConexionGoogle servicioConexionGoogle,
    ClienteGoogleCalendar clienteGoogleCalendar,
    RepositorioEntrevista repositorioEntrevista,
    ServicioPostulantesOferta servicioPostulantesOferta,
    ApplicationEventPublisher eventos,
    @Value("${app.url:http://localhost:8080/spring}") String urlBase
  ) {
    this(
      servicioConexionGoogle,
      clienteGoogleCalendar,
      repositorioEntrevista,
      servicioPostulantesOferta,
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
    ApplicationEventPublisher eventos,
    String urlBase,
    Clock reloj
  ) {
    this.servicioConexionGoogle = servicioConexionGoogle;
    this.clienteGoogleCalendar = clienteGoogleCalendar;
    this.repositorioEntrevista = repositorioEntrevista;
    this.servicioPostulantesOferta = servicioPostulantesOferta;
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
  public List<IntervaloHorario> obtenerTurnosDisponibles(Long empleadorId) {
    Instant ahora = reloj.instant();
    LocalDate primerDia = ahora.atZone(ZONA_HORARIA).toLocalDate();
    LocalDate limite = primerDia.plusDays(DIAS_DISPONIBLES);
    String token = servicioConexionGoogle.obtenerTokenAcceso(empleadorId);
    List<IntervaloHorario> ocupados = clienteGoogleCalendar.consultarPeriodosOcupados(
      token,
      ahora,
      limite.atStartOfDay(ZONA_HORARIA).toInstant()
    );

    List<IntervaloHorario> disponibles = new ArrayList<>();
    for (LocalDate dia = primerDia; dia.isBefore(limite); dia = dia.plusDays(1)) {
      if (esDiaLaboral(dia)) {
        for (IntervaloHorario turno : generarTurnosDelDia(dia)) {
          if (turno.getInicio().isAfter(ahora) && estaLibre(turno, ocupados)) {
            disponibles.add(turno);
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
