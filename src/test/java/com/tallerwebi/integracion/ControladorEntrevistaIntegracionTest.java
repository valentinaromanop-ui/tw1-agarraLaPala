package com.tallerwebi.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.config.CorreoConfig;
import com.tallerwebi.dominio.ClienteGoogleCalendar;
import com.tallerwebi.dominio.Entrevista;
import com.tallerwebi.dominio.EventoGoogleCalendar;
import com.tallerwebi.dominio.IntervaloHorario;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.ServicioConexionGoogle;
import com.tallerwebi.dominio.ServicioEntrevista;
import com.tallerwebi.dominio.ServicioIA;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.integracion.config.HibernateTestConfig;
import com.tallerwebi.integracion.config.SpringWebTestConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
  classes = { SpringWebTestConfig.class, HibernateTestConfig.class, CorreoConfig.class }
)
@Transactional
public class ControladorEntrevistaIntegracionTest {

  private static final String ENLACE_MEET = "https://meet.google.com/abc-defg-hij";

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private ServicioEntrevista servicioEntrevista;

  @MockitoBean
  private ClienteGoogleCalendar google;

  @MockitoBean
  private ServicioConexionGoogle conexion;

  @MockitoBean
  private ServicioIA servicioIA;

  private MockMvc mockMvc;
  private Entrevista entrevista;
  private Usuario empleador;
  private Usuario candidato;
  private Vacante vacante;

  @BeforeEach
  public void init() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    Session sesion = sessionFactory.getCurrentSession();
    empleador = guardarUsuario("EMPLEADOR");
    candidato = guardarUsuario("CANDIDATO");

    vacante = new Vacante();
    vacante.setEmpleadorId(empleador.getId());
    vacante.setTitulo("Desarrollador Java");
    sesion.persist(vacante);

    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(candidato);
    postulacion.setIdOferta("LOCAL:" + vacante.getId());
    postulacion.setTitulo(vacante.getTitulo());
    postulacion.setFechaPostulacion(LocalDateTime.now());
    sesion.persist(postulacion);

    entrevista = new Entrevista(postulacion, LocalDateTime.now());
    entrevista.setEmpleadorId(empleador.getId());
    sesion.persist(entrevista);
    when(conexion.obtenerTokenAcceso(empleador.getId())).thenReturn("acceso");
  }

  @Test
  public void confirmarUnTurnoDeberiaCrearYMostrarElMeet() throws Exception {
    // preparacion
    MockHttpSession sesion = crearSesion(candidato);
    String ruta = rutaInvitacion();
    String inicio = servicioEntrevista
      .obtenerTurnosDisponibles(empleador.getId())
      .get(1)
      .getInicio()
      .toString();
    when(
      google.crearEventoConMeet(
        eq("acceso"),
        anyString(),
        anyString(),
        eq(candidato.getEmail()),
        any()
      )
    )
      .thenAnswer(llamada -> new EventoGoogleCalendar(llamada.getArgument(1), ENLACE_MEET));

    mockMvc
      .perform(get(ruta).session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString(inicio)));

    // ejecucion
    mockMvc
      .perform(post(ruta + "/confirmar").session(sesion).param("inicio", inicio))
      .andExpect(redirectedUrl(ruta));

    // validacion
    verify(google)
      .crearEventoConMeet(eq("acceso"), anyString(), anyString(), eq(candidato.getEmail()), any());
    sessionFactory.getCurrentSession().flush();
    sessionFactory.getCurrentSession().clear();
    mockMvc
      .perform(get(ruta).session(sesion))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Entrevista agendada")))
      .andExpect(content().string(containsString(ENLACE_MEET)));
  }

  @Test
  public void laGestionDeberiaMostrarLosDesplegablesYLaCancelacion() throws Exception {
    // preparacion
    var turnos = servicioEntrevista.obtenerTurnosDisponibles(empleador.getId());
    entrevista.agendar(turnos.get(1), new EventoGoogleCalendar("evento", ENLACE_MEET));
    IntervaloHorario otroTurno = turnos.get(2);
    String ruta =
      "/ofertas/" +
      vacante.getId() +
      "/postulantes/" +
      entrevista.getPostulacion().getId() +
      "/entrevista";

    // ejecucion y validacion
    mockMvc
      .perform(get(ruta).session(crearSesion(empleador)))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Guardar nuevo horario")))
      .andExpect(content().string(containsString(otroTurno.getInicio().toString())))
      .andExpect(content().string(containsString(ruta + "/modificar")))
      .andExpect(content().string(containsString("Cancelar entrevista")));
  }

  @Test
  public void elPostulanteDeberiaVerLaCancelacionSinElSelectorNiElMeet() throws Exception {
    // preparacion
    entrevista.cancelar();

    // ejecucion y validacion
    mockMvc
      .perform(get(rutaInvitacion()).session(crearSesion(candidato)))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("La entrevista fue cancelada")))
      .andExpect(content().string(not(containsString("id=\"seleccionar-turno\""))))
      .andExpect(content().string(not(containsString("Entrar a Google Meet"))));
  }

  private Usuario guardarUsuario(String rol) {
    Usuario usuario = new Usuario();
    usuario.setEmail(UUID.randomUUID() + "@test.com");
    usuario.setPassword("test");
    usuario.setRol(rol);
    sessionFactory.getCurrentSession().persist(usuario);
    return usuario;
  }

  private MockHttpSession crearSesion(Usuario usuario) {
    MockHttpSession sesion = new MockHttpSession();
    sesion.setAttribute("USUARIO_ID", usuario.getId());
    sesion.setAttribute("ROL", usuario.getRol());
    return sesion;
  }

  private String rutaInvitacion() {
    return "/entrevistas/" + entrevista.getPostulacion().getId();
  }
}
