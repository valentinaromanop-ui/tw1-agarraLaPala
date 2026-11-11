package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tallerwebi.dominio.CandidatoSugeridoDTO;
import com.tallerwebi.dominio.ServicioInvitacion;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.Vacante;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

class ControladorPostulantesOfertaTest {

  @Test
  void postulantesOfertaMuestraCandidatosSugeridosYAccionDeInvitacion() throws Exception {
    ServicioPostulacion servicioPostulacion = mock(ServicioPostulacion.class);
    ServicioOfertaEmpleador servicioOferta = mock(ServicioOfertaEmpleador.class);
    ServicioPago servicioPago = mock(ServicioPago.class);
    ServicioInvitacion servicioInvitacion = mock(ServicioInvitacion.class);
    ControladorPostulacion controlador = new ControladorPostulacion(
      servicioPostulacion,
      servicioOferta,
      servicioPago
    );
    controlador.setServicioInvitacion(servicioInvitacion);
    Vacante oferta = new Vacante();
    oferta.setId(10L);
    oferta.setTitulo("Desarrollador Java");
    when(servicioPostulacion.obtenerPostulantes(10L, 42L)).thenReturn(List.of());
    when(servicioPostulacion.obtenerVacante("LOCAL", 10L)).thenReturn(oferta);
    when(servicioInvitacion.obtenerCandidatosSugeridos(10L, 42L))
      .thenReturn(
        List.of(
          new CandidatoSugeridoDTO(20L, "Ana Gómez", "ana@example.com", List.of("Java"), 100, null)
        )
      );
    when(servicioInvitacion.puedeInvitar(10L, 42L)).thenReturn(true);

    FileTemplateResolver templates = new FileTemplateResolver();
    templates.setPrefix("src/main/webapp/WEB-INF/views/thymeleaf/");
    templates.setSuffix(".html");
    templates.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(templates);
    ThymeleafViewResolver vistas = new ThymeleafViewResolver();
    vistas.setTemplateEngine(engine);
    vistas.setCharacterEncoding("UTF-8");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "EMPLEADOR");
    session.setAttribute("USUARIO_ID", 42L);

    String html = MockMvcBuilders
      .standaloneSetup(controlador)
      .setViewResolvers(vistas)
      .build()
      .perform(get("/ofertas/10/postulantes").session(session))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertTrue(html.contains("Candidatos sugeridos"));
    assertTrue(html.contains("Ana Gómez"));
    assertTrue(html.contains("Invitar a postularse"));
  }
}
