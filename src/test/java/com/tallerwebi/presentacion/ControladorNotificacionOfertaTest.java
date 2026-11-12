package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tallerwebi.dominio.EstadoInvitacion;
import com.tallerwebi.dominio.Invitacion;
import com.tallerwebi.dominio.NotificacionOferta;
import com.tallerwebi.dominio.ServicioInvitacion;
import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.ServicioNotificacionOferta;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.Vacante;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

class ControladorNotificacionOfertaTest {

  @Test
  void notificacionesRenderizaLaListaYLasInvitaciones() throws Exception {
    ServicioNotificacionOferta servicioNotificaciones = mock(ServicioNotificacionOferta.class);
    ServicioInvitacion servicioInvitacion = mock(ServicioInvitacion.class);
    ServicioMensaje servicioMensaje = mock(ServicioMensaje.class);
    Usuario empleador = new Usuario();
    empleador.setEmpresa("Empresa Ejemplo");
    Vacante oferta = new Vacante();
    oferta.setId(5L);
    oferta.setTitulo("Desarrollador Java");
    Invitacion invitacion = new Invitacion();
    invitacion.setId(7L);
    invitacion.setEmpleador(empleador);
    invitacion.setOferta(oferta);
    invitacion.setEstado(EstadoInvitacion.PENDIENTE);
    NotificacionOferta notificacion = new NotificacionOferta();
    notificacion.setId(8L);
    notificacion.setMensaje(
      "La empresa Empresa Ejemplo te invitó a postularte a Desarrollador Java"
    );
    notificacion.setEnlace("/vacantes/detalle?fuente=LOCAL&id=5");
    when(servicioNotificaciones.listar(20L)).thenReturn(List.of(notificacion));
    when(servicioInvitacion.listarParaCandidato(20L)).thenReturn(List.of(invitacion));
    ControladorNotificacionOferta controlador = new ControladorNotificacionOferta(
      servicioNotificaciones
    );
    controlador.setServicioInvitacion(servicioInvitacion);
    controlador.setServicioMensaje(servicioMensaje);

    FileTemplateResolver templates = new FileTemplateResolver();
    templates.setPrefix("src/main/webapp/WEB-INF/views/thymeleaf/");
    templates.setSuffix(".html");
    templates.setCharacterEncoding("UTF-8");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(templates);
    ThymeleafViewResolver vistas = new ThymeleafViewResolver();
    vistas.setTemplateEngine(engine);
    vistas.setCharacterEncoding("UTF-8");
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "CANDIDATO");
    session.setAttribute("USUARIO_ID", 20L);

    String html = MockMvcBuilders
      .standaloneSetup(controlador)
      .setViewResolvers(vistas)
      .build()
      .perform(get("/notificaciones").session(session))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertTrue(html.contains("Invitaciones a postularte"));
    assertTrue(html.contains("Descartar invitación"));
    assertTrue(html.contains("Desarrollador Java"));
    assertTrue(html.contains("Empresa Ejemplo"));
  }

  @Test
  void contadorDevuelveLaSumaDeAvisosYMensajesNoLeidos() throws Exception {
    ServicioNotificacionOferta servicioNotificaciones = mock(ServicioNotificacionOferta.class);
    ServicioInvitacion servicioInvitacion = mock(ServicioInvitacion.class);
    ServicioMensaje servicioMensaje = mock(ServicioMensaje.class);
    when(servicioNotificaciones.contarNoLeidas(20L)).thenReturn(2L);
    when(servicioMensaje.listarNoLeidos(20L))
      .thenReturn(List.of(mock(com.tallerwebi.dominio.NotificacionMensaje.class)));
    ControladorNotificacionOferta controlador = new ControladorNotificacionOferta(
      servicioNotificaciones
    );
    controlador.setServicioInvitacion(servicioInvitacion);
    controlador.setServicioMensaje(servicioMensaje);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "CANDIDATO");
    session.setAttribute("USUARIO_ID", 20L);

    var respuesta = MockMvcBuilders
      .standaloneSetup(controlador)
      .build()
      .perform(get("/notificaciones/contador").session(session))
      .andReturn()
      .getResponse();

    assertTrue(respuesta.getContentAsString().contains("\"count\":3"));
  }

  @Test
  void contadorTambienIncluyeLosMensajesDeUnEmpleador() throws Exception {
    ServicioNotificacionOferta servicioNotificaciones = mock(ServicioNotificacionOferta.class);
    ServicioInvitacion servicioInvitacion = mock(ServicioInvitacion.class);
    ServicioMensaje servicioMensaje = mock(ServicioMensaje.class);
    when(servicioMensaje.listarNoLeidos(42L))
      .thenReturn(List.of(mock(com.tallerwebi.dominio.NotificacionMensaje.class)));
    ControladorNotificacionOferta controlador = new ControladorNotificacionOferta(
      servicioNotificaciones
    );
    controlador.setServicioInvitacion(servicioInvitacion);
    controlador.setServicioMensaje(servicioMensaje);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "EMPLEADOR");
    session.setAttribute("USUARIO_ID", 42L);

    var respuesta = MockMvcBuilders
      .standaloneSetup(controlador)
      .build()
      .perform(get("/notificaciones/contador").session(session))
      .andReturn()
      .getResponse();

    assertTrue(respuesta.getContentAsString().contains("\"count\":1"));
  }

  @Test
  void abrirElCentroMarcaMensajesYAlertasComoLeidos() throws Exception {
    ServicioNotificacionOferta servicioNotificaciones = mock(ServicioNotificacionOferta.class);
    ServicioInvitacion servicioInvitacion = mock(ServicioInvitacion.class);
    ServicioMensaje servicioMensaje = mock(ServicioMensaje.class);
    ControladorNotificacionOferta controlador = new ControladorNotificacionOferta(
      servicioNotificaciones
    );
    controlador.setServicioInvitacion(servicioInvitacion);
    controlador.setServicioMensaje(servicioMensaje);
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("ROL", "CANDIDATO");
    session.setAttribute("USUARIO_ID", 20L);

    MockMvcBuilders
      .standaloneSetup(controlador)
      .build()
      .perform(post("/notificaciones/marcar-leidas").session(session))
      .andExpect(status().isNoContent());

    verify(servicioMensaje).marcarTodosLeidos(20L);
    verify(servicioNotificaciones).marcarTodasLeidas(20L);
  }
}
