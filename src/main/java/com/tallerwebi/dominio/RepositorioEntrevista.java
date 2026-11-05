package com.tallerwebi.dominio;

import java.time.Instant;
import java.util.List;

public interface RepositorioEntrevista {
  void guardar(Entrevista entrevista);
  Entrevista buscarPorPostulacionPorId(Long postulacionId);
  Entrevista buscarPorPostulacionParaActualizar(Long postulacionId);
  List<Entrevista> buscarAgendadas(Long empleadorId, Instant desde, Instant hasta);
}
