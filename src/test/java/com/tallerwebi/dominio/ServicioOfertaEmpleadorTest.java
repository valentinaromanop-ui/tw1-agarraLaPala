package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ServicioOfertaEmpleadorTest {

  private RepositorioUsuario repositorioUsuario;
  private RepositorioVacante repositorioVacante;
  private RepositorioPostulacion repositorioPostulacion;
  private ServicioPerfil servicioPerfil;
  private ServicioPago servicioPago;
  private ServicioNotificacionOferta servicioNotificacionOferta;
  private ApplicationEventPublisher eventos;
  private ServicioOfertaEmpleadorImpl servicio;
  private Vacante vacante;

  @BeforeEach
  void iniciar() {
    repositorioUsuario = mock(RepositorioUsuario.class);
    repositorioVacante = mock(RepositorioVacante.class);
    repositorioPostulacion = mock(RepositorioPostulacion.class);
    servicioPerfil = mock(ServicioPerfil.class);
    servicioPago = mock(ServicioPago.class);
    servicioNotificacionOferta = mock(ServicioNotificacionOferta.class);
    eventos = mock(ApplicationEventPublisher.class);
    servicio = crearServicio(eventos);
    vacante = new Vacante();
    vacante.setId(10L);
    vacante.setTitulo("Desarrollador Java");
    vacante.setEmpresa("Empresa de prueba");
    cargarSkills(List.of("Java", "SQL", "Docker", "Git"));
  }

  @Test
  void empleadorPuedeCerrarSuVacanteAbierta() {
    vacante.setEmpleadorId(42L);
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(vacante);

    servicio.cerrar(10L, 42L);

    assertEquals(EstadoVacante.CERRADA, vacante.getEstado());
    verify(repositorioVacante).obtenerPorIdParaActualizar(10L);
  }

  @Test
  void empleadorNoPuedeCerrarVacanteAjena() {
    vacante.setEmpleadorId(99L);
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(vacante);

    assertThrows(AccesoOfertaDenegadoException.class, () -> servicio.cerrar(10L, 42L));
    assertEquals(EstadoVacante.ABIERTA, vacante.getEstado());
  }

  @Test
  void noSePuedeCerrarUnaVacanteQueYaEstaCerrada() {
    vacante.setEmpleadorId(42L);
    vacante.setEstado(EstadoVacante.CERRADA);
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(vacante);

    assertThrows(OfertaCerradaException.class, () -> servicio.cerrar(10L, 42L));
  }

  @Test
  void cerrarUnaVacanteInexistenteFalla() {
    when(repositorioVacante.obtenerPorIdParaActualizar(10L)).thenReturn(null);

    assertThrows(VacanteNoEncontradaException.class, () -> servicio.cerrar(10L, 42L));
  }

  @Test
  void avisaSoloConAlMenosEl75PorCiento() {
    List<Object> correos = new ArrayList<>();
    servicio = crearServicio(correos::add);
    Usuario uno = crearUsuario(1L, "uno@gmail.com");
    Usuario dos = crearUsuario(2L, "dos@gmail.com");
    Usuario tres = crearUsuario(3L, "tres@gmail.com");
    when(repositorioUsuario.buscarPostulantes()).thenReturn(List.of(uno, dos, tres));
    when(servicioPerfil.obtenerHabilidades(1L)).thenReturn(List.of("Java", "JAVA", "SQL"));
    when(servicioPerfil.obtenerHabilidades(2L)).thenReturn(List.of("Java", "SQL", "Docker"));
    when(servicioPerfil.obtenerHabilidades(3L)).thenReturn(List.of("Java", "SQL", "Docker", "Git"));

    servicio.publicar(vacante);

    verify(repositorioVacante).guardar(vacante);
    Correo correo = (Correo) correos.getFirst();
    assertThat(correo.getDestinatarios(), containsInAnyOrder("dos@gmail.com", "tres@gmail.com"));
    assertThat(correo.getMensaje(), containsString("fuente=LOCAL&id=10"));
  }

  @Test
  void dosDeTresSkillsNoAlcanzanEl75PorCiento() {
    cargarSkills(List.of("Java", "SQL", "Docker"));
    when(repositorioUsuario.buscarPostulantes())
      .thenReturn(List.of(crearUsuario(1L, "uno@gmail.com")));
    when(servicioPerfil.obtenerHabilidades(1L)).thenReturn(List.of("Java", "SQL"));

    servicio.publicar(vacante);

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  void unaSkillCoincidenteAlcanzaSiEsElUnicoRequisito() {
    cargarSkills(List.of("Java"));
    when(repositorioUsuario.buscarPostulantes())
      .thenReturn(List.of(crearUsuario(1L, "uno@gmail.com")));
    when(servicioPerfil.obtenerHabilidades(1L)).thenReturn(List.of("Java", "SQL"));

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
    when(repositorioUsuario.buscarPostulantes())
      .thenReturn(List.of(crearUsuario(1L, "uno@gmail.com")));
    when(servicioPerfil.obtenerHabilidades(1L)).thenReturn(List.of("PHP"));

    servicio.publicar(vacante);

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  void notificaAQuienesTienenPremiumYUnaSkillCoincidente() {
    Usuario usuario = crearUsuario(1L, "premium@gmail.com");
    when(repositorioUsuario.buscarPostulantes()).thenReturn(List.of(usuario));
    when(servicioPerfil.obtenerHabilidades(1L)).thenReturn(List.of("java"));
    when(servicioPago.esPremiumActivo(1L)).thenReturn(true);

    servicio.publicar(vacante);

    verify(servicioNotificacionOferta)
      .notificar(
        usuario,
        "Nueva oferta acorde a tus habilidades: Desarrollador Java",
        "/vacantes/detalle?fuente=LOCAL&id=10"
      );
  }

  @Test
  void noAvisaSiFallaElGuardado() {
    doThrow(new IllegalStateException()).when(repositorioVacante).guardar(vacante);

    assertThrows(IllegalStateException.class, () -> servicio.publicar(vacante));

    verify(eventos, never()).publishEvent(any(Object.class));
  }

  @Test
  void resumenCuentaBusquedasYPostulacionesDeTodasLasVacantes() {
    Vacante activa = new Vacante();
    activa.setId(1L);
    Vacante cerrada = new Vacante();
    cerrada.setId(2L);
    cerrada.setEstado(EstadoVacante.CERRADA);
    when(repositorioVacante.buscarPorEmpleador(42L)).thenReturn(List.of(activa, cerrada));
    when(repositorioPostulacion.contarPorVacante(1L)).thenReturn(3L);
    when(repositorioPostulacion.contarPorVacante(2L)).thenReturn(2L);

    ResumenEmpleador resumen = servicio.obtenerResumen(42L);

    assertEquals(1L, resumen.getBusquedasActivas());
    assertEquals(1L, resumen.getBusquedasCerradas());
    assertEquals(5L, resumen.getPostulacionesRecibidas());
  }

  @Test
  void ofertasPremiumAparecenAntesYSeMarcanConLaInsignia() {
    Vacante normal = new Vacante();
    normal.setEmpleadorId(1L);
    normal.setFechaPublicacion(java.time.LocalDateTime.now());
    Vacante premium = new Vacante();
    premium.setEmpleadorId(2L);
    premium.setFechaPublicacion(java.time.LocalDateTime.now().minusDays(3));
    when(repositorioVacante.buscarOfertasPublicadasPorEmpleadores())
      .thenReturn(List.of(normal, premium));
    when(servicioPago.esPremiumActivo(2L)).thenReturn(true);

    List<Vacante> ofertas = servicio.listarOfertasPublicadas();

    assertEquals(premium, ofertas.getFirst());
    assertEquals(true, premium.isEmpresaPremium());
    assertEquals(false, normal.isEmpresaPremium());
  }

  @Test
  void limitaOfertasActivasGratuitasATres() {
    vacante.setEmpleadorId(42L);
    when(repositorioVacante.buscarPorEmpleador(42L))
      .thenReturn(List.of(new Vacante(), new Vacante(), new Vacante()));
    when(servicioPago.esPremiumActivo(42L)).thenReturn(false);

    LimiteOfertasAlcanzadoException excepcion = assertThrows(
      LimiteOfertasAlcanzadoException.class,
      () -> servicio.publicar(vacante)
    );

    assertThat(excepcion.getMessage(), containsString("3 ofertas activas"));
    verify(repositorioVacante, never()).guardar(vacante);
  }

  @Test
  void permiteHastaQuinceOfertasActivasPremium() {
    vacante.setEmpleadorId(42L);
    when(repositorioVacante.buscarPorEmpleador(42L))
      .thenReturn(crearOfertas(ServicioOfertaEmpleadorImpl.LIMITE_OFERTAS_PREMIUM - 1));
    when(servicioPago.esPremiumActivo(42L)).thenReturn(true);

    servicio.publicar(vacante);

    verify(repositorioVacante).guardar(vacante);
  }

  private ServicioOfertaEmpleadorImpl crearServicio(ApplicationEventPublisher publicador) {
    return new ServicioOfertaEmpleadorImpl(
      repositorioUsuario,
      repositorioVacante,
      repositorioPostulacion,
      servicioPerfil,
      servicioPago,
      servicioNotificacionOferta,
      publicador,
      "http://localhost:8080"
    );
  }

  private List<Vacante> crearOfertas(int cantidad) {
    List<Vacante> ofertas = new ArrayList<>();
    for (int indice = 0; indice < cantidad; indice++) {
      ofertas.add(new Vacante());
    }
    return ofertas;
  }

  private Usuario crearUsuario(Long id, String email) {
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
