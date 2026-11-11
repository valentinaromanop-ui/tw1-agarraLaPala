package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.config.CorreoConfig;
import com.tallerwebi.dominio.Resena;
import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.ServicioOfertaEmpleador;
import com.tallerwebi.dominio.ServicioPostulacion;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.excepcion.UsuarioExistente;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.util.List;
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

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
  classes = { SpringWebTestConfig.class, HibernateTestConfig.class, CorreoConfig.class }
)
class ControladorResenaIntegracionTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ServicioLogin servicioLogin;

  @Autowired
  private ServicioOfertaEmpleador servicioOfertaEmpleador;

  @Autowired
  private ServicioPostulacion servicioPostulacion;

  private MockMvc mockMvc;

  @BeforeEach
  void iniciar() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  void candidatoPuedeResenarYEmpleadorResponderUnaVez() throws Exception {
    Usuario empleador = registrarUsuario("EMPLEADOR");
    Usuario candidato = registrarUsuario("CANDIDATO");
    MockHttpSession sesionEmpleador = crearSesion(empleador);
    MockHttpSession sesionCandidato = crearSesion(candidato);
    Vacante oferta = new Vacante();
    oferta.setTitulo("Oferta para reseña");
    oferta.setEmpresa(empleador.getEmpresa());
    oferta.setEmpleadorId(empleador.getId());
    oferta.setFuente("LOCAL");
    servicioOfertaEmpleador.publicar(oferta);
    servicioPostulacion.crearPostulacion(candidato.getId(), "LOCAL:" + oferta.getId());

    String rutaEmpresa = "/empresas/" + empleador.getId();
    MvcResult perfilCandidato = mockMvc
      .perform(get(rutaEmpresa).session(sesionCandidato))
      .andExpect(status().isOk())
      .andReturn();
    assertThat(
      perfilCandidato.getResponse().getContentAsString(),
      containsString("Dejá tu reseña")
    );

    mockMvc
      .perform(
        post(rutaEmpresa + "/resenas")
          .session(sesionCandidato)
          .param("puntaje", "5")
          .param("comentario", "Buena experiencia")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(rutaEmpresa));

    MvcResult perfilEmpleador = mockMvc
      .perform(get(rutaEmpresa).session(sesionEmpleador))
      .andExpect(status().isOk())
      .andReturn();
    String contenidoResena = perfilEmpleador.getResponse().getContentAsString();
    assertThat(contenidoResena, containsString("Buena experiencia"));
    assertThat(contenidoResena, containsString("Responder como empresa"));

    Object resenasModel = perfilEmpleador.getModelAndView().getModel().get("resenas");
    if (
      !(resenasModel instanceof List<?> resenas) ||
      resenas.isEmpty() ||
      !(resenas.get(0) instanceof Resena resena)
    ) {
      throw new AssertionError("El perfil de empresa no incluyó la reseña guardada.");
    }
    Long resenaId = resena.getId();
    mockMvc
      .perform(
        post(rutaEmpresa + "/resenas/" + resenaId + "/responder")
          .session(sesionEmpleador)
          .param("respuesta", "Gracias por tu opinión")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl(rutaEmpresa));

    MvcResult perfilConRespuesta = mockMvc
      .perform(get(rutaEmpresa))
      .andExpect(status().isOk())
      .andReturn();
    assertThat(
      perfilConRespuesta.getResponse().getContentAsString(),
      containsString("Gracias por tu opinión")
    );
    assertThat(perfilConRespuesta.getResponse().getContentAsString(), containsString("Reportar"));
  }

  private Usuario registrarUsuario(String rol) throws UsuarioExistente {
    Usuario usuario = new Usuario();
    usuario.setEmail(rol.toLowerCase() + "-" + UUID.randomUUID() + "@empresa.com");
    usuario.setPassword("secreto");
    usuario.setRol(rol);
    usuario.setEmpresa("Empresa " + UUID.randomUUID());
    servicioLogin.registrar(usuario);
    return usuario;
  }

  private MockHttpSession crearSesion(Usuario usuario) {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", usuario.getId());
    session.setAttribute("ROL", usuario.getRol());
    return session;
  }
}
