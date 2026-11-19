package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.CuestionarioNoDisponible;
import java.util.List;

public interface ServicioCuestionario {
  List<String> obtenerPreguntas(Long postulacionId, Long usuarioId) throws CuestionarioNoDisponible;
  ResultadoCuestionario responder(Long postulacionId, Long usuarioId, List<String> respuestas)
    throws CuestionarioNoDisponible;
}
