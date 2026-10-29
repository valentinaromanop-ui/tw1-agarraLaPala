package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import com.tallerwebi.dominio.excepcion.MensajeInvalido;
import java.util.List;

public interface ServicioMensaje {
  Mensaje enviar(Long postulacionId, Long usuarioId, String texto)
    throws MensajeInvalido, AccesoChatDenegado;
  List<Mensaje> listar(Long postulacionId, Long usuarioId) throws AccesoChatDenegado;
}
