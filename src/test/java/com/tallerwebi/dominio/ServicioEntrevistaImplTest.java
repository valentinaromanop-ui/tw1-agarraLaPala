package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
  private ServicioPostulantesOferta servicioPostulantesOfertaMock;
  private ApplicationEventPublisher eventosMock;

  @BeforeEach
  public void init() {
    servicioConexionGoogleMock = mock(ServicioConexionGoogle.class);
    clienteGoogleCalendarMock = mock(ClienteGoogleCalendar.class);
    repositorioEntrevistaMock = mock(RepositorioEntrevista.class);
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
      captor.getValue().getMensaje().contains("http://localhost:8080/spring/entrevistas/30"),
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
