package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.VacanteDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

public class ControladorDetalleVacanteTest {

  private ServicioPostulacion servicio;
  private ControladorDetalleVacante controlador;
  private MockHttpSession session;
  private VacanteDTO oferta;

  @BeforeEach
  public void init() {
    servicio = mock(ServicioPostulacion.class);
    controlador = new ControladorDetalleVacante(servicio);
    session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 1L);
    session.setAttribute("ROL", "CANDIDATO");
    oferta = new VacanteDTO();
    oferta.setId(10L);
    oferta.setFuente("JOBICY");
    oferta.setTitulo("Desarrollador Java");
    oferta.setDescripcion("Descripción completa de la oferta");
    session.setAttribute("vacantes", List.of(oferta));
  }

  @Test
  public void deberiaMostrarLaOfertaDeLaBusquedaSinGuardarla() {
    ModelAndView vista = controlador.mostrarDetalle("JOBICY", 10L, session);

    assertEquals("detalle-vacante", vista.getViewName());
    assertSame(oferta, vista.getModel().get("vacante"));
    verify(servicio, never()).registrarEnVacante(any(), any());
  }

  @Test
  public void deberiaRegistrarYVolverConUnaConfirmacion() {
    RedirectAttributesModelMap mensajes = new RedirectAttributesModelMap();

    ModelAndView vista = controlador.postular(newDatosPostulacion(10L), session, mensajes);

    assertEquals("redirect:/vacantes/resultados", vista.getViewName());
    assertNotNull(mensajes.getFlashAttributes().get("mensaje"));
    verify(servicio).registrarEnVacante(1L, oferta);
  }

  @Test
  public void deberiaMarcarLaOfertaYaPostulada() {
    when(servicio.obtenerOfertasPostuladas(1L)).thenReturn(List.of("JOBICY:10"));

    ModelAndView vista = controlador.mostrarDetalle("JOBICY", 10L, session);

    assertEquals(true, vista.getModel().get("yaPostulado"));
  }

  @Test
  public void noDeberiaAceptarPostulacionesSinSesionDePostulante() {
    ModelAndView vista = controlador.postular(
      newDatosPostulacion(10L),
      new MockHttpSession(),
      new RedirectAttributesModelMap()
    );

    assertEquals("redirect:/login", vista.getViewName());
    verify(servicio, never()).registrarEnVacante(any(), any());
  }

  @Test
  public void noDeberiaGuardarUnaOfertaQueNoEstaEnLaBusqueda() {
    controlador.postular(newDatosPostulacion(999L), session, new RedirectAttributesModelMap());

    verify(servicio, never()).registrarEnVacante(any(), any());
  }

  private DatosPostulacion newDatosPostulacion(Long id) {
    DatosPostulacion datos = new DatosPostulacion();
    datos.setFuente("JOBICY");
    datos.setId(id);
    return datos;
  }

  @Test
  public void deberiaIncluirLosDatosParaPostularse() {
    ModelAndView vista = controlador.mostrarDetalle("JOBICY", 10L, session);
    DatosPostulacion datos = (DatosPostulacion) vista.getModel().get("datosPostulacion");

    assertEquals("JOBICY", datos.getFuente());
    assertEquals(10L, datos.getId());
  }
}
