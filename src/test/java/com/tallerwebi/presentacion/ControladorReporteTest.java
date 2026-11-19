package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.dominio.LimiteReportesAlcanzadoException;
import com.tallerwebi.dominio.ServicioReporte;
import com.tallerwebi.dominio.TipoReporte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorReporteTest {

  private ServicioReporte servicio;
  private ControladorReporte controlador;
  private MockHttpSession sesion;
  private MockHttpServletRequest request;
  private RedirectAttributesModelMap ra;

  @BeforeEach
  public void init() {
    servicio = mock(ServicioReporte.class);
    controlador = new ControladorReporte(servicio);
    sesion = new MockHttpSession();
    request = new MockHttpServletRequest();
    ra = new RedirectAttributesModelMap();
  }

  @Test
  public void sinSesionDeberiaRedirigirAlLogin() {
    String vista = controlador.reportar(
      TipoReporte.OFERTA,
      1L,
      2L,
      "Spam",
      null,
      sesion,
      request,
      ra
    );

    assertEquals("redirect:/login", vista);
    verifyNoInteractions(servicio);
  }

  @Test
  public void reporteValidoDeberiaLlamarAlServicioYAvisarExito() {
    sesion.setAttribute("USUARIO_ID", 5L);
    request.addHeader("Referer", "/vacantes/1");

    String vista = controlador.reportar(
      TipoReporte.OFERTA,
      1L,
      2L,
      "Spam",
      "detalle",
      sesion,
      request,
      ra
    );

    assertEquals("redirect:/vacantes/1", vista);
    verify(servicio).reportar(TipoReporte.OFERTA, 1L, 2L, 5L, "Spam", "detalle");
    assertEquals("Reporte enviado", ra.getFlashAttributes().get("exito"));
  }

  @Test
  public void sinRefererDeberiaRedirigirAHome() {
    sesion.setAttribute("USUARIO_ID", 5L);

    String vista = controlador.reportar(
      TipoReporte.MENSAJE,
      1L,
      2L,
      "Spam",
      null,
      sesion,
      request,
      ra
    );

    assertEquals("redirect:/home", vista);
  }

  @Test
  public void limiteAlcanzadoDeberiaAvisarError() {
    sesion.setAttribute("USUARIO_ID", 5L);
    doThrow(new LimiteReportesAlcanzadoException("Límite diario"))
      .when(servicio)
      .reportar(any(), any(), any(), any(), any(), any());

    controlador.reportar(TipoReporte.OFERTA, 1L, 2L, "Spam", null, sesion, request, ra);

    assertEquals("Límite diario", ra.getFlashAttributes().get("error"));
  }
}
