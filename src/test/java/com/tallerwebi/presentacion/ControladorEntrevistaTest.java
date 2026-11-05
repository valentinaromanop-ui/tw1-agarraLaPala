package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.AccesoOfertaDenegadoException;
import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.ServicioEntrevista;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
}
