package com.tallerwebi.integracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.config.CorreoConfig;
import com.tallerwebi.dominio.ServicioLogin;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
  classes = { SpringWebTestConfig.class, HibernateTestConfig.class, CorreoConfig.class }
)
class ControladorIntegCVTest {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private ServicioLogin servicioLogin;

  private MockMvc mockMvc;

  @BeforeEach
  void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  void generarCvSinSesionRedirigeAlLogin() throws Exception {
    MockMultipartFile archivo = new MockMultipartFile(
      "archivo",
      "miCV.pdf",
      "application/pdf",
      "%PDF-1.7".getBytes()
    );

    mockMvc.perform(multipart("/cv/generar").file(archivo)).andExpect(status().is3xxRedirection());
  }

  @Test
  void descargarCvSinSesionEsRechazado() throws Exception {
    mockMvc.perform(get("/cv/descargar")).andExpect(status().isUnauthorized());
  }

  @Test
  void perfilMuestraLaSeccionDelCvParaUnPostulante() throws Exception {
    Usuario postulante = new Usuario();
    postulante.setEmail("perfil-" + UUID.randomUUID() + "@correo.com");
    postulante.setPassword("secreto");
    postulante.setRol("CANDIDATO");
    servicioLogin.registrar(postulante);

    MockHttpServletRequestBuilder solicitud = get("/perfil")
      .sessionAttr("ROL", "CANDIDATO")
      .sessionAttr("USUARIO_ID", postulante.getId());

    mockMvc.perform(solicitud).andExpect(status().isOk());
  }

  @Test
  void perfilConUsuarioInexistenteRedirigeAlLogin() throws Exception {
    mockMvc
      .perform(get("/perfil").sessionAttr("ROL", "CANDIDATO").sessionAttr("USUARIO_ID", -1L))
      .andExpect(status().is3xxRedirection())
      .andExpect(
        org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/login")
      );
  }
}
