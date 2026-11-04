package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

class ControladorHomeTest {

  private ControladorHome controlador;
  private ServicioOfertaEmpleador servicioOfertaEmpleador;
  private ServicioPago servicioPago;

  @BeforeEach
  void iniciar() {
    servicioOfertaEmpleador = mock(ServicioOfertaEmpleador.class);
    servicioPago = mock(ServicioPago.class);
    controlador = new ControladorHome(servicioOfertaEmpleador, servicioPago);
    when(servicioOfertaEmpleador.listarOfertasPublicadas()).thenReturn(List.of());
  }

  @Test
  void homeDeEmpleadorRedirigeAlInicioDeEmpresas() {
    MockHttpServletRequest request = requestConRol("EMPLEADOR");

    ModelAndView resultado = controlador.mostrarHome(request);

    assertEquals("redirect:/home-empleador", resultado.getViewName());
  }

  @Test
  void homeDeCandidatoSigueMostrandoLaVistaActual() {
    MockHttpServletRequest request = requestConRol("CANDIDATO");

    ModelAndView resultado = controlador.mostrarHome(request);

    assertEquals("home", resultado.getViewName());
  }

  @Test
  void homeEmpleadorRedirigeACandidatoAlInicioDeCandidatos() {
    MockHttpServletRequest request = requestConRol("CANDIDATO");

    ModelAndView resultado = controlador.mostrarHomeEmpleador(request);

    assertEquals("redirect:/home", resultado.getViewName());
  }

  @Test
  void homeEmpleadorRedirigeASesionAusenteAlLogin() {
    ModelAndView resultado = controlador.mostrarHomeEmpleador(new MockHttpServletRequest());

    assertEquals("redirect:/login", resultado.getViewName());
  }

  @Test
  void homeEmpleadorMuestraVistaAEmpleadorAutenticado() {
    MockHttpServletRequest request = requestConRol("EMPLEADOR");

    ModelAndView resultado = controlador.mostrarHomeEmpleador(request);

    assertEquals("home-empleador", resultado.getViewName());
    assertEquals(List.of(), resultado.getModel().get("ofertasActivas"));
    assertEquals(List.of(), resultado.getModel().get("ofertasEmpleadores"));
  }

  private MockHttpServletRequest requestConRol(String rol) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", rol);
    session.setAttribute("USUARIO_ID", 42L);
    request.setSession(session);
    return request;
  }
}
