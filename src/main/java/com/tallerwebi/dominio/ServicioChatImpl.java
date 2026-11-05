package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ServicioChatImpl implements ServicioChat {

  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioMensaje repositorioMensaje;
  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;

  @Autowired
  public ServicioChatImpl(
    RepositorioPostulacion repositorioPostulacion,
    RepositorioMensaje repositorioMensaje,
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante
  ) {
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioMensaje = repositorioMensaje;
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
  }

  @Override
  public List<Postulacion> listarConversaciones(Long usuarioId) {
    List<Postulacion> postulaciones = repositorioPostulacion.buscarPorUsuarioId(usuarioId);
    postulaciones = new java.util.ArrayList<>(postulaciones);
    Vacante[] vacantes = repositorioVacante.buscarPorEmpleador(usuarioId).toArray(Vacante[]::new);
    for (Vacante vacante : vacantes) {
      postulaciones.addAll(repositorioPostulacion.buscarPorVacante(vacante.getId()));
    }
    return postulaciones
      .stream()
      .filter(postulacion -> postulacion.getEstado() == EstadoPostulacion.ACEPTADA)
      .filter(postulacion ->
        postulacion.getUsuario() != null &&
        (usuarioId.equals(postulacion.getUsuario().getId()) ||
          esEmpleadorDeLaOferta(usuarioId, postulacion))
      )
      .distinct()
      .sorted(Comparator.comparing(this::fechaUltimoMensaje).reversed())
      .toList();
  }

  @Override
  public List<Mensaje> listarMensajes(Long usuarioId, Long postulacionId) {
    validarAcceso(usuarioId, postulacionId);
    return repositorioMensaje.listarPorPostulacion(postulacionId);
  }

  @Override
  public Mensaje enviar(Long usuarioId, Long postulacionId, String contenido) {
    Postulacion postulacion = validarAcceso(usuarioId, postulacionId);
    if (contenido == null || contenido.trim().isEmpty() || contenido.trim().length() > 2000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no es válido.");
    }
    Mensaje mensaje = new Mensaje();
    mensaje.setPostulacion(postulacion);
    Usuario remitente = repositorioUsuario.buscarPorId(usuarioId);
    if (remitente == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión no es válida.");
    }
    mensaje.setRemitente(remitente);
    mensaje.setContenido(contenido.trim());
    mensaje.setFechaEnvio(LocalDateTime.now());
    mensaje.setLeido(false);
    return repositorioMensaje.guardar(mensaje);
  }

  @Override
  public void marcarLeidos(Long usuarioId, Long postulacionId) {
    validarAcceso(usuarioId, postulacionId);
    repositorioMensaje.marcarLeidos(postulacionId, usuarioId);
  }

  @Override
  public long contarNoLeidos(Long usuarioId) {
    return listarConversaciones(usuarioId)
      .stream()
      .mapToLong(postulacion -> repositorioMensaje.contarNoLeidos(postulacion.getId(), usuarioId))
      .sum();
  }

  private Postulacion validarAcceso(Long usuarioId, Long postulacionId) {
    Postulacion postulacion = repositorioPostulacion.buscarPorId(postulacionId);
    if (postulacion == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La postulación no existe.");
    }
    if (
      postulacion.getEstado() != EstadoPostulacion.ACEPTADA ||
      !esParticipante(usuarioId, postulacion)
    ) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "No tenés acceso a esta conversación."
      );
    }
    return postulacion;
  }

  private boolean esParticipante(Long usuarioId, Postulacion postulacion) {
    return (
      (postulacion.getUsuario() != null && usuarioId.equals(postulacion.getUsuario().getId())) ||
      esEmpleadorDeLaOferta(usuarioId, postulacion)
    );
  }

  private boolean esEmpleadorDeLaOferta(Long usuarioId, Postulacion postulacion) {
    String ofertaId = postulacion.getIdOferta();
    if (ofertaId == null || !ofertaId.startsWith("LOCAL:")) {
      return false;
    }
    try {
      Vacante vacante = repositorioVacante.obtenerPorId(Long.valueOf(ofertaId.substring(6)));
      return vacante != null && usuarioId.equals(vacante.getEmpleadorId());
    } catch (NumberFormatException excepcion) {
      return false;
    }
  }

  private LocalDateTime fechaUltimoMensaje(Postulacion postulacion) {
    List<Mensaje> mensajes = repositorioMensaje.listarPorPostulacion(postulacion.getId());
    return mensajes.isEmpty()
      ? postulacion.getFechaPostulacion()
      : mensajes.get(mensajes.size() - 1).getFechaEnvio();
  }
}
