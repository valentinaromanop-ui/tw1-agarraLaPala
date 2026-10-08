package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
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
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
public class ControladorOfertaEmpleadorIntegracionTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private ServicioLogin servicioLogin;

  private MockMvc mockMvc;

  @BeforeEach
  public void iniciar() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  public void empleadorPuedeCrearOfertaYVerlaEnSuListado() throws Exception {
    MockHttpSession session = crearSesionEmpleador();

    MvcResult formulario = mockMvc
      .perform(get("/crear-postulacion").session(session))
      .andExpect(status().isOk())
      .andReturn();
    assertThat(formulario.getResponse().getContentAsString(), containsString("Título del cargo"));

    MvcResult listaVacia = mockMvc
      .perform(get("/busquedas-Empleador").session(session))
      .andExpect(status().isOk())
      .andReturn();
    assertThat(listaVacia.getResponse().getContentAsString(), containsString("Para empresas"));
    assertThat(
      listaVacia.getResponse().getContentAsString(),
      containsString("Mis búsquedas activas")
    );
    assertThat(
      listaVacia.getResponse().getContentAsString(),
      containsString("Todavía no creaste postulaciones")
    );

    mockMvc
      .perform(
        post("/crear-postulacion")
          .session(session)
          .param("titulo", "Analista de Integración")
          .param("descripcion", "Integración de servicios.")
          .param("ubicacion", "Buenos Aires")
          .param("modalidad", "híbrido")
          .param("horario", "Lunes a viernes")
          .param("salario", "1150000.50")
          .param("skills", "Java, SQL")
      )
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/busquedas-Empleador?publicada=true"));

    MvcResult listado = mockMvc
      .perform(get("/busquedas-Empleador").param("publicada", "true").session(session))
      .andExpect(status().isOk())
      .andReturn();
    String contenidoListado = listado.getResponse().getContentAsString();
    assertThat(contenidoListado, containsString("Analista de Integración"));
    assertThat(contenidoListado, containsString("Buenos Aires"));
    assertThat(contenidoListado, containsString("1150000.50"));
    assertThat(contenidoListado, containsString("La postulación se publicó correctamente."));

    MvcResult home = mockMvc
      .perform(get("/home").session(session))
      .andExpect(status().isOk())
      .andReturn();
    String contenidoHome = home.getResponse().getContentAsString();
    assertThat(contenidoHome, containsString("Product Designer"));
    assertThat(contenidoHome, containsString("Analista de Integración"));
    assertThat(contenidoHome, containsString("Mostrando 11 ofertas"));
    assertThat(
      contenidoHome,
      containsString("data-oferta-id=\"" + ofertaIdDeListado(contenidoHome) + "\"")
    );

    mockMvc
      .perform(get("/vacantes").param("texto", "Analista de Integración").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].titulo").value("Analista de Integración"))
      .andExpect(jsonPath("$[0].empresa").value("Empresa de prueba"))
      .andExpect(jsonPath("$[0].salario").value(1150000.50))
      .andExpect(jsonPath("$[0].id").isNumber());

    mockMvc
      .perform(get("/vacantes/sugerencias").param("q", "Analista de Inte").session(session))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0]").value("Analista de Integración"));
  }

  private String ofertaIdDeListado(String contenidoHome) {
    int inicioTitulo = contenidoHome.indexOf("Analista de Integración");
    int inicioId = contenidoHome.lastIndexOf("data-oferta-id=\"", inicioTitulo);
    int inicioValor = inicioId + "data-oferta-id=\"".length();
    int finValor = contenidoHome.indexOf('"', inicioValor);
    return contenidoHome.substring(inicioValor, finValor);
  }

  @Test
  public void usuarioSinRolEmpleadorDebeSerEnviadoAlLogin() throws Exception {
    Usuario candidato = new Usuario();
    candidato.setEmail("candidato-" + UUID.randomUUID() + "@correo.com");
    candidato.setPassword("secreto");
    candidato.setRol("CANDIDATO");
    servicioLogin.registrar(candidato);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", candidato.getId());
    session.setAttribute("ROL", candidato.getRol());

    mockMvc
      .perform(get("/busquedas-Empleador").session(session))
      .andExpect(status().is3xxRedirection())
      .andExpect(redirectedUrl("/login"));
  }

  private MockHttpSession crearSesionEmpleador() throws Exception {
    Usuario empleador = new Usuario();
    empleador.setEmail("empleador-" + UUID.randomUUID() + "@empresa.com");
    empleador.setPassword("secreto");
    empleador.setRol("EMPLEADOR");
    empleador.setEmpresa("Empresa de prueba");
    empleador.setLegajo("12345");
    servicioLogin.registrar(empleador);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", empleador.getId());
    session.setAttribute("ROL", empleador.getRol());
    return session;
  }
}
