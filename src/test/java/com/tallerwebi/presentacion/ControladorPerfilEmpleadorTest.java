package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioConexionGoogle;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPerfilEmpleadorTest {

  private ControladorPerfilEmpleador controladorPerfilEmpleador;
  private ServicioOfertaEmpleador servicioOfertaEmpleadorMock;
  private ServicioConexionGoogle servicioConexionGoogleMock;
  private HttpServletRequest requestMock;
  private HttpSession sessionMock;

  @BeforeEach
  public void init() {
    servicioOfertaEmpleadorMock = mock(ServicioOfertaEmpleador.class);
    servicioConexionGoogleMock = mock(ServicioConexionGoogle.class);
    requestMock = mock(HttpServletRequest.class);
    sessionMock = mock(HttpSession.class);
    controladorPerfilEmpleador =
      new ControladorPerfilEmpleador(servicioOfertaEmpleadorMock, servicioConexionGoogleMock);
  }

  @Test
  public void perfilDeEmpleadorDeberiaMostrarLaConfiguracionYConexionDeGoogle() {
    // preparacion
    when(requestMock.getSession(false)).thenReturn(sessionMock);
    when(sessionMock.getAttribute("USUARIO_ID")).thenReturn(7L);
    when(sessionMock.getAttribute("ROL")).thenReturn("EMPLEADOR");
    when(servicioOfertaEmpleadorMock.obtenerEmpleador(7L)).thenReturn(new Usuario());
    when(servicioConexionGoogleMock.estaDisponible()).thenReturn(true);
    when(servicioConexionGoogleMock.conectado(7L)).thenReturn(true);

    // ejecucion
    ModelAndView modelAndView = controladorPerfilEmpleador.mostrarPerfil(requestMock);

    // validacion
    assertThat(modelAndView.getViewName(), equalTo("perfil-empleador"));
    assertThat(modelAndView.getModel().get("googleDisponible"), equalTo(true));
    assertThat(modelAndView.getModel().get("googleConectado"), equalTo(true));
  }
}
