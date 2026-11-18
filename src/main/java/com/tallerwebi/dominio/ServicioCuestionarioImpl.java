package com.tallerwebi.dominio;

import static com.tallerwebi.dominio.ServicioPostulacionImpl.FUENTE_LOCAL;

import com.tallerwebi.dominio.excepcion.CuestionarioNoDisponible;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioCuestionarioImpl implements ServicioCuestionario {

  private static final String PREFIJO_OFERTA_LOCAL = FUENTE_LOCAL + ":";

  private final RepositorioPostulacion repositorioPostulacion;
  private final ServicioPostulacion servicioPostulacion;
  private final EvaluadorIa evaluador;

  @Autowired
  public ServicioCuestionarioImpl(
    RepositorioPostulacion repositorioPostulacion,
    ServicioPostulacion servicioPostulacion,
    EvaluadorIa evaluador
  ) {
    this.repositorioPostulacion = repositorioPostulacion;
    this.servicioPostulacion = servicioPostulacion;
    this.evaluador = evaluador;
  }

  @Override
  public List<String> obtenerPreguntas(Long postulacionId, Long usuarioId)
    throws CuestionarioNoDisponible {
    return preguntasDe(obtenerPropia(postulacionId, usuarioId));
  }

  @Override
  public ResultadoCuestionario responder(
    Long postulacionId,
    Long usuarioId,
    List<String> respuestas
  ) throws CuestionarioNoDisponible {
    Postulacion postulacion = obtenerPropia(postulacionId, usuarioId);
    if (postulacion.getPuntajeIa() != null) {
      throw new CuestionarioNoDisponible("Ya completaste este cuestionario.");
    }
    ResultadoCuestionario resultado = evaluador.evaluar(preguntasDe(postulacion), respuestas);
    postulacion.setPuntajeIa(resultado.puntaje());
    postulacion.setDevolucionIa(resultado.devolucion());
    repositorioPostulacion.guardar(postulacion);
    return resultado;
  }

  private List<String> preguntasDe(Postulacion postulacion) throws CuestionarioNoDisponible {
    Long vacanteId = obtenerVacanteId(postulacion.getIdOferta());
    Vacante vacante = servicioPostulacion.obtenerVacante(FUENTE_LOCAL, vacanteId);
    if (vacante == null || !vacante.isCuestionarioIa()) {
      throw new CuestionarioNoDisponible("El cuestionario no está disponible para esta oferta.");
    }
    return PreguntasEntrevista.paraHabilidades(
      servicioPostulacion.obtenerHabilidadesOferta(FUENTE_LOCAL, vacanteId)
    );
  }

  private Long obtenerVacanteId(String idOferta) throws CuestionarioNoDisponible {
    if (idOferta == null || !idOferta.startsWith(PREFIJO_OFERTA_LOCAL)) {
      throw new CuestionarioNoDisponible(
        "El cuestionario solo está disponible para ofertas locales."
      );
    }
    try {
      return Long.valueOf(idOferta.substring(PREFIJO_OFERTA_LOCAL.length()));
    } catch (NumberFormatException excepcion) {
      throw new CuestionarioNoDisponible(
        "La oferta asociada al cuestionario no es válida.",
        excepcion
      );
    }
  }

  private Postulacion obtenerPropia(Long postulacionId, Long usuarioId)
    throws CuestionarioNoDisponible {
    Postulacion postulacion = repositorioPostulacion.buscarPorId(postulacionId);
    if (
      postulacion == null ||
      postulacion.getUsuario() == null ||
      postulacion.getUsuario().getId() == null ||
      !postulacion.getUsuario().getId().equals(usuarioId)
    ) {
      throw new CuestionarioNoDisponible("Postulación no encontrada.");
    }
    return postulacion;
  }
}
