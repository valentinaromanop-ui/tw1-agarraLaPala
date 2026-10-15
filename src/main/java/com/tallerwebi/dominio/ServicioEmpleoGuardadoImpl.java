package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioEmpleoGuardadoImpl implements ServicioEmpleoGuardado {

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioEmpleoGuardado repositorioEmpleoGuardado;

  @Autowired
  public ServicioEmpleoGuardadoImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioEmpleoGuardado repositorioEmpleoGuardado
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioEmpleoGuardado = repositorioEmpleoGuardado;
  }

  @Override
  public EmpleoGuardado guardar(Long usuarioId, EmpleoGuardado datos) {
    Usuario usuario = validarCandidato(usuarioId);
    if (datos == null || datos.getIdOferta() == null || datos.getIdOferta().trim().isEmpty()) {
      throw new IllegalArgumentException("La oferta es obligatoria.");
    }
    String ofertaId = datos.getIdOferta().trim();
    EmpleoGuardado existente = repositorioEmpleoGuardado.buscarPorUsuarioYOferta(
      usuarioId,
      ofertaId
    );
    if (existente != null) {
      return existente;
    }
    datos.setUsuario(usuario);
    datos.setIdOferta(ofertaId);
    datos.setFechaGuardado(LocalDateTime.now());
    if (datos.getTitulo() == null || datos.getTitulo().trim().isEmpty()) {
      datos.setTitulo("Oferta sin título");
    }
    return repositorioEmpleoGuardado.guardar(datos);
  }

  @Override
  public void quitar(Long usuarioId, String ofertaId) {
    validarCandidato(usuarioId);
    if (ofertaId == null || ofertaId.trim().isEmpty()) {
      throw new IllegalArgumentException("La oferta es obligatoria.");
    }
    EmpleoGuardado existente = repositorioEmpleoGuardado.buscarPorUsuarioYOferta(
      usuarioId,
      ofertaId.trim()
    );
    if (existente != null) {
      repositorioEmpleoGuardado.eliminar(existente);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmpleoGuardado> listar(Long usuarioId) {
    validarCandidato(usuarioId);
    return repositorioEmpleoGuardado.buscarPorUsuarioId(usuarioId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<String> listarIds(Long usuarioId) {
    List<String> ids = new ArrayList<>();
    for (EmpleoGuardado empleo : listar(usuarioId)) {
      ids.add(empleo.getIdOferta());
    }
    return ids;
  }

  private Usuario validarCandidato(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (
      usuario == null ||
      !("CANDIDATO".equalsIgnoreCase(usuario.getRol()) ||
        "POSTULANTE".equalsIgnoreCase(usuario.getRol()))
    ) {
      throw new IllegalArgumentException("Iniciá sesión como postulante.");
    }
    return usuario;
  }
}
