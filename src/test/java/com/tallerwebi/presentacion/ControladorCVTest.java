package com.tallerwebi.presentacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ServicioCV;
import com.tallerwebi.dominio.ServicioIA;
import com.tallerwebi.dominio.ServicioPerfil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.ModelAndView;

public class ControladorCVTest {

  private ControladorCV controladorCV;
  private ServicioCV servicioCVMOck;
  private ServicioPerfil servicioPerfilMock;
  private ServicioIA servicioIAMock;
  private MockHttpServletRequest request;

  @BeforeEach
  public void init() {
    servicioCVMOck = mock(ServicioCV.class);
    servicioPerfilMock = mock(ServicioPerfil.class);
    servicioIAMock = mock(ServicioIA.class);
    controladorCV = new ControladorCV(servicioCVMOck, servicioPerfilMock, servicioIAMock);
    request = new MockHttpServletRequest();
  }

  @Test
  public void SubirCVConFormatoValidoDeberiaVolverAHomeConMensajeDeExito() throws Exception {
    when(servicioCVMOck.esArchivoValido(eq("miCV.pdf"), any(byte[].class))).thenReturn(true);
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      "contenido".getBytes()
    );
    ModelAndView modelAndView = controladorCV.subirCV(archivo, request);
    MatcherAssert.assertThat(modelAndView.getViewName(), Matchers.equalToIgnoringCase("home"));
    MatcherAssert.assertThat(
      modelAndView.getModel().get("mensaje").toString(),
      Matchers.equalToIgnoringCase("Archivo subido correctamente")
    );
  }

  @Test
  public void SubirCVConFormatoInvalidoDeberiaDevolverMensajeDeError() throws Exception {
    when(servicioCVMOck.esArchivoValido(eq("archivo.exe"), any(byte[].class))).thenReturn(false);
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "archivo.exe",
      "application/pdf",
      "contenido".getBytes()
    );
    ModelAndView modelAndView = controladorCV.subirCV(archivo, request);
    MatcherAssert.assertThat(modelAndView.getViewName(), Matchers.equalToIgnoringCase("home"));
    MatcherAssert.assertThat(
      modelAndView.getModel().get("error").toString(),
      Matchers.equalToIgnoringCase("Formato de archivo no válido")
    );
  }

  @Test
  public void subirCVValidoDeberiaMostrarElCVGeneradoPorLaIA() throws Exception {
    when(servicioCVMOck.esArchivoValido(eq("miCV.pdf"), any(byte[].class))).thenReturn(true);
    when(servicioCVMOck.extraerTexto(any(byte[].class))).thenReturn("texto del cv");
    when(servicioIAMock.generarCvAts("texto del cv", "Sin habilidades adicionales"))
      .thenReturn("CV ATS");
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      "x".getBytes()
    );

    ModelAndView modelAndView = controladorCV.subirCV(archivo, request);

    MatcherAssert.assertThat(
      modelAndView.getModel().get("cvProcesado"),
      Matchers.equalTo("CV ATS")
    );
  }

  @Test
  public void subirCVSiLaIAFallaDeberiaMostrarMensajeDeError() throws Exception {
    when(servicioCVMOck.esArchivoValido(eq("miCV.pdf"), any(byte[].class))).thenReturn(true);
    when(servicioCVMOck.extraerTexto(any(byte[].class))).thenReturn("texto");
    when(servicioIAMock.generarCvAts(any(), any())).thenThrow(new IllegalStateException("falla"));
    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      "x".getBytes()
    );

    ModelAndView modelAndView = controladorCV.subirCV(archivo, request);

    MatcherAssert.assertThat(
      modelAndView.getModel().get("error").toString(),
      Matchers.containsString("No se pudo generar el CV")
    );
  }

  @Test
  public void deberiaDevolverCVConSkillsCargadasEnPerfil() throws Exception {
    when(servicioCVMOck.esArchivoValido(eq("miCV.pdf"), any(byte[].class))).thenReturn(true);
    when(servicioCVMOck.extraerTexto(any(byte[].class))).thenReturn("texto del cv");
    when(servicioIAMock.generarCvAts("texto del cv", "Java, SQL")).thenReturn("CV ATS con skills");
    request.getSession().setAttribute("USUARIO_ID", 1L);
    when(servicioPerfilMock.obtenerPerfil(1L))
      .thenReturn(
        new com.tallerwebi.dominio.PerfilPostulante() {
          {
            getPostulanteSkills()
              .add(
                new com.tallerwebi.dominio.PostulanteSkill() {
                  {
                    setSkill(
                      new com.tallerwebi.dominio.Skill() {
                        {
                          setNombre("Java");
                        }
                      }
                    );
                  }
                }
              );
            getPostulanteSkills()
              .add(
                new com.tallerwebi.dominio.PostulanteSkill() {
                  {
                    setSkill(
                      new com.tallerwebi.dominio.Skill() {
                        {
                          setNombre("SQL");
                        }
                      }
                    );
                  }
                }
              );
          }
        }
      );

    MockMultipartFile archivo = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      "x".getBytes()
    );

    ModelAndView modelAndView = controladorCV.subirCV(archivo, request);

    MatcherAssert.assertThat(
      modelAndView.getModel().get("cvProcesado"),
      Matchers.equalTo("CV ATS con skills")
    );
  }
}
