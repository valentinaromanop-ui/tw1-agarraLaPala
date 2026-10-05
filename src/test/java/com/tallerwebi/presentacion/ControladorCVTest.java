package com.tallerwebi.presentacion;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioCV;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.ModelAndView;

public class ControladorCVTest {

  private ControladorCV controladorCV;
  private ServicioCV servicioCVMOck;

  @BeforeEach
  public void init() {
    servicioCVMOck = mock(ServicioCV.class);
    controladorCV = new ControladorCV(servicioCVMOck);
  }

  @Test
  public void SubirCVConFormatoValidoDeberiaVolverAHomeConMensajeDeExito() {
    when(servicioCVMOck.esFormatoValido(anyString())).thenReturn(true);
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      "contenido".getBytes()
    );
    ModelAndView modelAndView = controladorCV.subirCV(archivo);
    MatcherAssert.assertThat(modelAndView.getViewName(), Matchers.equalToIgnoringCase("home"));
    MatcherAssert.assertThat(
      modelAndView.getModel().get("mensaje").toString(),
      Matchers.equalToIgnoringCase("Archivo subido correctamente")
    );
    verify(servicioCVMOck, times(1)).esFormatoValido("miCV.pdf"); // Sirve para asegurarte de que el controlador realmente consulta al servicio y no responde a ciegas.
  }
}
