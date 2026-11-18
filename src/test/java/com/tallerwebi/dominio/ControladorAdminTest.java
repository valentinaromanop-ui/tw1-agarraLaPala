package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tallerwebi.presentacion.ControladorAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

public class ControladorAdminTest {

  ServicioReporte s = mock(ServicioReporte.class);
  ControladorAdmin c = new ControladorAdmin(
    s,
    mock(RepositorioUsuario.class),
    mock(RepositorioVacante.class)
  );

  @Test
  void noAdminEsRedirigidoAlLogin() {
    MockHttpSession ses = new MockHttpSession();
    ses.setAttribute("ROL", "CANDIDATO");
    assertEquals("redirect:/login", c.bc(1L, ses));
    verifyNoInteractions(s);
  }

  @Test
  void adminBloqueaUsuario() {
    MockHttpSession ses = new MockHttpSession();
    ses.setAttribute("ROL", "ADMIN");
    assertEquals("redirect:/admin", c.ub(5L, ses));
    verify(s).bloquearUsuarioDirecto(5L);
  }

  @Test
  void panelCargaListasParaAdmin() {
    MockHttpSession ses = new MockHttpSession();
    ses.setAttribute("ROL", "ADMIN");
    assertEquals("admin", c.panel(ses).getViewName());
  }
}
