package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.Postulacion;
import com.tallerwebi.dominio.ServicioChat;
import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ControladorChat {

  private static final String PATH_VARIABLE_POSTULACION_ID = "postulacionId";
  private static final String RUTA_CONVERSACIONES = "/api/chat/conversaciones";
  private static final DateTimeFormatter FECHA_JSON = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private final ServicioChat servicioChat;
  private final ServicioMensaje servicioMensaje;

  @Autowired
  public ControladorChat(ServicioChat servicioChat, ServicioMensaje servicioMensaje) {
    this.servicioChat = servicioChat;
    this.servicioMensaje = servicioMensaje;
  }

  public ControladorChat(ServicioChat servicioChat) {
    this(servicioChat, null);
  }

  public ControladorChat(ServicioMensaje servicioMensaje) {
    this(null, servicioMensaje);
  }

  @RequestMapping(path = "/chat/{" + PATH_VARIABLE_POSTULACION_ID + "}", method = RequestMethod.GET)
  public ModelAndView verChat(
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpServletRequest request
  ) {
    HttpSession session = request.getSession(false);
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    if (!(id instanceof Number)) {
      return new ModelAndView("redirect:/login");
    }

    Long usuarioId = ((Number) id).longValue();
    Map<String, Object> modelo = new LinkedHashMap<>();
    try {
      modelo.put("mensajes", servicioMensaje.listar(postulacionId, usuarioId));
    } catch (AccesoChatDenegado excepcion) {
      return new ModelAndView("redirect:/home");
    }
    modelo.put(PATH_VARIABLE_POSTULACION_ID, postulacionId);
    modelo.put("usuarioId", usuarioId);
    return new ModelAndView("chat", modelo);
  }

  @RequestMapping(path = RUTA_CONVERSACIONES, method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<?> conversaciones(HttpServletRequest request) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    List<Map<String, Object>> resultado = new ArrayList<>();
    for (Postulacion postulacion : servicioChat.listarConversaciones(usuarioId)) {
      List<Mensaje> mensajes = servicioChat.listarMensajes(usuarioId, postulacion.getId());
      Mensaje ultimo = mensajes.isEmpty() ? null : mensajes.get(mensajes.size() - 1);
      Map<String, Object> conversacion = new LinkedHashMap<>();
      conversacion.put("id", postulacion.getId());
      conversacion.put("puesto", postulacion.getTitulo());
      conversacion.put("otroParticipante", nombreOtro(postulacion, usuarioId));
      conversacion.put("ultimoMensaje", ultimo == null ? "" : ultimo.getContenido());
      conversacion.put(
        "hora",
        formatearFecha(ultimo == null ? postulacion.getFechaPostulacion() : ultimo.getFechaEnvio())
      );
      conversacion.put(
        "noLeidos",
        mensajes
          .stream()
          .filter(m ->
            !usuarioId.equals(m.getRemitente().getId()) && !Boolean.TRUE.equals(m.getLeido())
          )
          .count()
      );
      resultado.add(conversacion);
    }
    return ResponseEntity.ok(resultado);
  }

  @RequestMapping(path = "/api/chat/no-leidos", method = RequestMethod.GET)
  @ResponseBody
  public ResponseEntity<?> noLeidos(HttpServletRequest request) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(Map.of("cantidad", servicioChat.contarNoLeidos(usuarioId)));
  }

  @RequestMapping(
    path = {
      RUTA_CONVERSACIONES + "/{" + PATH_VARIABLE_POSTULACION_ID + "}",
      RUTA_CONVERSACIONES + "/{" + PATH_VARIABLE_POSTULACION_ID + "}/mensajes",
    },
    method = RequestMethod.GET
  )
  @ResponseBody
  public ResponseEntity<?> mensajes(
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpServletRequest request
  ) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(
      servicioChat
        .listarMensajes(usuarioId, postulacionId)
        .stream()
        .map(m -> {
          Map<String, Object> respuesta = new LinkedHashMap<>();
          respuesta.put("id", m.getId());
          respuesta.put("contenido", m.getContenido());
          respuesta.put("remitenteId", m.getRemitente().getId());
          respuesta.put("fecha", formatearFecha(m.getFechaEnvio()));
          return respuesta;
        })
        .toList()
    );
  }

  @RequestMapping(
    path = RUTA_CONVERSACIONES + "/{" + PATH_VARIABLE_POSTULACION_ID + "}/leido",
    method = RequestMethod.POST
  )
  @ResponseBody
  public ResponseEntity<?> marcarLeidos(
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    HttpServletRequest request
  ) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    servicioChat.marcarLeidos(usuarioId, postulacionId);
    return ResponseEntity.ok(Map.of("cantidad", servicioChat.contarNoLeidos(usuarioId)));
  }

  @RequestMapping(
    path = RUTA_CONVERSACIONES + "/{" + PATH_VARIABLE_POSTULACION_ID + "}/mensajes",
    method = RequestMethod.POST
  )
  @ResponseBody
  public ResponseEntity<?> enviar(
    @PathVariable(PATH_VARIABLE_POSTULACION_ID) Long postulacionId,
    @RequestParam("contenido") String contenido,
    HttpServletRequest request
  ) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (contenido == null || contenido.trim().isEmpty() || contenido.trim().length() > 2000) {
      return ResponseEntity.badRequest().build();
    }
    Mensaje mensaje = servicioChat.enviar(usuarioId, postulacionId, contenido);
    Map<String, Object> respuesta = new LinkedHashMap<>();
    respuesta.put("id", mensaje.getId());
    respuesta.put("contenido", mensaje.getContenido());
    respuesta.put("remitenteId", usuarioId);
    respuesta.put("fecha", formatearFecha(mensaje.getFechaEnvio()));
    return ResponseEntity.ok(respuesta);
  }

  private String formatearFecha(java.time.LocalDateTime fecha) {
    return fecha == null ? null : FECHA_JSON.format(fecha);
  }

  private Long usuarioId(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    return id instanceof Number ? ((Number) id).longValue() : null;
  }

  private String nombreOtro(Postulacion postulacion, Long usuarioId) {
    Usuario candidato = postulacion.getUsuario();
    if (candidato != null && usuarioId.equals(candidato.getId())) {
      return postulacion.getEmpresa() == null ? "Empresa" : postulacion.getEmpresa();
    }
    return candidato == null ? "Postulante" : nombre(candidato);
  }

  private String nombre(Usuario usuario) {
    String nombre =
      ((usuario.getNombre() == null ? "" : usuario.getNombre()) +
        " " +
        (usuario.getApellido() == null ? "" : usuario.getApellido())).trim();
    return nombre.isEmpty() ? usuario.getEmail() : nombre;
  }
}
