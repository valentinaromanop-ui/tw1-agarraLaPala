package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioPostulacionImpl implements ServicioPostulacion {

  private static final String ESTADO_ENVIADA = "Enviada";

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPostulacion repositorioPostulacion;

  @Autowired
  public ServicioPostulacionImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioPostulacion repositorioPostulacion
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPostulacion = repositorioPostulacion;
  }

  @Override
  public Postulacion crearPostulacion(Long usuarioId, String ofertaId) {
    if (ofertaId == null || ofertaId.trim().isEmpty()) {
      throw new IllegalArgumentException("La oferta es obligatoria.");
    }
    String identificador = ofertaId.trim();
    Postulacion existente = repositorioPostulacion.buscarPorUsuarioYOferta(
      usuarioId,
      identificador
    );
    if (existente != null) {
      return existente;
    }
    Postulacion postulacion = new Postulacion();
    postulacion.setUsuario(repositorioUsuario.buscarPorId(usuarioId));
    postulacion.setOfertaId(identificador);
    postulacion.setFechaPostulacion(LocalDateTime.now());
    postulacion.setEstado(ESTADO_ENVIADA);
    return repositorioPostulacion.guardar(postulacion);
  }

  @Override
  public List<Postulacion> listarPostulaciones(Long usuarioId) {
    return repositorioPostulacion.buscarPorUsuarioId(usuarioId);
  }
}
