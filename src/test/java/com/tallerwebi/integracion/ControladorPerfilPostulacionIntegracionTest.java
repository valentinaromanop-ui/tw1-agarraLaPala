package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.config.CorreoConfig;
import com.tallerwebi.dominio.PerfilPostulante;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
  classes = { SpringWebTestConfig.class, HibernateTestConfig.class, CorreoConfig.class }
)
class ControladorPerfilPostulacionIntegracionTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private ServicioLogin servicioLogin;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private MockMvc mockMvc;

  @BeforeEach
  void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  void debeCrearYActualizarPerfilConSkillsYMostrarlo() throws Exception {
    MockHttpSession session = crearSesionPostulante();
    Skill java = guardarSkill("java-" + UUID.randomUUID());
    Skill sql = guardarSkill("sql-" + UUID.randomUUID());

    MvcResult perfilInicial = mockMvc
      .perform(get("/perfil").session(session))
      .andExpect(status().isOk())
      .andReturn();
    assertThat(perfilInicial.getModelAndView().getModel().get("editarPerfil"), is(true));
    assertThat(
      ((PerfilPostulante) perfilInicial.getModelAndView().getModel().get("perfil")).getNombre(),
      is("Nombre")
    );
    mockMvc
      .perform(post("/perfil/guardar").session(session).param("telefono", "12A45"))
      .andExpect(status().isOk());

    mockMvc
      .perform(
        post("/perfil/guardar")
          .session(session)
          .param("nombre", "Ana Maria")
          .param("apellido", "Pérez")
          .param("telefono", "123456")
          .param("ubicacion", "Buenos Aires")
          .param("disponibilidadHoraria", "Mañana")
          .param("diasSemana", "Lunes")
          .param("modalidades", "REMOTO")
          .param("nivelExperiencia", "Junior")
          .param("tipoBusqueda", "Tiempo completo")
          .param("nivelIngles", "Intermedio")
          .param("linkedin", "https://linkedin.com/in/candidato")
          .param("github", "https://github.com/candidato")
          .param("presentacion", "Desarrollador backend")
          .param("idiomas[Español]", "Nativo")
          .param("skillIds", java.getId().toString())
      )
      .andExpect(status().is3xxRedirection());

    MvcResult resultadoGet = mockMvc
      .perform(get("/perfil").session(session).param("guardado", "true"))
      .andExpect(status().isOk())
      .andReturn();

    ModelAndView modelAndView = resultadoGet.getModelAndView();
    assertNotNull(modelAndView);
    PerfilPostulante perfil = (PerfilPostulante) modelAndView.getModel().get("perfil");
    assertEquals("Buenos Aires", perfil.getUbicacion());
    assertEquals("Ana Maria", perfil.getNombre());
    assertEquals("Pérez", perfil.getApellido());
    Usuario usuarioGuardado = (Usuario) modelAndView.getModel().get("usuario");
    assertEquals("Ana Maria", usuarioGuardado.getNombre());
    assertEquals("Pérez", usuarioGuardado.getApellido());
    assertThat(perfil.getSkillIds(), contains(java.getId()));
    assertThat(
      modelAndView.getModel().get("mensaje"),
      is("Perfil guardado correctamente. Tus selecciones quedaron asociadas a tu cuenta.")
    );
    assertThat(modelAndView.getModel().get("editarPerfil"), is(false));

    mockMvc
      .perform(
        post("/perfil/guardar")
          .session(session)
          .param("nombre", "Ana Maria")
          .param("apellido", "Pérez")
          .param("telefono", "654321")
          .param("skillIds", sql.getId().toString())
      )
      .andExpect(status().is3xxRedirection());

    MvcResult resultadoActualizado = mockMvc
      .perform(get("/perfil").session(session))
      .andExpect(status().isOk())
      .andReturn();
    PerfilPostulante perfilActualizado = (PerfilPostulante) resultadoActualizado
      .getModelAndView()
      .getModel()
      .get("perfil");
    assertEquals("654321", perfilActualizado.getTelefono());
    assertThat(perfilActualizado.getSkillIds(), contains(sql.getId()));
  }

  @Test
  void debeGuardarPostulacionUnaSolaVezYListarla() throws Exception {
    MockHttpSession session = crearSesionPostulante();

    mockMvc
      .perform(post("/postularse").session(session).param("ofertaId", "oferta-local-1"))
      .andExpect(status().isOk());
    mockMvc
      .perform(post("/postularse").session(session).param("ofertaId", " oferta-local-1 "))
      .andExpect(status().isOk());
    mockMvc
      .perform(post("/postularse").session(session).param("ofertaId", "oferta-local-2"))
      .andExpect(status().isOk());

    MvcResult resultado = mockMvc
      .perform(get("/postulaciones").session(session))
      .andExpect(status().isOk())
      .andReturn();
    ModelAndView modelAndView = resultado.getModelAndView();
    assertNotNull(modelAndView);
    assertThat((java.util.List<?>) modelAndView.getModel().get("postulaciones"), hasSize(2));
  }

  @Test
  void debeRegistrarPostulacionDesdeOfertasYMarcarlaComoPostulada() throws Exception {
    MockHttpSession session = crearSesionPostulante();
    String ofertaId = "JOBICY:987654321";

    mockMvc
      .perform(
        post("/postulaciones")
          .session(session)
          .param("idOferta", ofertaId)
          .param("titulo", "Oferta de prueba")
          .param("empresa", "Empresa de prueba")
      )
      .andExpect(status().isOk());

    mockMvc
      .perform(get("/postulaciones/ids").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$", hasItem(ofertaId)));
  }

  @Test
  void debeRedirigirAUsuariosNoAutenticadosDePerfilYPostulaciones() throws Exception {
    mockMvc.perform(get("/perfil")).andExpect(status().is3xxRedirection());
    mockMvc.perform(post("/perfil/guardar")).andExpect(status().is3xxRedirection());
    mockMvc.perform(get("/postulaciones")).andExpect(status().is3xxRedirection());
  }

  @Test
  void empresaVeUnPerfilDeLecturaYSeRechazanOtrosRolesYOfertas() throws Exception {
    long[] ids = guardarOfertaYPostulacion();
    MockHttpSession sesionEmpresa = new MockHttpSession();
    sesionEmpresa.setAttribute("ROL", "EMPLEADOR");
    sesionEmpresa.setAttribute("USUARIO_ID", ids[0]);

    MvcResult resultado = mockMvc
      .perform(
        get("/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil", ids[1], ids[2])
          .session(sesionEmpresa)
      )
      .andExpect(status().isOk())
      .andReturn();

    assertNotNull(resultado.getModelAndView());
    assertEquals("perfil-postulante-lectura", resultado.getModelAndView().getViewName());
    assertTrue(
      resultado.getModelAndView().getModel().get("perfilPostulante") instanceof
      com.tallerwebi.dominio.PerfilPostulanteDTO
    );
    assertFalse(resultado.getModelAndView().getModel().containsKey("usuario"));
    assertFalse(resultado.getModelAndView().getModel().containsKey("perfil"));
    String html = resultado.getResponse().getContentAsString();
    assertTrue(html.contains("Vista de empresa · solo lectura"));
    assertTrue(html.contains("Ada Lovelace"));
    assertTrue(html.contains("Java"));
    assertTrue(html.contains("Sin CV"));
    assertFalse(html.contains("no-debe-aparecer"));
    assertFalse(html.contains("<input"));

    MockHttpSession sesionCandidato = new MockHttpSession();
    sesionCandidato.setAttribute("ROL", "CANDIDATO");
    sesionCandidato.setAttribute("USUARIO_ID", ids[3]);
    mockMvc
      .perform(
        get("/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil", ids[1], ids[2])
          .session(sesionCandidato)
      )
      .andExpect(status().isForbidden());

    MockHttpSession otraEmpresa = new MockHttpSession();
    otraEmpresa.setAttribute("ROL", "EMPLEADOR");
    otraEmpresa.setAttribute("USUARIO_ID", ids[0] + 10000);
    mockMvc
      .perform(
        get("/ofertas/{ofertaId}/postulantes/{postulacionId}/perfil", ids[1], ids[2])
          .session(otraEmpresa)
      )
      .andExpect(status().isForbidden());
  }

  private long[] guardarOfertaYPostulacion() {
    return new TransactionTemplate(transactionManager)
      .execute(status -> {
        Usuario empresa = new Usuario();
        empresa.setEmail("empresa-" + UUID.randomUUID() + "@correo.com");
        empresa.setPassword("interno-no-exponer");
        empresa.setRol("EMPLEADOR");
        sessionFactory.getCurrentSession().persist(empresa);

        Usuario candidato = new Usuario();
        candidato.setEmail("ada-" + UUID.randomUUID() + "@correo.com");
        candidato.setPassword("no-debe-aparecer");
        candidato.setRol("CANDIDATO");
        candidato.setNombre("Ada");
        candidato.setApellido("Lovelace");
        candidato.setHabilidades(java.util.List.of("Java, SQL"));
        sessionFactory.getCurrentSession().persist(candidato);

        Vacante oferta = new Vacante();
        oferta.setTitulo("Desarrolladora Java");
        oferta.setEmpleadorId(empresa.getId());
        sessionFactory.getCurrentSession().persist(oferta);

        Postulacion postulacion = new Postulacion();
        postulacion.setUsuario(candidato);
        postulacion.setIdOferta("LOCAL:" + oferta.getId());
        postulacion.setFechaPostulacion(LocalDateTime.of(2026, 10, 20, 15, 30));
        sessionFactory.getCurrentSession().persist(postulacion);

        PerfilPostulante perfil = new PerfilPostulante();
        perfil.setUsuario(candidato);
        perfil.setPresentacion("Desarrolladora de software");
        perfil.setLinkedin("https://example.com/ada");
        sessionFactory.getCurrentSession().persist(perfil);
        sessionFactory.getCurrentSession().flush();
        return new long[] {
          empresa.getId(),
          oferta.getId(),
          postulacion.getId(),
          candidato.getId(),
        };
      });
  }

  private MockHttpSession crearSesionPostulante() throws UsuarioExistente {
    String email = "postulante-" + UUID.randomUUID() + "@correo.com";
    Usuario usuario = new Usuario();
    usuario.setNombre("Nombre");
    usuario.setApellido("Inicial");
    usuario.setEmail(email);
    usuario.setPassword("secreto");
    usuario.setRol("CANDIDATO");
    servicioLogin.registrar(usuario);

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "CANDIDATO");
    session.setAttribute("USUARIO_ID", usuario.getId());
    return session;
  }

  private Skill guardarSkill(String nombre) {
    return new TransactionTemplate(transactionManager)
      .execute(status -> {
        Skill skill = new Skill();
        skill.setNombre(nombre);
        sessionFactory.getCurrentSession().persist(skill);
        return skill;
      });
  }
}
