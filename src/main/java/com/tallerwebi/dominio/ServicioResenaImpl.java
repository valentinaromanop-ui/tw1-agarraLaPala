package com.tallerwebi.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@SuppressWarnings("PMD.CyclomaticComplexity")
public class ServicioResenaImpl implements ServicioResena {

  public static final int MAX_LARGO_RESPUESTA = 2000;
  private static final String MENSAJE_RESENA_NO_EXISTE = "La reseña no existe.";
  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private static final String ROL_CANDIDATO = "CANDIDATO";
  private static final String ROL_POSTULANTE = "POSTULANTE";

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;
  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioResena repositorioResena;

  @Autowired
  public ServicioResenaImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante,
    RepositorioPostulacion repositorioPostulacion,
    RepositorioResena repositorioResena
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioResena = repositorioResena;
  }

  @Override
  public Usuario obtenerEmpleador(Long empleadorId) {
    if (empleadorId == null) {
      return null;
    }
    Usuario empleador = repositorioUsuario.buscarPorId(empleadorId);
    return esEmpleador(empleador) ? empleador : null;
  }

  @Override
  public List<Resena> listarPorEmpleador(Long empleadorId) {
    validarEmpleador(empleadorId);
    return repositorioResena.buscarPorEmpleador(empleadorId);
  }

  @Override
  public Resena buscarPorCandidatoYEmpleador(Long candidatoId, Long empleadorId) {
    if (
      candidatoId == null ||
      empleadorId == null ||
      !esCandidato(repositorioUsuario.buscarPorId(candidatoId))
    ) {
      return null;
    }
    return repositorioResena.buscarPorCandidatoYEmpleador(candidatoId, empleadorId);
  }

  @Override
  public boolean puedeResenar(Long candidatoId, Long empleadorId) {
    if (
      candidatoId == null ||
      !esCandidato(repositorioUsuario.buscarPorId(candidatoId)) ||
      obtenerEmpleador(empleadorId) == null ||
      repositorioResena.buscarPorCandidatoYEmpleador(candidatoId, empleadorId) != null
    ) {
      return false;
    }
    return sePostuloAOfertaDelEmpleador(candidatoId, empleadorId);
  }

  @Override
  public ResumenResenas obtenerResumen(Long empleadorId) {
    return resumir(empleadorId, listarPorEmpleador(empleadorId));
  }

  @Override
  public Map<Long, ResumenResenas> obtenerResumenes(List<Long> empleadorIds) {
    Map<Long, List<Resena>> resenasPorEmpleador = new HashMap<>();
    if (empleadorIds == null || empleadorIds.isEmpty()) {
      return Map.of();
    }
    List<Long> idsUnicos = empleadorIds.stream().filter(id -> id != null).distinct().toList();
    for (Long empleadorId : idsUnicos) {
      resenasPorEmpleador.put(empleadorId, new ArrayList<>());
    }
    if (idsUnicos.isEmpty()) {
      return Map.of();
    }
    for (Resena resena : repositorioResena.buscarPorEmpleadores(idsUnicos)) {
      resenasPorEmpleador
        .computeIfAbsent(resena.getEmpleador().getId(), id -> new ArrayList<>())
        .add(resena);
    }
    Map<Long, ResumenResenas> resumenes = new HashMap<>();
    for (Map.Entry<Long, List<Resena>> entrada : resenasPorEmpleador.entrySet()) {
      resumenes.put(entrada.getKey(), resumir(entrada.getKey(), entrada.getValue()));
    }
    return resumenes;
  }

  @Override
  public Resena crear(Long candidatoId, Long empleadorId, int puntaje, String comentario) {
    Usuario candidato = validarCandidato(candidatoId);
    Usuario empleador = validarEmpleador(empleadorId);
    validarPuntaje(puntaje);
    validarTexto(comentario, Resena.MAX_LARGO_COMENTARIO, "comentario");
    if (!sePostuloAOfertaDelEmpleador(candidatoId, empleadorId)) {
      throw new IllegalArgumentException(
        "Solo pueden reseñar candidatos que se postularon a una oferta de la empresa."
      );
    }
    if (repositorioResena.buscarPorCandidatoYEmpleador(candidatoId, empleadorId) != null) {
      throw new IllegalArgumentException("Ya escribiste una reseña para esta empresa.");
    }

    Resena resena = new Resena();
    resena.setCandidato(candidato);
    resena.setEmpleador(empleador);
    resena.setPuntaje(puntaje);
    resena.setComentario(comentario.trim());
    resena.setFecha(LocalDateTime.now());
    return repositorioResena.guardar(resena);
  }

  @Override
  public void editar(
    Long candidatoId,
    Long empleadorId,
    Long resenaId,
    int puntaje,
    String comentario
  ) {
    validarCandidato(candidatoId);
    validarPuntaje(puntaje);
    validarTexto(comentario, Resena.MAX_LARGO_COMENTARIO, "comentario");
    Resena resena = buscarResenaDeEmpleador(empleadorId, resenaId);
    validarAutor(resena, candidatoId);
    resena.setPuntaje(puntaje);
    resena.setComentario(comentario.trim());
    repositorioResena.guardar(resena);
  }

  @Override
  public void eliminar(Long candidatoId, Long empleadorId, Long resenaId) {
    validarCandidato(candidatoId);
    Resena resena = buscarResenaDeEmpleador(empleadorId, resenaId);
    validarAutor(resena, candidatoId);
    repositorioResena.eliminar(resena);
  }

  @Override
  public void responder(Long empleadorId, Long resenaId, String respuesta) {
    validarEmpleador(empleadorId);
    validarTexto(respuesta, MAX_LARGO_RESPUESTA, "respuesta");
    if (resenaId == null) {
      throw new IllegalArgumentException(MENSAJE_RESENA_NO_EXISTE);
    }
    Resena resena = repositorioResena.buscarPorIdParaActualizar(resenaId);
    if (resena == null) {
      throw new IllegalArgumentException(MENSAJE_RESENA_NO_EXISTE);
    }
    if (!empleadorId.equals(resena.getEmpleador().getId())) {
      throw new IllegalArgumentException("No podés responder reseñas de otra empresa.");
    }
    if (resena.getRespuestaEmpleador() != null) {
      throw new IllegalArgumentException("Esta reseña ya tiene una respuesta.");
    }
    resena.setRespuestaEmpleador(respuesta.trim());
    resena.setFechaRespuesta(LocalDateTime.now());
    repositorioResena.guardar(resena);
  }

  @Override
  public void reportar(Long empleadorId, Long resenaId, Long reportanteId) {
    Resena resena = buscarResenaDeEmpleador(empleadorId, resenaId);
    ReporteResena reporte = new ReporteResena();
    reporte.setResena(resena);
    if (reportanteId != null) {
      Usuario reportante = repositorioUsuario.buscarPorId(reportanteId);
      if (reportante == null) {
        throw new IllegalArgumentException("No se encontró la cuenta que reporta.");
      }
      reporte.setReportante(reportante);
    }
    reporte.setFecha(LocalDateTime.now());
    repositorioResena.guardarReporte(reporte);
  }

  private boolean sePostuloAOfertaDelEmpleador(Long candidatoId, Long empleadorId) {
    List<Vacante> ofertas = repositorioVacante.buscarPorEmpleador(empleadorId);
    List<Postulacion> postulaciones = repositorioPostulacion.buscarPorUsuarioId(candidatoId);
    for (Vacante oferta : ofertas) {
      if (oferta.getId() == null) {
        continue;
      }
      String idLocal = "LOCAL:" + oferta.getId();
      String idNumerico = String.valueOf(oferta.getId());
      for (Postulacion postulacion : postulaciones) {
        if (
          idLocal.equals(postulacion.getIdOferta()) || idNumerico.equals(postulacion.getIdOferta())
        ) {
          return true;
        }
      }
    }
    return false;
  }

  private ResumenResenas resumir(Long empleadorId, List<Resena> resenas) {
    if (resenas.isEmpty()) {
      return new ResumenResenas(empleadorId, BigDecimal.ZERO, 0);
    }
    int total = resenas.stream().mapToInt(Resena::getPuntaje).sum();
    BigDecimal promedio = BigDecimal
      .valueOf(total)
      .divide(BigDecimal.valueOf(resenas.size()), 1, RoundingMode.HALF_UP);
    return new ResumenResenas(empleadorId, promedio, resenas.size());
  }

  private Resena buscarResena(Long resenaId) {
    if (resenaId == null) {
      throw new IllegalArgumentException(MENSAJE_RESENA_NO_EXISTE);
    }
    Resena resena = repositorioResena.buscarPorId(resenaId);
    if (resena == null) {
      throw new IllegalArgumentException(MENSAJE_RESENA_NO_EXISTE);
    }
    return resena;
  }

  private Resena buscarResenaDeEmpleador(Long empleadorId, Long resenaId) {
    validarEmpleador(empleadorId);
    Resena resena = buscarResena(resenaId);
    if (!empleadorId.equals(resena.getEmpleador().getId())) {
      throw new IllegalArgumentException("La reseña no pertenece a esta empresa.");
    }
    return resena;
  }

  private void validarAutor(Resena resena, Long candidatoId) {
    if (!candidatoId.equals(resena.getCandidato().getId())) {
      throw new IllegalArgumentException("Solo podés modificar tu propia reseña.");
    }
  }

  private Usuario validarCandidato(Long candidatoId) {
    Usuario candidato = candidatoId == null ? null : repositorioUsuario.buscarPorId(candidatoId);
    if (!esCandidato(candidato)) {
      throw new IllegalArgumentException("Iniciá sesión como candidato para reseñar.");
    }
    return candidato;
  }

  private Usuario validarEmpleador(Long empleadorId) {
    Usuario empleador = obtenerEmpleador(empleadorId);
    if (empleador == null) {
      throw new IllegalArgumentException("La empresa no existe o la cuenta no es empleadora.");
    }
    return empleador;
  }

  private void validarPuntaje(int puntaje) {
    if (puntaje < 1 || puntaje > 5) {
      throw new IllegalArgumentException("La calificación debe estar entre 1 y 5.");
    }
  }

  private void validarTexto(String contenido, int maximo, String nombre) {
    if (contenido == null || contenido.trim().isEmpty()) {
      throw new IllegalArgumentException("El " + nombre + " no puede estar vacío.");
    }
    if (contenido.trim().length() > maximo) {
      throw new IllegalArgumentException(
        "El " + nombre + " no puede superar " + maximo + " caracteres."
      );
    }
  }

  private boolean esCandidato(Usuario usuario) {
    return (
      usuario != null &&
      (ROL_CANDIDATO.equalsIgnoreCase(usuario.getRol()) ||
        ROL_POSTULANTE.equalsIgnoreCase(usuario.getRol()))
    );
  }

  private boolean esEmpleador(Usuario usuario) {
    return usuario != null && ROL_EMPLEADOR.equalsIgnoreCase(usuario.getRol());
  }
}
