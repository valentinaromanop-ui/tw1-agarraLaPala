package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Pago;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.Suscripcion;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ControladorPagoTest {

  private ControladorPago controlador;
  private ServicioPago servicioPago;

  @BeforeEach
  void iniciar() {
    servicioPago = mock(ServicioPago.class);
    controlador = new ControladorPago(servicioPago);
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

  @Test
  void premiumActivoOcultaFormularioHastaQueElUsuarioPideExtenderlo() {
    when(servicioPago.fechaFinPremium(42L)).thenReturn(LocalDateTime.of(2026, 12, 1, 0, 0));

    ModelAndView sinExtender = controlador.mostrarPago(requestConRol("CANDIDATO"));
    MockHttpServletRequest request = requestConRol("CANDIDATO");
    request.setParameter("extender", "TRUE");
    ModelAndView extender = controlador.mostrarPago(request);

    assertEquals(false, sinExtender.getModel().get("mostrarFormularioPago"));
    assertEquals(true, extender.getModel().get("mostrarFormularioPago"));
    assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), extender.getModel().get("premiumHasta"));
  }

  @Test
  void pagoDeCandidatoRedirigeYAgregaConfirmacionConLaFechaDeVencimiento() {
    DatosPago datos = datosPago();
    Suscripcion suscripcion = suscripcionHasta(LocalDateTime.of(2026, 12, 10, 0, 0));
    when(servicioPago.pagar(42L, "Ana Pérez", "4111111111111111", "12/99", "123"))
      .thenReturn(suscripcion);
    RedirectAttributesModelMap mensajes = new RedirectAttributesModelMap();

    ModelAndView resultado = controlador.pagar(datos, requestConRol("CANDIDATO"), mensajes);

    assertEquals("redirect:/perfil", resultado.getViewName());
    assertEquals(
      "¡Ya sos Premium! Disfrutá tus ventajas hasta 10/12/2026",
      mensajes.getFlashAttributes().get("mensaje")
    );
    verify(servicioPago).pagar(42L, "Ana Pérez", "4111111111111111", "12/99", "123");
  }

  @Test
  void pagoRechazadoLimpiaNumeroYCvvYConservaElError() {
    DatosPago datos = datosPago();
    when(servicioPago.pagar(42L, "Ana Pérez", "4111111111111111", "12/99", "123"))
      .thenThrow(new IllegalArgumentException("El pago fue rechazado."));
    when(servicioPago.fechaFinPremium(42L)).thenReturn(null);

    ModelAndView resultado = controlador.pagar(datos, requestConRol("CANDIDATO"));

    assertEquals("pago-premium", resultado.getViewName());
    assertEquals("El pago fue rechazado.", resultado.getModel().get("error"));
    assertEquals(true, resultado.getModel().get("mostrarFormularioPago"));
    assertNull(resultado.getModel().get("premiumHasta"));
    assertEquals("", datos.getNumero());
    assertEquals("", datos.getCvv());
  }

  @Test
  void pagoDeEmpleadorRedirigeAConfiguracionDeEmpresa() {
    DatosPago datos = datosPago();
    Suscripcion suscripcion = suscripcionHasta(LocalDateTime.of(2026, 12, 10, 0, 0));
    when(servicioPago.pagarEmpleador(42L, "Ana Pérez", "4111111111111111", "12/99", "123"))
      .thenReturn(suscripcion);
    RedirectAttributesModelMap mensajes = new RedirectAttributesModelMap();

    ModelAndView resultado = controlador.pagarEmpleador(
      datos,
      requestConRol("EMPLEADOR"),
      mensajes
    );

    assertEquals("redirect:/empleador/perfil", resultado.getViewName());
    assertEquals(
      "¡Ya sos Premium! Disfrutá tus ventajas hasta 10/12/2026",
      mensajes.getFlashAttributes().get("mensaje")
    );
    verify(servicioPago).pagarEmpleador(42L, "Ana Pérez", "4111111111111111", "12/99", "123");
  }

  @Test
  void rechazoDePagoDeEmpleadorMuestraErrorYBorraDatosSensibles() {
    DatosPago datos = datosPago();
    when(servicioPago.pagarEmpleador(42L, "Ana Pérez", "4111111111111111", "12/99", "123"))
      .thenThrow(new IllegalArgumentException("Tarjeta inválida"));
    when(servicioPago.fechaFinPremium(42L)).thenReturn(null);

    ModelAndView resultado = controlador.pagarEmpleador(datos, requestConRol("EMPLEADOR"));

    assertEquals("pago-premium-empleador", resultado.getViewName());
    assertEquals("Tarjeta inválida", resultado.getModel().get("error"));
    assertEquals(true, resultado.getModel().get("mostrarFormularioPago"));
    assertEquals("", datos.getNumero());
    assertEquals("", datos.getCvv());
  }

  @Test
  void historialRequiereCandidatoAutenticadoYListaSusPagos() {
    MockHttpServletRequest requestSinSesion = new MockHttpServletRequest();
    assertEquals("redirect:/login", controlador.historial(requestSinSesion).getViewName());
    when(servicioPago.listarPagos(42L)).thenReturn(List.of(new Pago()));

    ModelAndView resultado = controlador.historial(requestConRol("CANDIDATO"));

    assertEquals("pagos", resultado.getViewName());
    assertEquals(1, ((List<?>) resultado.getModel().get("pagos")).size());
    verify(servicioPago).listarPagos(42L);
  }

  private MockHttpServletRequest requestConRol(String rol) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", rol);
    session.setAttribute("USUARIO_ID", 42L);
    request.setSession(session);
    return request;
  }

  private DatosPago datosPago() {
    DatosPago datos = new DatosPago();
    datos.setTitular("Ana Pérez");
    datos.setNumero("4111111111111111");
    datos.setVencimiento("12/99");
    datos.setCvv("123");
    return datos;
  }

  private Suscripcion suscripcionHasta(LocalDateTime fechaFin) {
    Suscripcion suscripcion = new Suscripcion();
    suscripcion.setFechaFin(fechaFin);
    return suscripcion;
  }
}
