package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tallerwebi.dominio.ServicioConexionGoogle;
import com.tallerwebi.dominio.excepcion.ConexionGoogleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorGoogleCalendarTest {

  private ServicioConexionGoogle servicioConexionGoogleMock;
  private ControladorGoogleCalendar controladorGoogleCalendar;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;
  private RedirectAttributesModelMap atributos;

  @BeforeEach
  public void init() {
    servicioConexionGoogleMock = mock(ServicioConexionGoogle.class);
    controladorGoogleCalendar = new ControladorGoogleCalendar(servicioConexionGoogleMock);
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    when(sessionMock.getAttribute("USUARIO_ID")).thenReturn(7L);
    when(sessionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    atributos = new RedirectAttributesModelMap();
  }

  @Test
  public void deberiaConectarConUnaAutorizacionValida() {
    // preparacion
    dadoQueExisteUnaAutorizacionPendiente();
    String state = "estado";

    // ejecucion
    controladorGoogleCalendar.callback(requestMock, state, "codigo", null, atributos);

    // validacion
    assertThat(
      atributos.getFlashAttributes().get("googleMensaje"),
      equalTo("Google Calendar conectado.")
    );
    verify(servicioConexionGoogleMock).conectar(7L, "codigo");
  }

  @Test
  public void deberiaRechazarUnaAutorizacionYaUtilizada() {
    // preparacion
    when(sessionMock.getAttribute("GOOGLE_CALENDAR_STATE"))
      .thenReturn(
        new ControladorGoogleCalendar.EstadoOAuth("estado", 7L, Instant.now().plusSeconds(60))
      )
      .thenReturn(null);
    controladorGoogleCalendar.callback(requestMock, "estado", "codigo", null, atributos);

    // ejecucion
    controladorGoogleCalendar.callback(requestMock, "estado", "codigo", null, atributos);

    // validacion
    verify(servicioConexionGoogleMock, times(1)).conectar(7L, "codigo");
    verify(sessionMock, times(2)).removeAttribute("GOOGLE_CALENDAR_STATE");
    assertThat(atributos.getFlashAttributes().containsKey("googleError"), equalTo(true));
  }

  @Test
  public void deberiaRedirigirAGoogleAlIniciarLaConexion() {
    // preparacion
    when(servicioConexionGoogleMock.iniciar(eq(7L), anyString()))
      .thenReturn("https://accounts.google.com/autorizacion");

    // ejecucion
    ModelAndView vista = controladorGoogleCalendar.conectar(requestMock, atributos);

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:https://accounts.google.com/autorizacion"));
    verify(sessionMock)
      .setAttribute(eq("GOOGLE_CALENDAR_STATE"), any(ControladorGoogleCalendar.EstadoOAuth.class));
  }

  @Test
  public void rechazaStateDistinto() {
    // preparacion
    dadoQueExisteUnaAutorizacionPendiente();
    // ejecucion
    controladorGoogleCalendar.callback(requestMock, "otro", "codigo", null, atributos);

    // validacion
    verify(servicioConexionGoogleMock, never()).conectar(anyLong(), anyString());
    assertThat(atributos.getFlashAttributes().containsKey("googleError"), equalTo(true));
  }

  @Test
  public void rechazaStateVencido() {
    // preparacion
    when(sessionMock.getAttribute("GOOGLE_CALENDAR_STATE"))
      .thenReturn(
        new ControladorGoogleCalendar.EstadoOAuth("estado", 7L, Instant.now().minusSeconds(1))
      );
    // ejecucion
    controladorGoogleCalendar.callback(requestMock, "estado", "codigo", null, atributos);
    // validacion
    verifyNoInteractions(servicioConexionGoogleMock);
  }

  @Test
  public void rechazaCambioDeUsuarioEnLaMismaSesion() {
    // preparacion
    dadoQueExisteUnaAutorizacionPendiente();
    String state = "estado";
    when(sessionMock.getAttribute("USUARIO_ID")).thenReturn(8L);
    // ejecucion
    controladorGoogleCalendar.callback(requestMock, state, "codigo", null, atributos);

    // validacion
    verify(servicioConexionGoogleMock, never()).conectar(anyLong(), anyString());
  }

  @Test
  public void rechazaCallbackDeOtraSesion() {
    // preparacion
    dadoQueExisteUnaAutorizacionPendiente();
    String state = "estado";
    HttpSession otraSesionMock = mock(HttpSession.class);
    when(otraSesionMock.getAttribute("USUARIO_ID")).thenReturn(7L);
    when(otraSesionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");
    when(requestMock.getSession(false)).thenReturn(otraSesionMock);
    // ejecucion
    controladorGoogleCalendar.callback(requestMock, state, "codigo", null, atributos);

    // validacion
    verify(servicioConexionGoogleMock, never()).conectar(anyLong(), anyString());
  }

  @Test
  public void manejaAutorizacionDenegadaSinIntercambiarCodigo() {
    // preparacion
    dadoQueExisteUnaAutorizacionPendiente();
    String state = "estado";
    // ejecucion
    controladorGoogleCalendar.callback(requestMock, state, null, "access_denied", atributos);

    // validacion
    verify(servicioConexionGoogleMock, never()).conectar(anyLong(), anyString());
    assertThat(atributos.getFlashAttributes().containsKey("googleError"), equalTo(true));
  }

  @Test
  public void manejaErrorDeGoogleSinExponerDatos() {
    // preparacion
    dadoQueExisteUnaAutorizacionPendiente();
    String state = "estado";
    doThrow(new ConexionGoogleException()).when(servicioConexionGoogleMock).conectar(7L, "codigo");
    // ejecucion
    ModelAndView vista = controladorGoogleCalendar.callback(
      requestMock,
      state,
      "codigo",
      null,
      atributos
    );

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/empleador/perfil"));
    assertThat(atributos.getFlashAttributes().containsKey("googleMensaje"), equalTo(false));
  }

  @Test
  public void deberiaExigirUnaSesionIniciada() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(null);
    // ejecucion y validación de la excepción
    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> controladorGoogleCalendar.conectar(requestMock, atributos)
    );

    // validacion
    assertThat(excepcion.getStatusCode().value(), equalTo(401));
    verifyNoInteractions(servicioConexionGoogleMock);
  }

  @Test
  public void deberiaRechazarAlCandidato() {
    // preparacion
    when(sessionMock.getAttribute("ROL")).thenReturn("CANDIDATO");
    // ejecucion
    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> controladorGoogleCalendar.callback(requestMock, "estado", "codigo", null, atributos)
    );

    // validacion
    assertThat(excepcion.getStatusCode().value(), equalTo(403));
    verifyNoInteractions(servicioConexionGoogleMock);
  }

  @Test
  public void muestraErrorCuandoFaltaConfiguracion() {
    // preparacion
    when(servicioConexionGoogleMock.iniciar(eq(7L), anyString()))
      .thenThrow(new ConexionGoogleException());
    // ejecucion
    ModelAndView vista = controladorGoogleCalendar.conectar(requestMock, atributos);

    // validacion
    assertThat(vista.getViewName(), equalTo("redirect:/empleador/perfil"));
    verify(sessionMock, never()).setAttribute(eq("GOOGLE_CALENDAR_STATE"), any());
    assertThat(atributos.getFlashAttributes().containsKey("googleError"), equalTo(true));
  }

  @Test
  public void callbackValidoDeberiaRedirigirAlPerfilConMensaje() throws Exception {
    // preparacion
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 7L);
    session.setAttribute("ROL", "EMPLEADOR");
    session.setAttribute(
      "GOOGLE_CALENDAR_STATE",
      new ControladorGoogleCalendar.EstadoOAuth("estado", 7L, Instant.now().plusSeconds(60))
    );
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controladorGoogleCalendar).build();

    // ejecucion
    ResultActions resultado = mockMvc.perform(
      get("/google-calendar/callback")
        .session(session)
        .param("state", "estado")
        .param("code", "codigo")
    );

    // validacion
    resultado
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/empleador/perfil"))
      .andExpect(flash().attribute("googleMensaje", "Google Calendar conectado."));
    verify(servicioConexionGoogleMock).conectar(7L, "codigo");
  }

  private void dadoQueExisteUnaAutorizacionPendiente() {
    when(sessionMock.getAttribute("GOOGLE_CALENDAR_STATE"))
      .thenReturn(
        new ControladorGoogleCalendar.EstadoOAuth("estado", 7L, Instant.now().plusSeconds(60))
      );
  }
}
