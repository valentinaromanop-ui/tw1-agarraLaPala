package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.ServicioMensaje;
import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import com.tallerwebi.dominio.excepcion.MensajeInvalido;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ControladorChatWs {

  private final ServicioMensaje servicioMensaje;

  @Autowired
  public ControladorChatWs(ServicioMensaje servicioMensaje) {
    this.servicioMensaje = servicioMensaje;
  }

  @MessageMapping("/chat/{postulacionId}")
  @SendTo("/tema/chat/{postulacionId}")
  public MensajeDTO enviar(
    @DestinationVariable Long postulacionId,
    MensajeEntrante entrante,
    SimpMessageHeaderAccessor headers
  ) throws MensajeInvalido, AccesoChatDenegado {
    Map<String, Object> sesion = headers.getSessionAttributes();
    if (sesion == null || !(sesion.get("USUARIO_ID") instanceof Number)) {
      throw new AccesoChatDenegado("No hay sesión iniciada");
    }
    Long usuarioId = ((Number) sesion.get("USUARIO_ID")).longValue();

    Mensaje mensaje = servicioMensaje.enviar(postulacionId, usuarioId, entrante.getTexto());

    return new MensajeDTO(
      mensaje.getId(),
      mensaje.getEmisor().getId(),
      mensaje.getTexto(),
      mensaje.getFecha().toString()
    );
  }
}
