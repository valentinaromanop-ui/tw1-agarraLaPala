package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioCurriculumGeneradoTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioPerfilPostulante repositorioPerfil;
  private RepositorioCurriculumGenerado repositorioCurriculum;
  private ServicioCV servicioCV;
  private ServicioIA servicioIA;
  private GeneradorPdfAts generadorPdf;
  private ServicioCurriculumGeneradoImpl servicio;

  @BeforeEach
  void init() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioPerfil = mock(RepositorioPerfilPostulante.class);
    repositorioCurriculum = mock(RepositorioCurriculumGenerado.class);
    servicioCV = mock(ServicioCV.class);
    servicioIA = mock(ServicioIA.class);
    generadorPdf = mock(GeneradorPdfAts.class);
    servicio =
      new ServicioCurriculumGeneradoImpl(
        repositorioUsuario,
        repositorioPerfil,
        repositorioCurriculum,
        servicioCV,
        servicioIA,
        generadorPdf
      );
  }

  @Test
  void guardaElOriginalSinLlamarALaIa() throws Exception {
    when(repositorioUsuario.buscarPorIdParaActualizar(7L)).thenReturn(postulante());
    when(repositorioCurriculum.buscarPorUsuarioId(7L)).thenReturn(null);
    when(servicioCV.esArchivoValido("original.pdf", new byte[] { 1 })).thenReturn(true);
    when(servicioCV.extraerTexto(new byte[] { 1 })).thenReturn("Experiencia original");
    when(repositorioCurriculum.guardar(any(CurriculumGenerado.class)))
      .thenAnswer(invocacion -> invocacion.getArgument(0));

    CurriculumGenerado original = servicio.subirOriginal(7L, "original.pdf", new byte[] { 1 });

    assertThat(original.getNombreArchivoOriginal(), equalTo("original.pdf"));
    assertThat(original.getArchivoOriginal(), equalTo(new byte[] { 1 }));
    assertThat(original.getTextoOriginal(), equalTo("Experiencia original"));
    assertThat(original.getContenidoAts(), equalTo(null));
    verify(servicioIA, never()).generarCvAts(anyString(), anyString());
  }

  @Test
  void generaUnaUnicaVersionAtsEnPdfDesdeElOriginalGuardado() throws Exception {
    Usuario usuario = postulante();
    usuario.setHabilidades(List.of("Java"));
    CurriculumGenerado original = new CurriculumGenerado();
    original.setTextoOriginal("Experiencia original");
    when(repositorioUsuario.buscarPorIdParaActualizar(7L)).thenReturn(usuario);
    when(repositorioCurriculum.buscarPorUsuarioIdParaActualizar(7L)).thenReturn(original);
    when(repositorioPerfil.buscarPorUsuarioId(7L)).thenReturn(perfilConSkill("SQL"));
    when(servicioIA.generarCvAts("Experiencia original", "Java, SQL")).thenReturn("CV ATS");
    when(generadorPdf.generar("CV ATS")).thenReturn(new byte[] { 37, 80, 68, 70 });
    when(repositorioCurriculum.guardar(original)).thenReturn(original);

    CurriculumGenerado generado = servicio.generarAts(7L);

    assertThat(generado.getContenidoAts(), equalTo("CV ATS"));
    assertThat(generado.getArchivoAtsPdf(), equalTo(new byte[] { 37, 80, 68, 70 }));
    verify(repositorioCurriculum).guardar(original);
  }

  @Test
  void impideGenerarMasDeUnaVersionAts() {
    CurriculumGenerado original = new CurriculumGenerado();
    original.setContenidoAts("Ya generado");
    original.setArchivoAtsPdf(new byte[] { 37, 80, 68, 70 });
    when(repositorioUsuario.buscarPorIdParaActualizar(7L)).thenReturn(postulante());
    when(repositorioCurriculum.buscarPorUsuarioIdParaActualizar(7L)).thenReturn(original);

    assertThrows(CurriculumGratisYaGeneradoException.class, () -> servicio.generarAts(7L));
    verify(servicioIA, never()).generarCvAts(anyString(), anyString());
  }

  @Test
  void noPermiteSubirMasDeUnCvOriginal() throws Exception {
    when(repositorioUsuario.buscarPorIdParaActualizar(7L)).thenReturn(postulante());
    when(repositorioCurriculum.buscarPorUsuarioId(7L)).thenReturn(new CurriculumGenerado());

    assertThrows(
      IllegalStateException.class,
      () -> servicio.subirOriginal(7L, "original.pdf", new byte[] { 1 })
    );
    verify(servicioCV, never()).esArchivoValido(anyString(), any(byte[].class));
  }

  private Usuario postulante() {
    Usuario usuario = new Usuario();
    usuario.setId(7L);
    usuario.setRol("CANDIDATO");
    return usuario;
  }

  private PerfilPostulante perfilConSkill(String nombreSkill) {
    Skill skill = new Skill();
    skill.setNombre(nombreSkill);
    PostulanteSkill postulanteSkill = new PostulanteSkill();
    postulanteSkill.setSkill(skill);
    PerfilPostulante perfil = new PerfilPostulante();
    perfil.setPostulanteSkills(List.of(postulanteSkill));
    return perfil;
  }
}
