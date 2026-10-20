package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.tallerwebi.dominio.PerfilPostulanteDTO;
import com.tallerwebi.dominio.ServicioPostulacion;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

class ControladorPostulacionTest {

  private final ServicioPostulacion servicio = mock(ServicioPostulacion.class);
  private final ControladorPostulacion controlador = new ControladorPostulacion(servicio);

  @Test
  void laRutaDePerfilEsSoloGet() throws NoSuchMethodException {
    Method metodo =
      ControladorPostulacion.class.getMethod(
          "mostrarPerfilPostulante",
          Long.class,
          Long.class,
          HttpSession.class
        );

    assertTrue(metodo.isAnnotationPresent(GetMapping.class));
    assertEquals(
      "/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil",
      metodo.getAnnotation(GetMapping.class).value()[0]
    );
  }

  @Test
  void rechazaCandidatosAntesDeConsultarElPerfil() {
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute("ROL", "CANDIDATO");
    sesion.setAttribute("USUARIO_ID", 15L);

    ResponseStatusException excepcion = assertThrows(
      ResponseStatusException.class,
      () -> controlador.mostrarPerfilPostulante(10L, 20L, sesion)
    );

    assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode());
    verify(servicio, never()).obtenerPerfilPostulante(10L, 20L, 15L);
  }

  @Test
  void laVistaRecibeSoloElDtoDeLectura() {
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute("ROL", "EMPLEADOR");
    sesion.setAttribute("USUARIO_ID", 42L);
    PerfilPostulanteDTO perfil = new PerfilPostulanteDTO(
      "Ada Lovelace",
      "ada@example.com",
      "AL",
      false,
      false,
      java.util.List.of(),
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      java.util.List.of(),
      java.util.List.of(),
      java.util.List.of(),
      java.util.Map.of(),
      "Desarrolladora Java",
      java.time.LocalDateTime.of(2026, 10, 20, 15, 30)
    );
    org.mockito.Mockito.when(servicio.obtenerPerfilPostulante(10L, 20L, 42L)).thenReturn(perfil);

    org.springframework.web.servlet.ModelAndView resultado = controlador.mostrarPerfilPostulante(
      10L,
      20L,
      sesion
    );

    assertEquals("perfil-postulante-lectura", resultado.getViewName());
    assertEquals(perfil, resultado.getModel().get("perfilPostulante"));
    assertEquals(10L, resultado.getModel().get("ofertaId"));
    assertEquals(20L, resultado.getModel().get("postulacionId"));
  }
}
