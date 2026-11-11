package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ResumenConversacion;
import com.tallerwebi.dominio.ServicioMensaje;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ControladorChatApiTest {

  private ServicioMensaje servicio;
  private ControladorChatApi controlador;
  private HttpServletRequest request;
  private HttpSession session;

  @BeforeEach
  public void init() {
    servicio = mock(ServicioMensaje.class);
    controlador = new ControladorChatApi(servicio);
    request = mock(HttpServletRequest.class);
    session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);
  }

  @Test
  public void listadoDevuelveSoloLoQueEntregaElServicioParaElUsuario() {
    when(session.getAttribute("USUARIO_ID")).thenReturn(7L);
    when(servicio.listarConversaciones(7L))
      .thenReturn(List.of(new ResumenConversacion(3L, "Empresa", "Puesto", "Hola", null, 2)));

    ResponseEntity<?> respuesta = controlador.conversaciones(request);

    assertThat(respuesta.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(((List<?>) respuesta.getBody()).size(), equalTo(1));
  }

  @Test
  public void endpointsSinSesionDevuelven401() {
    when(request.getSession(false)).thenReturn(null);

    assertThat(
      controlador.conversaciones(request).getStatusCode(),
      equalTo(HttpStatus.UNAUTHORIZED)
    );
    assertThat(controlador.mensajes(3L, request).getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    assertThat(
      controlador.marcarLeidos(3L, request).getStatusCode(),
      equalTo(HttpStatus.UNAUTHORIZED)
    );
  }
}
