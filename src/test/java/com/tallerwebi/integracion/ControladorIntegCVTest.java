package com.tallerwebi.integracion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
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
@ContextConfiguration(classes = { SpringWebTestConfig.class, HibernateTestConfig.class })
public class ControladorIntegCVTest {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void alSubirUnCVSeMuestraLaPaginaDeHome() throws Exception {
    // preparacion
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      "%PDF-1.7".getBytes()
    );

    // ejecucion
    MvcResult result =
      this.mockMvc.perform(multipart("/cv").file(archivo)).andExpect(status().isOk()).andReturn();

    // validacion
    ModelAndView modelAndView = result.getModelAndView();
    assert modelAndView != null;
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("home"));
  }

  @Test
  public void rechazaUnArchivoPDFConContenidoQueNoEsPDF() throws Exception {
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "imagen.pdf",
      "application/pdf",
      "esto no es un PDF".getBytes()
    );

    MvcResult result =
      this.mockMvc.perform(multipart("/cv").file(archivo)).andExpect(status().isOk()).andReturn();

    ModelAndView modelAndView = result.getModelAndView();
    assert modelAndView != null;
    assertThat(modelAndView.getViewName(), equalToIgnoringCase("home"));
    assertThat(
      modelAndView.getModel().get("error").toString(),
      equalToIgnoringCase("Formato de archivo no válido")
    );
  }
}
