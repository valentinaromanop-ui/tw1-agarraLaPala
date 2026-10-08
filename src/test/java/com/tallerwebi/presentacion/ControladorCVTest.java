package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.ArchivoCvInvalidoException;
import com.tallerwebi.dominio.CurriculumGenerado;
import com.tallerwebi.dominio.ServicioCurriculumGenerado;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ControladorCVTest {

  private ControladorCV controladorCV;
  private ServicioCurriculumGenerado servicioCurriculum;
  private MockHttpSession session;

  @BeforeEach
  void init() {
    servicioCurriculum = mock(ServicioCurriculumGenerado.class);
    controladorCV = new ControladorCV(servicioCurriculum);
    session = new MockHttpSession();
    session.setAttribute("USUARIO_ID", 1L);
    session.setAttribute("ROL", "CANDIDATO");
  }

  @Test
  void subirOriginalLoGuardaSinGenerarAtsTodavia() throws Exception {
    MockMultipartFile archivo = archivo();

    ModelAndView respuesta = controladorCV.subirOriginal(
      archivo,
      null,
      session,
      new RedirectAttributesModelMap()
    );

    assertThat(respuesta.getViewName(), equalTo("redirect:/perfil?cvSubido=true"));
    verify(servicioCurriculum).subirOriginal(1L, "miCV.pdf", archivo.getBytes());
  }

  @Test
  void requiereSesionDePostulante() throws Exception {
    MockHttpSession sesionSinUsuario = new MockHttpSession();
    RedirectAttributesModelMap atributos = new RedirectAttributesModelMap();

    ModelAndView respuesta = controladorCV.subirOriginal(
      archivo(),
      null,
      sesionSinUsuario,
      atributos
    );

    assertThat(respuesta.getViewName(), equalTo("redirect:/home"));
    assertThat(
      String.valueOf(atributos.getFlashAttributes().get("errorCV")),
      containsString("No pude verificar tu sesión")
    );
  }

  @Test
  void noRedirigeAlLoginSiLaSesionPerteneceAUnaCuentaNoPostulante() throws Exception {
    MockHttpSession sesionEmpresa = new MockHttpSession();
    sesionEmpresa.setAttribute("USUARIO_ID", 1L);
    sesionEmpresa.setAttribute("ROL", "EMPLEADOR");
    RedirectAttributesModelMap atributos = new RedirectAttributesModelMap();

    ModelAndView respuesta = controladorCV.subirOriginal(archivo(), null, sesionEmpresa, atributos);

    assertThat(respuesta.getViewName(), equalTo("redirect:/home"));
    assertThat(
      atributos.getFlashAttributes().get("errorCV"),
      equalTo("La generación de CV está disponible para cuentas postulantes.")
    );
  }

  @Test
  void muestraUnErrorYRedirigeSiElArchivoEsInvalido() throws Exception {
    when(servicioCurriculum.subirOriginal(1L, "miCV.pdf", new byte[] { 1 }))
      .thenThrow(new ArchivoCvInvalidoException());
    RedirectAttributesModelMap atributos = new RedirectAttributesModelMap();

    ModelAndView respuesta = controladorCV.subirOriginal(archivo(), null, session, atributos);

    assertThat(respuesta.getViewName(), equalTo("redirect:/home"));
    assertThat(
      atributos.getFlashAttributes().get("errorCV"),
      equalTo("El archivo no es un documento PDF, DOC o DOCX válido.")
    );
  }

  @Test
  void aceptaElNombreDeCampoAnteriorDelFormulario() throws Exception {
    MockMultipartFile cvAnterior = new MockMultipartFile(
      "cv",
      "miCV.pdf",
      "application/pdf",
      new byte[] { 1 }
    );

    ModelAndView respuesta = controladorCV.subirOriginal(
      null,
      cvAnterior,
      session,
      new RedirectAttributesModelMap()
    );

    assertThat(respuesta.getViewName(), equalTo("redirect:/perfil?cvSubido=true"));
    verify(servicioCurriculum).subirOriginal(1L, "miCV.pdf", cvAnterior.getBytes());
  }

  @Test
  void generarAtsUsaElOriginalGuardadoYRedirigeAlPerfil() {
    ModelAndView respuesta = controladorCV.generarAts(session, new RedirectAttributesModelMap());

    assertThat(respuesta.getViewName(), equalTo("redirect:/perfil?cvGenerado=true"));
    verify(servicioCurriculum).generarAts(1L);
  }

  @Test
  void descargaYVisualizacionDevuelvenElPdfAtsSinLimite() {
    CurriculumGenerado cv = new CurriculumGenerado();
    cv.setArchivoAtsPdf(new byte[] { 37, 80, 68, 70, 45 });
    cv.setNombreArchivoOriginal("cv-original.pdf");
    cv.setArchivoOriginal(new byte[] { 37, 80, 68, 70, 45 });
    when(servicioCurriculum.obtener(1L)).thenReturn(cv);

    var respuesta = controladorCV.descargar(session);

    assertThat(respuesta.getStatusCode().value(), equalTo(200));
    assertThat(respuesta.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
    assertThat(
      respuesta.getHeaders().getFirst("Content-Disposition"),
      containsString("attachment")
    );
    assertThat(respuesta.getBody()[0], is((byte) 37));
    var visualizacion = controladorCV.verAts(session);
    assertThat(
      visualizacion.getHeaders().getFirst("Content-Disposition"),
      containsString("inline")
    );
    assertThat(visualizacion.getBody()[0], is((byte) 37));
    verify(servicioCurriculum, org.mockito.Mockito.times(2)).obtener(1L);
  }

  @Test
  void muestraYPermiteDescargarElPdfOriginalSubido() {
    CurriculumGenerado cv = new CurriculumGenerado();
    cv.setNombreArchivoOriginal("original.pdf");
    cv.setArchivoOriginal(new byte[] { 37, 80, 68, 70, 45 });
    when(servicioCurriculum.obtener(1L)).thenReturn(cv);

    var visualizacion = controladorCV.verOriginal(session);
    var descarga = controladorCV.descargarOriginal(session);

    assertThat(visualizacion.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
    assertThat(
      visualizacion.getHeaders().getFirst("Content-Disposition"),
      containsString("inline")
    );
    assertThat(descarga.getHeaders().getFirst("Content-Disposition"), containsString("attachment"));
    assertThat(descarga.getBody()[0], is((byte) 37));
  }

  @Test
  void descargarRequiereSesionDePostulante() throws Exception {
    HttpSession sinSesion = null;

    var respuesta = controladorCV.descargar(sinSesion);

    assertThat(respuesta.getStatusCode().value(), equalTo(401));
  }

  private MockMultipartFile archivo() {
    return new MockMultipartFile("archivo", "miCV.pdf", "application/pdf", new byte[] { 1 });
  }
}
