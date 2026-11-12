package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

public class ServicioEntrevistaImplTest {

  private ServicioEntrevista servicioEntrevista;
  private ServicioConexionGoogle servicioConexionGoogleMock;
  private ClienteGoogleCalendar clienteGoogleCalendarMock;
  private RepositorioEntrevista repositorioEntrevistaMock;
  private RepositorioUsuario repositorioUsuarioMock;
  private ServicioPostulantesOferta servicioPostulantesOfertaMock;
  private ApplicationEventPublisher eventosMock;

  @BeforeEach
  public void init() {
    servicioConexionGoogleMock = mock(ServicioConexionGoogle.class);
    clienteGoogleCalendarMock = mock(ClienteGoogleCalendar.class);
    repositorioEntrevistaMock = mock(RepositorioEntrevista.class);
    repositorioUsuarioMock = mock(RepositorioUsuario.class);
    servicioPostulantesOfertaMock = mock(ServicioPostulantesOferta.class);
    eventosMock = mock(ApplicationEventPublisher.class);
    when(servicioConexionGoogleMock.obtenerTokenAcceso(7L)).thenReturn("acceso");
    when(clienteGoogleCalendarMock.consultarPeriodosOcupados(anyString(), any(), any()))
      .thenReturn(List.of());
    dadoQueAhoraEs("2026-11-02T08:00");
  }

  @Test
  public void deberiaGenerarTurnosLaboralesDeTreintaMinutosDuranteCatorceDias() {
    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion: dos semanas contienen diez dias laborales de dieciocho turnos.
    assertThat(turnos.size(), equalTo(180));
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-02T09:00")));
    assertThat(turnos.getLast().getInicio(), equalTo(hora("2026-11-13T17:30")));
    assertThat(turnos.getLast().getFin(), equalTo(hora("2026-11-13T18:00")));
    assertThat(
      Duration.between(turnos.getFirst().getInicio(), turnos.getFirst().getFin()),
      equalTo(Duration.ofMinutes(30))
    );
    verify(clienteGoogleCalendarMock)
      .consultarPeriodosOcupados("acceso", hora("2026-11-02T08:00"), hora("2026-11-16T00:00"));
  }

  @Test
  public void deberiaExcluirLosFinesDeSemana() {
    // preparacion
    dadoQueAhoraEs("2026-11-07T08:00");

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-09T09:00")));
    for (IntervaloHorario turno : turnos) {
      int dia = turno
        .getInicio()
        .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
        .getDayOfWeek()
        .getValue();
      assertThat(dia <= 5, equalTo(true));
    }
  }

  @Test
  public void deberiaExcluirLosHorariosPasadosSinDesplazarLosBloques() {
    // preparacion
    dadoQueAhoraEs("2026-11-02T10:10");

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-02T10:30")));
  }

  @Test
  public void deberiaExcluirCadaBloqueQueSeSuperpongaConUnPeriodoOcupado() {
    // preparacion
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-02T09:15"), hora("2026-11-02T09:45"))
    );

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.size(), equalTo(178));
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-02T10:00")));
  }

  @Test
  public void deberiaPermitirTurnosQueTerminanOEmpiezanJustoAlLimiteDelOcupado() {
    // preparacion
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-02T09:30"), hora("2026-11-02T10:00"))
    );

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.get(0).getInicio(), equalTo(hora("2026-11-02T09:00")));
    assertThat(turnos.get(1).getInicio(), equalTo(hora("2026-11-02T10:00")));
    assertThat(turnos.size(), equalTo(179));
  }

  @Test
  public void deberiaExcluirLosEventosQueOcupanUnDiaCompleto() {
    // preparacion
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-02T00:00"), hora("2026-11-03T00:00"))
    );

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.size(), equalTo(162));
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-03T09:00")));
  }

  @Test
  public void deberiaCalcularLasFechasSegunBuenosAiresAunqueElServidorUseUtc() {
    // preparacion: en UTC ya es martes; en Buenos Aires todavia es lunes.
    Clock relojUtc = Clock.fixed(Instant.parse("2026-11-03T00:30:00Z"), ZoneOffset.UTC);
    servicioEntrevista =
      new ServicioEntrevistaImpl(
        servicioConexionGoogleMock,
        clienteGoogleCalendarMock,
        repositorioEntrevistaMock,
        servicioPostulantesOfertaMock,
        repositorioUsuarioMock,
        eventosMock,
        "http://localhost:8080/spring",
        relojUtc
      );

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-03T09:00")));
    verify(clienteGoogleCalendarMock)
      .consultarPeriodosOcupados(
        "acceso",
        Instant.parse("2026-11-03T00:30:00Z"),
        hora("2026-11-16T00:00")
      );
  }

  @Test
  public void deberiaDevolverUnaListaVaciaSiTodoElPeriodoEstaOcupado() {
    // preparacion
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-02T00:00"), hora("2026-11-16T00:00"))
    );

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.isEmpty(), equalTo(true));
  }

  @Test
  public void deberiaPropagarElErrorDeGoogleSinOfrecerHorarios() {
    // preparacion
    when(clienteGoogleCalendarMock.consultarPeriodosOcupados(anyString(), any(), any()))
      .thenThrow(new ConexionGoogleException());

    // ejecucion y validacion
    assertThrows(
      ConexionGoogleException.class,
      () -> servicioEntrevista.obtenerTurnosDisponibles(7L)
    );
  }

  @Test
  public void noDeberiaConsultarLaAgendaSiNoPuedeObtenerLaAutorizacion() {
    // preparacion
    when(servicioConexionGoogleMock.obtenerTokenAcceso(7L))
      .thenThrow(new ConexionGoogleException());

    // ejecucion y validacion
    assertThrows(
      ConexionGoogleException.class,
      () -> servicioEntrevista.obtenerTurnosDisponibles(7L)
    );
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  @Test
  public void invitarDeberiaGuardarLaEntrevistaYPublicarElCorreo() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacionParaInvitar();
    when(servicioConexionGoogleMock.conectado(7L)).thenReturn(true);

    // ejecucion
    Entrevista entrevista = servicioEntrevista.invitar(20L, 30L, 7L);

    // validacion
    verify(repositorioEntrevistaMock).guardar(entrevista);
    assertThat(entrevista.getPostulacion(), equalTo(postulacion));
    assertThat(entrevista.getEstado(), equalTo(EstadoEntrevista.INVITADA));
    assertThat(entrevista.getFechaInvitacion(), equalTo(LocalDateTime.parse("2026-11-02T08:00")));
    ArgumentCaptor<Correo> captor = ArgumentCaptor.forClass(Correo.class);
    verify(eventosMock).publishEvent(captor.capture());
    assertThat(captor.getValue().getDestinatarios(), equalTo(List.of("candidato@test.com")));
    assertThat(
      captor.getValue().getMensaje().contains("http://localhost:8080/entrevistas/30"),
      equalTo(true)
    );
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  @Test
  public void noDeberiaInvitarUnaPostulacionRechazada() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacionParaInvitar();
    postulacion.setEstado(EstadoPostulacion.RECHAZADA);

    // ejecucion y validacion
    assertThrows(IllegalArgumentException.class, () -> servicioEntrevista.invitar(20L, 30L, 7L));
    verifyNoInteractions(repositorioEntrevistaMock, eventosMock);
  }

  @Test
  public void noDeberiaDuplicarUnaInvitacionNiVolverAEnviarElCorreo() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacionParaInvitar();
    when(repositorioEntrevistaMock.buscarPorPostulacionPorId(30L))
      .thenReturn(new Entrevista(postulacion, LocalDateTime.now()));

    // ejecucion y validacion
    assertThrows(IllegalArgumentException.class, () -> servicioEntrevista.invitar(20L, 30L, 7L));
    verify(repositorioEntrevistaMock, never()).guardar(any());
    verifyNoInteractions(eventosMock);
  }

  @Test
  public void noDeberiaInvitarSinCalendarioConectado() {
    // preparacion
    dadoQueExisteUnaPostulacionParaInvitar();
    when(servicioConexionGoogleMock.conectado(7L)).thenReturn(false);

    // ejecucion y validacion
    assertThrows(IllegalArgumentException.class, () -> servicioEntrevista.invitar(20L, 30L, 7L));
    verify(repositorioEntrevistaMock, never()).guardar(any());
    verifyNoInteractions(eventosMock);
  }

  @Test
  public void noDeberiaInvitarSiLaOfertaNoPerteneceAlEmpleador() {
    // preparacion
    when(servicioPostulantesOfertaMock.buscarPostulacionPropia(20L, 30L, 7L))
      .thenThrow(new AccesoOfertaDenegadoException());

    // ejecucion y validacion
    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicioEntrevista.invitar(20L, 30L, 7L)
    );
    verifyNoInteractions(repositorioEntrevistaMock, eventosMock);
  }

  @Test
  public void noDeberiaPublicarElCorreoSiFallaElGuardado() {
    // preparacion
    dadoQueExisteUnaPostulacionParaInvitar();
    when(servicioConexionGoogleMock.conectado(7L)).thenReturn(true);
    doThrow(new IllegalStateException("No se pudo guardar"))
      .when(repositorioEntrevistaMock)
      .guardar(any());

    // ejecucion y validacion
    assertThrows(IllegalStateException.class, () -> servicioEntrevista.invitar(20L, 30L, 7L));
    verifyNoInteractions(eventosMock);
  }

  @Test
  public void deberiaPermitirConsultarLaInvitacionAlCandidatoCorrespondiente() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacionParaInvitar();
    Entrevista entrevista = new Entrevista(postulacion, LocalDateTime.now());
    when(repositorioEntrevistaMock.buscarPorPostulacionPorId(30L)).thenReturn(entrevista);

    // ejecucion
    Entrevista obtenida = servicioEntrevista.obtenerInvitacion(30L, 12L);

    // validacion
    assertThat(obtenida, equalTo(entrevista));
  }

  @Test
  public void noDeberiaMostrarLaInvitacionAOtroCandidato() {
    // preparacion
    Postulacion postulacion = dadoQueExisteUnaPostulacionParaInvitar();
    when(repositorioEntrevistaMock.buscarPorPostulacionPorId(30L))
      .thenReturn(new Entrevista(postulacion, LocalDateTime.now()));

    // ejecucion y validacion
    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicioEntrevista.obtenerInvitacion(30L, 99L)
    );
  }

  @Test
  public void deberiaInformarCuandoLaInvitacionNoExiste() {
    // ejecucion y validacion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.obtenerInvitacion(30L, 12L)
    );
  }

  @Test
  public void confirmarDeberiaGuardarElHorarioYElEventoConMeet() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    when(
      clienteGoogleCalendarMock.crearEventoConMeet(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      )
    )
      .thenReturn(
        new EventoGoogleCalendar("evento-google", "https://meet.google.com/abc-defg-hij")
      );

    // ejecucion
    Entrevista resultado = servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T09:00"));

    // validacion
    assertThat(resultado, equalTo(entrevista));
    assertThat(resultado.getEstado(), equalTo(EstadoEntrevista.AGENDADA));
    assertThat(resultado.getInicio(), equalTo(hora("2026-11-02T09:00")));
    assertThat(resultado.getFin(), equalTo(hora("2026-11-02T09:30")));
    assertThat(resultado.getGoogleEventId(), equalTo("evento-google"));
    assertThat(resultado.getUrlMeet(), equalTo("https://meet.google.com/abc-defg-hij"));
    verify(repositorioUsuarioMock).buscarPorIdParaActualizar(7L);
    ArgumentCaptor<IntervaloHorario> horario = ArgumentCaptor.forClass(IntervaloHorario.class);
    verify(clienteGoogleCalendarMock)
      .crearEventoConMeet(
        eq("acceso"),
        anyString(),
        eq("Entrevista: Desarrollador Java"),
        eq("candidato@test.com"),
        horario.capture()
      );
    assertThat(horario.getValue().getFin(), equalTo(hora("2026-11-02T09:30")));
  }

  @Test
  public void unaConfirmacionRepetidaNoDeberiaCrearOtroEvento() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    when(
      clienteGoogleCalendarMock.crearEventoConMeet(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      )
    )
      .thenReturn(
        new EventoGoogleCalendar("evento-google", "https://meet.google.com/abc-defg-hij")
      );
    servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T09:00"));

    // ejecucion
    servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T10:00"));

    // validacion
    assertThat(entrevista.getInicio(), equalTo(hora("2026-11-02T09:00")));
    verify(clienteGoogleCalendarMock, times(1))
      .crearEventoConMeet(anyString(), anyString(), anyString(), anyString(), any());
  }

  @Test
  public void noDeberiaConfirmarElHorarioQueGoogleMarcaOcupado() {
    // preparacion
    dadoQueExisteUnaInvitacion();
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-02T09:00"), hora("2026-11-02T10:00"))
    );

    // ejecucion y validacion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T09:00"))
    );
    verify(clienteGoogleCalendarMock, never())
      .crearEventoConMeet(anyString(), anyString(), anyString(), anyString(), any());
  }

  @Test
  public void noDeberiaConfirmarUnHorarioFueraDeLaFranjaLaboral() {
    // preparacion
    dadoQueExisteUnaInvitacion();

    // ejecucion y validacion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T18:00"))
    );
    verify(clienteGoogleCalendarMock, never())
      .crearEventoConMeet(anyString(), anyString(), anyString(), anyString(), any());
  }

  @Test
  public void noDeberiaConfirmarUnHorarioQueNoRespetaLosBloquesDeTreintaMinutos() {
    // preparacion
    dadoQueExisteUnaInvitacion();

    // ejecucion y validacion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T09:15"))
    );
  }

  @Test
  public void noDeberiaConfirmarUnaFechaFueraDeLosCatorceDias() {
    // preparacion
    dadoQueExisteUnaInvitacion();

    // ejecucion y validacion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-16T09:00"))
    );
  }

  @Test
  public void noDeberiaConfirmarLaInvitacionDeOtroPostulante() {
    // preparacion
    dadoQueExisteUnaInvitacion();

    // ejecucion y validacion
    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicioEntrevista.confirmar(30L, 99L, hora("2026-11-02T09:00"))
    );
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  @Test
  public void noDeberiaConfirmarUnaPostulacionQueFueRechazadaDespuesDeInvitar() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    entrevista.getPostulacion().setEstado(EstadoPostulacion.RECHAZADA);

    // ejecucion y validacion
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T09:00"))
    );
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  @Test
  public void unErrorDeGoogleNoDeberiaMarcarLaEntrevistaComoAgendada() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    when(
      clienteGoogleCalendarMock.crearEventoConMeet(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any()
      )
    )
      .thenThrow(new ConexionGoogleException());

    // ejecucion y validacion
    assertThrows(
      ConexionGoogleException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-02T09:00"))
    );
    assertThat(entrevista.getEstado(), equalTo(EstadoEntrevista.INVITADA));
    assertThat(entrevista.getGoogleEventId(), equalTo(null));
  }

  @Test
  public void losTurnosDeberianExcluirLasEntrevistasYaGuardadasAunqueGoogleNoLasDevuelva() {
    // preparacion
    Entrevista agendada = dadoQueExisteUnaInvitacion();
    agendada.agendar(
      new IntervaloHorario(hora("2026-11-02T09:00"), hora("2026-11-02T09:30")),
      new EventoGoogleCalendar("anterior", null)
    );
    when(repositorioEntrevistaMock.buscarAgendadas(eq(7L), any(), any()))
      .thenReturn(List.of(agendada));

    // ejecucion
    List<IntervaloHorario> turnos = servicioEntrevista.obtenerTurnosDisponibles(7L);

    // validacion
    assertThat(turnos.getFirst().getInicio(), equalTo(hora("2026-11-02T09:30")));
    assertThat(turnos.size(), equalTo(179));
  }

  @Test
  public void deberiaConservarLaCompatibilidadConInvitacionesSinEmpleadorGuardado() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    entrevista.setEmpleadorId(null);
    Vacante vacante = new Vacante();
    vacante.setEmpleadorId(7L);
    when(servicioPostulantesOfertaMock.obtenerVacanteDePostulacion(entrevista.getPostulacion()))
      .thenReturn(vacante);

    // ejecucion
    var turnosPorDia = servicioEntrevista.obtenerTurnosParaInvitacion(30L, 12L);

    // validacion
    verify(servicioConexionGoogleMock).obtenerTokenAcceso(7L);
    assertThat(turnosPorDia.size(), equalTo(10));
    assertThat(turnosPorDia.values().stream().mapToInt(List::size).sum(), equalTo(180));
  }

  @Test
  public void deberiaMostrarUnDiaAunqueTodosSusTurnosEstenOcupados() {
    // preparacion
    dadoQueExisteUnaInvitacion();
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-02T09:00"), hora("2026-11-02T18:00"))
    );

    // ejecucion
    var turnosPorDia = servicioEntrevista.obtenerTurnosParaInvitacion(30L, 12L);

    // validacion
    assertThat(turnosPorDia.get(LocalDate.of(2026, 11, 2)), equalTo(List.of()));
    assertThat(turnosPorDia.get(LocalDate.of(2026, 11, 3)).size(), equalTo(18));
  }

  @Test
  public void deberiaRecuperarElEnlaceDeMeetSinCrearOtroEvento() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    entrevista.agendar(
      new IntervaloHorario(hora("2026-11-02T09:00"), hora("2026-11-02T09:30")),
      new EventoGoogleCalendar("evento-google", null)
    );
    when(clienteGoogleCalendarMock.obtenerEnlaceMeet("acceso", "evento-google"))
      .thenReturn("https://meet.google.com/abc-defg-hij");

    // ejecucion
    servicioEntrevista.actualizarEnlaceMeet(30L, 12L);

    // validacion
    assertThat(entrevista.getUrlMeet(), equalTo("https://meet.google.com/abc-defg-hij"));
    verify(clienteGoogleCalendarMock, never())
      .crearEventoConMeet(anyString(), anyString(), anyString(), anyString(), any());
  }

  @Test
  public void noDeberiaConsultarGoogleSiElEnlaceDeMeetYaEstaGuardado() {
    // preparacion
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    entrevista.agendar(
      new IntervaloHorario(hora("2026-11-02T09:00"), hora("2026-11-02T09:30")),
      new EventoGoogleCalendar("evento-google", "https://meet.google.com/abc-defg-hij")
    );

    // ejecucion
    servicioEntrevista.actualizarEnlaceMeet(30L, 12L);

    // validacion
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  @Test
  public void modificarDeberiaConservarElEventoYElMeet() {
    Entrevista entrevista = dadoQueExisteUnaEntrevistaAgendada();

    servicioEntrevista.modificar(20L, 30L, 7L, hora("2026-11-03T10:00"));

    assertThat(entrevista.getInicio(), equalTo(hora("2026-11-03T10:00")));
    assertThat(entrevista.getFin(), equalTo(hora("2026-11-03T10:30")));
    assertThat(entrevista.getGoogleEventId(), equalTo("evento-google"));
    assertThat(entrevista.getUrlMeet(), equalTo("https://meet.google.com/abc-defg-hij"));
    verify(clienteGoogleCalendarMock).modificarHorario(eq("acceso"), eq("evento-google"), any());
  }

  @Test
  public void modificarUnHorarioOcupadoDeberiaConservarElAnterior() {
    Entrevista entrevista = dadoQueExisteUnaEntrevistaAgendada();
    dadoQueGoogleDevuelveOcupados(
      new IntervaloHorario(hora("2026-11-03T10:00"), hora("2026-11-03T10:30"))
    );

    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.modificar(20L, 30L, 7L, hora("2026-11-03T10:00"))
    );

    assertThat(entrevista.getInicio(), equalTo(hora("2026-11-02T09:00")));
    verify(clienteGoogleCalendarMock, never()).modificarHorario(anyString(), anyString(), any());
  }

  @Test
  public void cancelarDosVecesDeberiaEliminarElEventoUnaSolaVez() {
    Entrevista entrevista = dadoQueExisteUnaEntrevistaAgendada();

    servicioEntrevista.cancelar(20L, 30L, 7L);
    servicioEntrevista.cancelar(20L, 30L, 7L);

    assertThat(entrevista.getEstado(), equalTo(EstadoEntrevista.CANCELADA));
    verify(clienteGoogleCalendarMock, times(1)).eliminarEvento("acceso", "evento-google");
  }

  @Test
  public void siGoogleFallaNoDeberiaCancelarNiCambiarElHorario() {
    Entrevista entrevista = dadoQueExisteUnaEntrevistaAgendada();
    doThrow(new ConexionGoogleException())
      .when(clienteGoogleCalendarMock)
      .eliminarEvento("acceso", "evento-google");
    doThrow(new ConexionGoogleException())
      .when(clienteGoogleCalendarMock)
      .modificarHorario(anyString(), anyString(), any());

    assertThrows(ConexionGoogleException.class, () -> servicioEntrevista.cancelar(20L, 30L, 7L));
    assertThrows(
      ConexionGoogleException.class,
      () -> servicioEntrevista.modificar(20L, 30L, 7L, hora("2026-11-03T10:00"))
    );

    assertThat(entrevista.getEstado(), equalTo(EstadoEntrevista.AGENDADA));
    assertThat(entrevista.getInicio(), equalTo(hora("2026-11-02T09:00")));
  }

  @Test
  public void otroEmpleadorNoDeberiaGestionarLaEntrevista() {
    when(servicioPostulantesOfertaMock.buscarPostulacionPropia(20L, 30L, 8L))
      .thenThrow(new AccesoOfertaDenegadoException());

    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicioEntrevista.obtenerParaEmpleador(20L, 30L, 8L)
    );
    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicioEntrevista.modificar(20L, 30L, 8L, hora("2026-11-03T10:00"))
    );
    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> servicioEntrevista.cancelar(20L, 30L, 8L)
    );
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  @Test
  public void unaCanceladaNoDeberiaVolverAConfirmarse() {
    Entrevista entrevista = dadoQueExisteUnaEntrevistaAgendada();
    entrevista.cancelar();

    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.confirmar(30L, 12L, hora("2026-11-03T10:00"))
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.obtenerTurnosParaInvitacion(30L, 12L)
    );
    assertThrows(
      IllegalArgumentException.class,
      () -> servicioEntrevista.modificar(20L, 30L, 7L, hora("2026-11-03T10:00"))
    );
    verifyNoInteractions(clienteGoogleCalendarMock);
  }

  private Entrevista dadoQueExisteUnaEntrevistaAgendada() {
    Entrevista entrevista = dadoQueExisteUnaInvitacion();
    entrevista.agendar(
      new IntervaloHorario(hora("2026-11-02T09:00"), hora("2026-11-02T09:30")),
      new EventoGoogleCalendar("evento-google", "https://meet.google.com/abc-defg-hij")
    );
    return entrevista;
  }

  private Entrevista dadoQueExisteUnaInvitacion() {
    Postulacion postulacion = dadoQueExisteUnaPostulacionParaInvitar();
    Entrevista entrevista = new Entrevista(postulacion, LocalDateTime.parse("2026-11-02T08:00"));
    entrevista.setEmpleadorId(7L);
    when(repositorioEntrevistaMock.buscarPorPostulacionPorId(30L)).thenReturn(entrevista);
    when(repositorioEntrevistaMock.buscarPorPostulacionParaActualizar(30L)).thenReturn(entrevista);
    return entrevista;
  }

  private Postulacion dadoQueExisteUnaPostulacionParaInvitar() {
    Usuario candidato = new Usuario();
    candidato.setId(12L);
    candidato.setEmail("candidato@test.com");
    Postulacion postulacion = new Postulacion();
    postulacion.setId(30L);
    postulacion.setUsuario(candidato);
    postulacion.setTitulo("Desarrollador Java");
    when(servicioPostulantesOfertaMock.buscarPostulacionPropia(20L, 30L, 7L))
      .thenReturn(postulacion);
    return postulacion;
  }

  private void dadoQueAhoraEs(String fechaYHora) {
    Clock reloj = Clock.fixed(hora(fechaYHora), ZoneOffset.UTC);
    servicioEntrevista =
      new ServicioEntrevistaImpl(
        servicioConexionGoogleMock,
        clienteGoogleCalendarMock,
        repositorioEntrevistaMock,
        servicioPostulantesOfertaMock,
        repositorioUsuarioMock,
        eventosMock,
        "http://localhost:8080/spring",
        reloj
      );
  }

  private void dadoQueGoogleDevuelveOcupados(IntervaloHorario ocupado) {
    when(clienteGoogleCalendarMock.consultarPeriodosOcupados(anyString(), any(), any()))
      .thenReturn(List.of(ocupado));
  }

  private Instant hora(String fechaYHora) {
    return LocalDateTime
      .parse(fechaYHora)
      .atZone(ZoneId.of("America/Argentina/Buenos_Aires"))
      .toInstant();
  }
}
