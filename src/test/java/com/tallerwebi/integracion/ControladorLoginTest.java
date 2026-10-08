package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import com.tallerwebi.presentacion.DatosLogin;
import java.util.Objects;
import java.util.UUID;
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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class }) //SpringWebTestConfig configura la parte web, y HibernateTestConfig usa una base de datos en memoria (HSQLDB),
public class ControladorLoginTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private ServicioLogin servicioLogin;

  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void debeRetornarLaPaginaLoginCuandoSeNavegaALaRaiz() throws Exception {
    MvcResult result =
      this.mockMvc.perform(get("/"))
        /*.andDo(print())*/
        .andExpect(status().is3xxRedirection())
        .andReturn();

    ModelAndView modelAndView = result.getModelAndView();
    assert modelAndView != null;
    assertThat(
      "redirect:/login",
      equalToIgnoringCase(Objects.requireNonNull(modelAndView.getViewName()))
    );
    assertThat(true, is(modelAndView.getModel().isEmpty()));
  }

  @Test
  public void debeRetornarLaPaginaLoginCuandoSeNavegaALLogin() throws Exception {
    MvcResult result = this.mockMvc.perform(get("/login")).andExpect(status().isOk()).andReturn();

    ModelAndView modelAndView = result.getModelAndView();
    assert modelAndView != null;
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("login"));
    assertThat(modelAndView.getModel().get("datosLogin"), instanceOf(DatosLogin.class));
  }

  @Test
  public void debeMostrarElSelectorYLosFormulariosDeRegistro() throws Exception {
    this.mockMvc.perform(get("/registro")).andExpect(status().isOk());
    this.mockMvc.perform(get("/registro/empleador")).andExpect(status().isOk());
    this.mockMvc.perform(get("/registro/candidato")).andExpect(status().isOk());
  }

  @Test
  public void debeRegistrarEmpleadorYDejarloIdentificadoEnLaSesion() throws Exception {
    String email = "empleador-" + UUID.randomUUID() + "@empresa.com";

    MvcResult resultado =
      this.mockMvc.perform(
          post("/registro/empleador")
            .param("empresa", "Empresa SA")
            .param("legajo", "12345")
            .param("email", email)
            .param("password", "secreto")
            .param("confirmarPassword", "secreto")
        )
        .andExpect(status().is3xxRedirection())
        .andReturn();

    MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
    assertThat(session.getAttribute("ROL"), is("EMPLEADOR"));
    Usuario usuario = servicioLogin.consultarUsuario(email, "secreto");
    assertThat(usuario.getEmpresa(), is("Empresa SA"));
    assertThat(usuario.getLegajo(), is("12345"));
    assertThat(usuario.getPassword().equals("secreto"), is(false));
  }

  @Test
  public void debeRegistrarCandidatoConHabilidadesYPermitirLoginUnico() throws Exception {
    String email = "candidato-" + UUID.randomUUID() + "@correo.com";

    MvcResult resultado =
      this.mockMvc.perform(
          post("/registro/candidato")
            .param("email", email)
            .param("password", "secreto")
            .param("confirmarPassword", "secreto")
            .param("habilidades", "Java", "SQL")
        )
        .andExpect(status().is3xxRedirection())
        .andReturn();

    MockHttpSession session = (MockHttpSession) resultado.getRequest().getSession(false);
    assertThat(session.getAttribute("ROL"), is("CANDIDATO"));
    Usuario usuario = servicioLogin.consultarUsuario(email, "secreto");
    assertThat(usuario.getRol(), is("CANDIDATO"));

    String sessionIdBeforeLogin = session.getId();
    this.mockMvc.perform(
        post("/validar-login").session(session).param("email", email).param("password", "secreto")
      )
      .andExpect(status().is3xxRedirection());
    org.junit.jupiter.api.Assertions.assertNotEquals(sessionIdBeforeLogin, session.getId());
    assertThat(session.getAttribute("ROL"), is("CANDIDATO"));
  }
}
