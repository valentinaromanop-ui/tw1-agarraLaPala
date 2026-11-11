package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.ResumenConversacion;
import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import com.tallerwebi.dominio.excepcion.MensajeInvalido;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ControladorChatApi {

  private final ServicioMensaje servicioMensaje;

  @Autowired
  public ControladorChatApi(ServicioMensaje servicioMensaje) {
    this.servicioMensaje = servicioMensaje;
  }

  @GetMapping("/conversaciones")
  public ResponseEntity<?> conversaciones(HttpServletRequest request) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(servicioMensaje.listarConversaciones(usuarioId));
  }

  @GetMapping("/conversaciones/{postulacionId}/mensajes")
  public ResponseEntity<?> mensajes(@PathVariable Long postulacionId, HttpServletRequest request) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      return ResponseEntity.ok(
        servicioMensaje.listar(postulacionId, usuarioId).stream().map(this::dto).toList()
      );
    } catch (AccesoChatDenegado e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
  }

  @PostMapping("/conversaciones/{postulacionId}/leidos")
  public ResponseEntity<?> marcarLeidos(
    @PathVariable Long postulacionId,
    HttpServletRequest request
  ) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      servicioMensaje.marcarComoLeidos(postulacionId, usuarioId);
      return ResponseEntity.noContent().build();
    } catch (AccesoChatDenegado e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
  }

  @PostMapping("/conversaciones/{postulacionId}/mensajes")
  public ResponseEntity<?> enviar(
    @PathVariable Long postulacionId,
    @RequestBody MensajeEntrante entrante,
    HttpServletRequest request
  ) {
    Long usuarioId = usuarioId(request);
    if (usuarioId == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    try {
      Mensaje mensaje = servicioMensaje.enviar(postulacionId, usuarioId, entrante.getTexto());
      return ResponseEntity.ok(dto(mensaje));
    } catch (AccesoChatDenegado e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    } catch (MensajeInvalido e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  private MensajeDTO dto(Mensaje mensaje) {
    return new MensajeDTO(
      mensaje.getId(),
      mensaje.getEmisor().getId(),
      mensaje.getTexto(),
      mensaje.getFecha().toString()
    );
  }

  private Long usuarioId(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object id = session == null ? null : session.getAttribute("USUARIO_ID");
    return id instanceof Number ? ((Number) id).longValue() : null;
  }
}
