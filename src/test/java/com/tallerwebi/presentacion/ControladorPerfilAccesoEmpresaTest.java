package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.dominio.PerfilPostulante;
import com.tallerwebi.dominio.ServicioCurriculumGenerado;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPerfil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ControladorPerfilAccesoEmpresaTest {

  private final ServicioPerfil servicioPerfil = mock(ServicioPerfil.class);
  private final ControladorPerfil controlador = new ControladorPerfil(
    servicioPerfil,
    mock(ServicioCurriculumGenerado.class),
    mock(ServicioPago.class)
  );

  @Test
  void empresaNoPuedeUsarLosEndpointsDeEdicionDelPerfilDelCandidato() {
    MockHttpSession sesionEmpresa = new MockHttpSession();
    sesionEmpresa.setAttribute("ROL", "EMPLEADOR");
    sesionEmpresa.setAttribute("USUARIO_ID", 42L);
    RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    assertEquals(
      "redirect:/login",
      controlador.subirFotoPerfil(
        new MockMultipartFile("foto", "foto.png", "image/png", new byte[] { 1 }),
        sesionEmpresa,
        redirect
      )
    );
    assertEquals("redirect:/login", controlador.eliminarFotoPerfil(sesionEmpresa, redirect));
    assertEquals(HttpStatus.FORBIDDEN, controlador.verFoto(sesionEmpresa).getStatusCode());

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setSession(sesionEmpresa);
    assertEquals(
      "redirect:/login",
      controlador.guardarPerfil(new PerfilPostulante(), request).getViewName()
    );
    verifyNoInteractions(servicioPerfil);
  }
}
