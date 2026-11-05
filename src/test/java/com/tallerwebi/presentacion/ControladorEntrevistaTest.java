package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.AccesoOfertaDenegadoException;
import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.IntervaloHorario;
import com.tallerwebi.dominio.ServicioEntrevista;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorEntrevistaTest {

  private ServicioEntrevista servicioEntrevistaMock;
  private ControladorEntrevista controladorEntrevista;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private RedirectAttributesModelMap mensajes;

  @BeforeEach
  public void init() {
    servicioEntrevistaMock = mock(ServicioEntrevista.class);
    controladorEntrevista = new ControladorEntrevista(servicioEntrevistaMock);
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO_ID")).thenReturn(7L);
    mensajes = new RedirectAttributesModelMap();
  }

  @Test
  public void invitarDeberiaRedirigirAlListadoConLaConfirmacion() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");

    // ejecucion
    ModelAndView vista = controladorEntrevista.invitar(20L, 30L, sessionMock, mensajes);

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/ofertas/20/postulantes"));
    assertThat(mensajes.getFlashAttributes().containsKey("mensaje"), equalTo(true));
    verify(servicioEntrevistaMock).invitar(20L, 30L, 7L);
  }

  @Test
  public void deberiaMostrarElErrorDeValidacionSinConfirmarLaInvitacion() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");
    when(servicioEntrevistaMock.invitar(20L, 30L, 7L))
      .thenThrow(new IllegalArgumentException("Ya se envió una invitación."));

    // ejecucion
    ModelAndView vista = controladorEntrevista.invitar(20L, 30L, sessionMock, mensajes);

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/ofertas/20/postulantes"));
    assertThat(mensajes.getFlashAttributes().get("error"), equalTo("Ya se envió una invitación."));
    assertThat(mensajes.getFlashAttributes().containsKey("mensaje"), equalTo(false));
  }

  @Test
  public void unCandidatoNoDeberiaPoderInvitar() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");

    // ejecucion y validacion
    ResponseStatusException error = assertThrows(
      ResponseStatusException.class,
      () -> controladorEntrevista.invitar(20L, 30L, sessionMock, mensajes)
    );
    assertThat(error.getStatusCode().value(), equalTo(403));
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void noDeberiaInvitarSinSesion() {
    // ejecucion y validacion
    ResponseStatusException error = assertThrows(
      ResponseStatusException.class,
      () -> controladorEntrevista.invitar(20L, 30L, null, mensajes)
    );
    assertThat(error.getStatusCode().value(), equalTo(401));
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void elPostulanteDeberiaVerSuInvitacion() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    Entrevista entrevista = new Entrevista();
    when(servicioEntrevistaMock.obtenerInvitacion(30L, 7L)).thenReturn(entrevista);

    // ejecucion
    ModelAndView vista = controladorEntrevista.mostrarInvitacion(30L, requestMock);

    // validacion
    assertThat(vista.getViewName(), equalTo("invitacion-entrevista"));
    assertThat(vista.getModel().get("entrevista"), equalTo(entrevista));
  }

  @Test
  public void elEnlaceDeberiaExigirLogin() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(null);

    // ejecucion
    ModelAndView vista = controladorEntrevista.mostrarInvitacion(30L, requestMock);

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/login"));
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void elEmpleadorNoDeberiaUsarElEnlaceDelCandidato() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");

    // ejecucion y validacion
    ResponseStatusException error = assertThrows(
      ResponseStatusException.class,
      () -> controladorEntrevista.mostrarInvitacion(30L, requestMock)
    );
    assertThat(error.getStatusCode().value(), equalTo(403));
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void deberiaRechazarElEnlaceDeOtroCandidato() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    when(servicioEntrevistaMock.obtenerInvitacion(30L, 7L))
      .thenThrow(new AccesoOfertaDenegadoException());

    // ejecucion y validacion
    assertThrows(
      AccesoOfertaDenegadoException.class,
      () -> controladorEntrevista.mostrarInvitacion(30L, requestMock)
    );
  }

  @Test
  public void deberiaInformarSiElEnlaceNoTieneUnaInvitacion() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("POSTULANTE");
    when(servicioEntrevistaMock.obtenerInvitacion(30L, 7L))
      .thenThrow(new IllegalArgumentException("No existe la invitación."));

    // ejecucion y validacion
    ResponseStatusException error = assertThrows(
      ResponseStatusException.class,
      () -> controladorEntrevista.mostrarInvitacion(30L, requestMock)
    );
    assertThat(error.getStatusCode().value(), equalTo(404));
  }

  @Test
  public void deberiaAgruparLosTurnosPorDiaEnLaPaginaDeInvitacion() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    when(servicioEntrevistaMock.obtenerInvitacion(30L, 7L)).thenReturn(new Entrevista());
    IntervaloHorario turno = new IntervaloHorario(
      Instant.parse("2026-11-02T12:00:00Z"),
      Instant.parse("2026-11-02T12:30:00Z")
    );
    when(servicioEntrevistaMock.obtenerTurnosParaInvitacion(30L, 7L))
      .thenReturn(Map.of(LocalDate.of(2026, 11, 2), List.of(turno)));

    // ejecucion
    ModelAndView vista = controladorEntrevista.mostrarInvitacion(30L, requestMock);

    // validacion
    assertThat(
      vista.getModel().get("turnosPorDia"),
      equalTo(Map.of(LocalDate.of(2026, 11, 2), List.of(turno)))
    );
  }

  @Test
  public void confirmarDeberiaEnviarSoloElInicioElegidoAlServicio() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");

    // ejecucion
    ModelAndView vista = controladorEntrevista.confirmar(
      30L,
      "2026-11-02T12:00:00Z",
      requestMock,
      mensajes
    );

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/entrevistas/30"));
    assertThat(mensajes.getFlashAttributes().get("mensaje"), equalTo("Entrevista agendada."));
    verify(servicioEntrevistaMock).confirmar(30L, 7L, Instant.parse("2026-11-02T12:00:00Z"));
  }

  @Test
  public void confirmarSinHorarioDeberiaMostrarUnError() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");

    // ejecucion
    controladorEntrevista.confirmar(30L, null, requestMock, mensajes);

    // validacion
    assertThat(
      mensajes.getFlashAttributes().get("error"),
      equalTo("Elegí un horario para confirmar la entrevista.")
    );
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void confirmarConUnaFechaInvalidaNoDeberiaLlamarAlServicio() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");

    // ejecucion
    controladorEntrevista.confirmar(30L, "fecha-invalida", requestMock, mensajes);

    // validacion
    assertThat(
      mensajes.getFlashAttributes().get("error"),
      equalTo("El horario seleccionado no es válido.")
    );
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void unHorarioOcupadoDeberiaVolverALaInvitacionConElError() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    when(servicioEntrevistaMock.confirmar(eq(30L), eq(7L), any()))
      .thenThrow(new IllegalArgumentException("El horario no está disponible."));

    // ejecucion
    ModelAndView vista = controladorEntrevista.confirmar(
      30L,
      "2026-11-02T12:00:00Z",
      requestMock,
      mensajes
    );

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/entrevistas/30"));
    assertThat(
      mensajes.getFlashAttributes().get("error"),
      equalTo("El horario no está disponible.")
    );
    assertThat(mensajes.getFlashAttributes().containsKey("mensaje"), equalTo(false));
  }

  @Test
  public void elEmpleadorNoDeberiaPoderConfirmarPorElCandidato() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");

    // ejecucion y validacion
    ResponseStatusException error = assertThrows(
      ResponseStatusException.class,
      () -> controladorEntrevista.confirmar(30L, "2026-11-02T12:00:00Z", requestMock, mensajes)
    );
    assertThat(error.getStatusCode().value(), equalTo(403));
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void confirmarSinSesionDeberiaRedirigirAlLogin() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(null);

    // ejecucion
    ModelAndView vista = controladorEntrevista.confirmar(
      30L,
      "2026-11-02T12:00:00Z",
      requestMock,
      mensajes
    );

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/login"));
    verifyNoInteractions(servicioEntrevistaMock);
  }

  @Test
  public void unErrorDeGoogleNoDeberiaMostrarUnaConfirmacionExitosa() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    when(servicioEntrevistaMock.confirmar(eq(30L), eq(7L), any()))
      .thenThrow(new ConexionGoogleException());

    // ejecucion
    controladorEntrevista.confirmar(30L, "2026-11-02T12:00:00Z", requestMock, mensajes);

    // validacion
    assertThat(
      mensajes.getFlashAttributes().get("error"),
      equalTo("No se pudo confirmar con Google Calendar. Volvé a intentar.")
    );
    assertThat(mensajes.getFlashAttributes().containsKey("mensaje"), equalTo(false));
  }

  @Test
  public void unErrorAlListarTurnosDeberiaConservarLaInvitacionSinOfrecerHorarios() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    Entrevista entrevista = new Entrevista();
    when(servicioEntrevistaMock.obtenerInvitacion(30L, 7L)).thenReturn(entrevista);
    when(servicioEntrevistaMock.obtenerTurnosParaInvitacion(30L, 7L))
      .thenThrow(new ConexionGoogleException());

    // ejecucion
    ModelAndView vista = controladorEntrevista.mostrarInvitacion(30L, requestMock);

    // validacion
    assertThat(vista.getModel().get("entrevista"), equalTo(entrevista));
    assertThat(vista.getModel().get("turnosPorDia"), equalTo(Map.of()));
    assertThat(
      vista.getModel().get("error"),
      equalTo("No se pudo consultar Google Calendar. Volvé a intentar.")
    );
  }
}
