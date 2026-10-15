package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ServicioOfertaEmpleadorTest {

  private RepositorioUsuario usuarios;
  private RepositorioVacante vacantes;
  private ServicioPerfil perfiles;
  private ApplicationEventPublisher eventos;
  private ServicioOfertaEmpleadorImpl servicio;
  private Vacante vacante;

  @BeforeEach
  void init() {
    usuarios = mock(RepositorioUsuario.class);
    vacantes = mock(RepositorioVacante.class);
    perfiles = mock(ServicioPerfil.class);
    eventos = mock(ApplicationEventPublisher.class);
    servicio =
      new ServicioOfertaEmpleadorImpl(
        usuarios,
        vacantes,
        perfiles,
        eventos,
        "http://localhost:8080/spring"
      );
    vacante = new Vacante();
    vacante.setId(10L);
    vacante.setTitulo("Desarrollador Java");
    vacante.setEmpresa("Empresa de prueba");
    cargarSkills(List.of("Java", "SQL", "Docker", "Git"));
  }

  @Test
  void avisaSoloConAlMenosEl75PorCiento() {
    List<Object> correos = new ArrayList<>();
    servicio =
      new ServicioOfertaEmpleadorImpl(
        usuarios,
        vacantes,
        perfiles,
        correos::add,
        "http://localhost:8080/spring"
      );
    Usuario uno = buildUsuario(1L, "uno@gmail.com");
    Usuario dos = buildUsuario(2L, "dos@gmail.com");
    Usuario tres = buildUsuario(3L, "tres@gmail.com");
    when(usuarios.buscarPostulantes()).thenReturn(List.of(uno, dos, tres));
    when(perfiles.obtenerHabilidades(1L)).thenReturn(List.of("Java", "JAVA", "SQL"));
    when(perfiles.obtenerHabilidades(2L)).thenReturn(List.of("Java", "SQL", "Docker"));
    when(perfiles.obtenerHabilidades(3L)).thenReturn(List.of("Java", "SQL", "Docker", "Git"));

    servicio.publicar(vacante);

    verify(vacantes).guardar(vacante);
    Correo correo = (Correo) correos.getFirst();
    assertThat(correo.getDestinatarios(), containsInAnyOrder("dos@gmail.com", "tres@gmail.com"));
    assertThat(correo.getMensaje(), containsString("fuente=LOCAL&id=10"));
  }

  @Test
  void dosDeTresSkillsNoAlcanzanEl75PorCiento() {
    cargarSkills(List.of("Java", "SQL", "Docker"));
    when(usuarios.buscarPostulantes()).thenReturn(List.of(buildUsuario(1L, "uno@gmail.com")));
    when(perfiles.obtenerHabilidades(1L)).thenReturn(List.of("Java", "SQL"));

    servicio.publicar(vacante);

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  void unaSkillCoincidenteAlcanzaSiEsElUnicoRequisito() {
    cargarSkills(List.of("Java"));
    when(usuarios.buscarPostulantes()).thenReturn(List.of(buildUsuario(1L, "uno@gmail.com")));
    when(perfiles.obtenerHabilidades(1L)).thenReturn(List.of("Java", "SQL"));

    servicio.publicar(vacante);

    verify(eventos).publishEvent(any(Correo.class));
  }

  @Test
  void sinSkillsEnLaOfertaNoEnviaAvisos() {
    vacante.getVacanteSkills().clear();

    servicio.publicar(vacante);

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  void noAvisaSiNadieCoincide() {
    when(usuarios.buscarPostulantes()).thenReturn(List.of(buildUsuario(1L, "uno@gmail.com")));
    when(perfiles.obtenerHabilidades(1L)).thenReturn(List.of("PHP"));

    servicio.publicar(vacante);

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  void noAvisaSiFallaElGuardado() {
    doThrow(new IllegalStateException()).when(vacantes).guardar(vacante);

    assertThrows(IllegalStateException.class, () -> servicio.publicar(vacante));

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  private Usuario buildUsuario(Long id, String email) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setEmail(email);
    return usuario;
  }

  private void cargarSkills(List<String> nombres) {
    vacante.getVacanteSkills().clear();
    for (String nombre : nombres) {
      Skill skill = new Skill();
      skill.setNombre(nombre);
      VacanteSkill relacion = new VacanteSkill();
      relacion.setSkill(skill);
      relacion.setVacante(vacante);
      vacante.getVacanteSkills().add(relacion);
    }
  }
}
