package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.tallerwebi.dominio.ServicioPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

class ControladorPagoTest {

  private ControladorPago controlador;

  @BeforeEach
  void iniciar() {
    controlador = new ControladorPago(mock(ServicioPago.class));
  }

  @Test
  void empleadorEnRutaPremiumDeCandidatoEsRedirigidoASuPlan() {
    ModelAndView resultado = controlador.mostrarPago(requestConRol("EMPLEADOR"));

    assertEquals("redirect:/premium/empleador/pagar", resultado.getViewName());
  }

  @Test
  void candidatoEnRutaPremiumDeEmpleadorEsRedirigidoASuPlan() {
    ModelAndView resultado = controlador.mostrarPagoEmpleador(requestConRol("CANDIDATO"));

    assertEquals("redirect:/premium/pagar", resultado.getViewName());
  }

  @Test
  void sinSesionEnRutasPremiumEsRedirigidoAlLogin() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertEquals("redirect:/login", controlador.mostrarPago(request).getViewName());
    assertEquals("redirect:/login", controlador.mostrarPagoEmpleador(request).getViewName());
  }

  @Test
  void empleadorAccedeAlFormularioDeSuPlan() {
    ModelAndView resultado = controlador.mostrarPagoEmpleador(requestConRol("EMPLEADOR"));

    assertEquals("pago-premium-empleador", resultado.getViewName());
    assertEquals("15", resultado.getModel().get("precioPlanEmpleador"));
  }

  @Test
  void candidatoAccedeAlFormularioDeSuPlanExistente() {
    ModelAndView resultado = controlador.mostrarPago(requestConRol("CANDIDATO"));

    assertEquals("pago-premium", resultado.getViewName());
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
