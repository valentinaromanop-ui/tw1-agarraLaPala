package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tallerwebi.dominio.CandidatoSugeridoDTO;
import com.tallerwebi.dominio.ServicioInvitacion;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPago;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteSkill;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

class ControladorPostulantesOfertaTest {

  @Test
  void filtrosDeCandidatosCombinanPorcentajeYTodasLasSkillsElegidas() {
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
    Skill java = new Skill();
    java.setNombre("Java");
    Skill sql = new Skill();
    sql.setNombre("SQL");
    VacanteSkill javaOferta = new VacanteSkill();
    javaOferta.setSkill(java);
    VacanteSkill sqlOferta = new VacanteSkill();
    sqlOferta.setSkill(sql);
    oferta.setVacanteSkills(Set.of(javaOferta, sqlOferta));
    when(servicioPostulacion.obtenerPostulantes(10L, 42L)).thenReturn(List.of());
    when(servicioPostulacion.obtenerVacante("LOCAL", 10L)).thenReturn(oferta);
    when(servicioPostulacion.obtenerHabilidadesOferta("LOCAL", 10L))
      .thenReturn(List.of("Java", "SQL"));
    when(servicioInvitacion.obtenerCandidatosSugeridos(10L, 42L))
      .thenReturn(
        List.of(
          sugerido(20L, "Java y SQL", List.of("Java", "SQL"), 100),
          sugerido(21L, "Java", List.of("Java"), 50),
          sugerido(22L, "SQL", List.of("SQL"), 50)
        )
      );
    when(servicioInvitacion.puedeInvitar(10L, 42L)).thenReturn(true);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "EMPLEADOR");
    session.setAttribute("USUARIO_ID", 42L);

    var porSkill = controlador.mostrarPostulantesConFiltros(
      10L,
      "TODOS",
      null,
      null,
      null,
      false,
      50,
      List.of("Java"),
      1,
      session,
      null,
      false
    );
    var porPorcentaje = controlador.mostrarPostulantesConFiltros(
      10L,
      "TODOS",
      null,
      null,
      null,
      false,
      75,
      null,
      1,
      session,
      null,
      false
    );
    var combinados = controlador.mostrarPostulantesConFiltros(
      10L,
      "TODOS",
      null,
      null,
      null,
      false,
      75,
      List.of("Java"),
      1,
      session,
      null,
      false
    );

    assertEquals(
      List.of(20L, 21L),
      ((List<CandidatoSugeridoDTO>) porSkill.getModel().get("candidatosSugeridos")).stream()
        .map(CandidatoSugeridoDTO::getCandidatoId)
        .toList()
    );
    assertEquals(
      List.of(20L),
      ((List<CandidatoSugeridoDTO>) porPorcentaje.getModel().get("candidatosSugeridos")).stream()
        .map(CandidatoSugeridoDTO::getCandidatoId)
        .toList()
    );
    assertEquals(
      List.of(20L),
      ((List<CandidatoSugeridoDTO>) combinados.getModel().get("candidatosSugeridos")).stream()
        .map(CandidatoSugeridoDTO::getCandidatoId)
        .toList()
    );
  }

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
    Skill java = new Skill();
    java.setNombre("Java");
    VacanteSkill javaOferta = new VacanteSkill();
    javaOferta.setSkill(java);
    oferta.setVacanteSkills(Set.of(javaOferta));
    when(servicioPostulacion.obtenerPostulantes(10L, 42L)).thenReturn(List.of());
    when(servicioPostulacion.obtenerVacante("LOCAL", 10L)).thenReturn(oferta);
    when(servicioPostulacion.obtenerHabilidadesOferta("LOCAL", 10L)).thenReturn(List.of("Java"));
    List<CandidatoSugeridoDTO> sugeridos = new ArrayList<>();
    sugeridos.add(
      new CandidatoSugeridoDTO(20L, "Ana Gómez", "ana@example.com", List.of("Java"), 100, null)
    );
    for (long id = 21; id <= 30; id++) {
      sugeridos.add(sugerido(id, "Candidato " + id, List.of("Java"), 100));
    }
    when(servicioInvitacion.obtenerCandidatosSugeridos(10L, 42L)).thenReturn(sugeridos);
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

    var mockMvc = MockMvcBuilders.standaloneSetup(controlador).setViewResolvers(vistas).build();
    String html = mockMvc
      .perform(
        get("/ofertas/10/postulantes")
          .param("porcentajeMinimo", "75")
          .param("skills", "Java")
          .session(session)
      )
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertTrue(html.contains("Candidatos sugeridos"));
    assertTrue(html.contains("Ana Gómez"));
    assertTrue(html.contains("Invitar a postularse"));
    assertTrue(html.contains("Siguiente"));
    assertTrue(html.contains("porcentajeMinimo=75"));
    assertTrue(html.contains("skills=Java"));
    assertTrue(html.contains("Vas a invitar a 11 candidatos a la oferta Desarrollador Java"));

    String segundaPagina = mockMvc
      .perform(
        get("/ofertas/10/postulantes")
          .param("porcentajeMinimo", "75")
          .param("skills", "Java")
          .param("pagina", "2")
          .session(session)
      )
      .andReturn()
      .getResponse()
      .getContentAsString();
    assertTrue(segundaPagina.contains("Página 2 de 2"));
    assertTrue(segundaPagina.contains("Candidato 30"));
  }

  private CandidatoSugeridoDTO sugerido(
    Long id,
    String nombre,
    List<String> habilidades,
    double porcentaje
  ) {
    return new CandidatoSugeridoDTO(
      id,
      nombre,
      nombre + "@example.com",
      habilidades,
      porcentaje,
      null
    );
  }
}
