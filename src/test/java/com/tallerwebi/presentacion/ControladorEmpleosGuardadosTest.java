package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.EmpleoGuardado;
import com.tallerwebi.dominio.ServicioEmpleoGuardado;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

class ControladorEmpleosGuardadosTest {

  private ServicioEmpleoGuardado servicio;
  private ControladorEmpleosGuardados controlador;
  private MockHttpSession session;

  @BeforeEach
  void iniciar() {
    servicio = mock(ServicioEmpleoGuardado.class);
    controlador = new ControladorEmpleosGuardados(servicio);
    session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 1L);
  }

  @Test
  void guardaYDevuelveRespuestaJson() {
    EmpleoGuardado guardado = new EmpleoGuardado();
    guardado.setIdOferta("LOCAL:42");
    when(servicio.guardar(any(Long.class), any(EmpleoGuardado.class))).thenReturn(guardado);

    ResponseEntity<?> respuesta = controlador.guardar(
      "LOCAL:42",
      "Título",
      "Empresa",
      "CABA",
      "Remota",
      "https://ejemplo.test",
      session
    );

    assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    Map<?, ?> cuerpo = assertInstanceOf(Map.class, respuesta.getBody());
    assertEquals(true, cuerpo.get("ok"));
    assertEquals("LOCAL:42", cuerpo.get("idOferta"));
  }

  @Test
  void quitaYDevuelveRespuestaJson() {
    ResponseEntity<?> respuesta = controlador.quitar("LOCAL:42", session);

    assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    Map<?, ?> cuerpo = assertInstanceOf(Map.class, respuesta.getBody());
    assertEquals(true, cuerpo.get("ok"));
    assertEquals("LOCAL:42", cuerpo.get("idOferta"));
    verify(servicio).quitar(1L, "LOCAL:42");
  }

  @Test
  void listaVistaConEmpleosDelUsuario() {
    List<EmpleoGuardado> empleos = List.of(new EmpleoGuardado());
    when(servicio.listar(1L)).thenReturn(empleos);

    ModelAndView respuesta = controlador.mostrarEmpleosGuardados(session);

    assertEquals("empleos-guardados", respuesta.getViewName());
    assertEquals(empleos, respuesta.getModel().get("empleosGuardados"));
  }

  @Test
  void listaIdsGuardados() {
    when(servicio.listarIds(1L)).thenReturn(List.of("LOCAL:42"));

    ResponseEntity<?> respuesta = controlador.listarIds(session);

    assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    assertEquals(List.of("LOCAL:42"), respuesta.getBody());
  }

  @Test
  void rechazaSesionAusenteEnTodasLasOperaciones() {
    HttpSession sesionVacia = new MockHttpSession();

    assertEquals("redirect:/login", controlador.mostrarEmpleosGuardados(sesionVacia).getViewName());
    assertEquals(HttpStatus.UNAUTHORIZED, controlador.listarIds(sesionVacia).getStatusCode());
    assertEquals(
      HttpStatus.UNAUTHORIZED,
      controlador.guardar("LOCAL:42", null, null, null, null, null, sesionVacia).getStatusCode()
    );
    assertEquals(
      HttpStatus.UNAUTHORIZED,
      controlador.quitar("LOCAL:42", sesionVacia).getStatusCode()
    );
  }

  @Test
  void traduceErroresDelServicioARespuestaBadRequest() {
    when(servicio.guardar(any(Long.class), any(EmpleoGuardado.class)))
      .thenThrow(new IllegalArgumentException("La oferta es obligatoria."));
    when(servicio.listar(any(Long.class))).thenThrow(new IllegalArgumentException("Error"));
    doThrow(new IllegalArgumentException("La oferta es obligatoria."))
      .when(servicio)
      .quitar(1L, "LOCAL:42");

    assertEquals(
      HttpStatus.BAD_REQUEST,
      controlador.guardar("LOCAL:42", null, null, null, null, null, session).getStatusCode()
    );
    assertEquals(HttpStatus.BAD_REQUEST, controlador.quitar("LOCAL:42", session).getStatusCode());
    org.junit.jupiter.api.Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> controlador.mostrarEmpleosGuardados(session)
    );
  }
}
