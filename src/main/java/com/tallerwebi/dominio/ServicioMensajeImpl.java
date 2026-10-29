package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.AccesoChatDenegado;
import com.tallerwebi.dominio.excepcion.MensajeInvalido;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioMensajeImpl implements ServicioMensaje {

  private final RepositorioMensaje repositorioMensaje;
  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;
  private static final int MAX_LARGO_MENSAJE = 1000;

  @Autowired
  public ServicioMensajeImpl(
    RepositorioMensaje repositorioMensaje,
    RepositorioPostulacion repositorioPostulacion,
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante
  ) {
    this.repositorioMensaje = repositorioMensaje;
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
  }

  @Override
  public Mensaje enviar(Long postulacionId, Long usuarioId, String texto)
    throws MensajeInvalido, AccesoChatDenegado {
    if (texto == null || texto.isBlank()) { //se fija si el msj esta en blanco no como empty que toma los espacios como caracteres
      throw new MensajeInvalido("El mensaje no puede estar vacío");
    }
    String textoLimpio = texto.trim(); //elimina espacios al inicio y al final
    if (textoLimpio.length() > MAX_LARGO_MENSAJE) { //verifica que longitud no sea mayor a 1000 caracteres
      throw new MensajeInvalido("El mensaje es demasiado largo");
    }
    Postulacion postulacion = obtenerYValidar(postulacionId, usuarioId);
    Usuario emisor = repositorioUsuario.buscarPorId(usuarioId);
    Mensaje mensaje = new Mensaje();
    mensaje.setPostulacion(postulacion);
    mensaje.setEmisor(emisor);
    mensaje.setTexto(textoLimpio);
    return repositorioMensaje.guardarMensaje(mensaje);
  }

  @Override
  public List<Mensaje> listar(Long postulacionId, Long usuarioId) throws AccesoChatDenegado {
    obtenerYValidar(postulacionId, usuarioId);
    return repositorioMensaje.listarPorPostulacion(postulacionId);
  }

  private Postulacion obtenerYValidar(Long postulacionId, Long usuarioId)
    throws AccesoChatDenegado {
    Postulacion postulacion = repositorioPostulacion.buscarPorId(postulacionId);
    if (postulacion == null) {
      throw new AccesoChatDenegado("La postulación no existe");
    }

    boolean esPostulante = postulacion.getUsuario().getId().equals(usuarioId);
    boolean esEmpleador = esEmpleadorDeOferta(postulacion.getIdOferta(), usuarioId);

    if (!esPostulante && !esEmpleador) {
      throw new AccesoChatDenegado("No tenés acceso a este chat");
    }
    return postulacion;
  }

  private boolean esEmpleadorDeOferta(String idOferta, Long usuarioId) {
    try {
      Long vacanteId = Long.valueOf(idOferta.replace("LOCAL:", ""));
      Vacante vacante = repositorioVacante.obtenerPorId(vacanteId);
      return vacante != null && usuarioId.equals(vacante.getEmpleadorId());
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
