package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.PerfilPostulante;
import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
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
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
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

    mockMvc.perform(get("/perfil").session(session)).andExpect(status().isOk());
    mockMvc
      .perform(post("/perfil/guardar").session(session).param("telefono", "12A45"))
      .andExpect(status().isOk());

    mockMvc
      .perform(
        post("/perfil/guardar")
          .session(session)
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
    assertThat(perfil.getSkillIds(), contains(java.getId()));
    assertThat(
      modelAndView.getModel().get("mensaje"),
      is("Perfil guardado correctamente. Tus selecciones quedaron asociadas a tu cuenta.")
    );

    mockMvc
      .perform(
        post("/perfil/guardar")
          .session(session)
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
  void debeRedirigirAUsuariosNoAutenticadosDePerfilYPostulaciones() throws Exception {
    mockMvc.perform(get("/perfil")).andExpect(status().is3xxRedirection());
    mockMvc.perform(post("/perfil/guardar")).andExpect(status().is3xxRedirection());
    mockMvc.perform(get("/postulaciones")).andExpect(status().is3xxRedirection());
  }

  private MockHttpSession crearSesionPostulante() throws UsuarioExistente {
    String email = "postulante-" + UUID.randomUUID() + "@correo.com";
    Usuario usuario = new Usuario();
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
