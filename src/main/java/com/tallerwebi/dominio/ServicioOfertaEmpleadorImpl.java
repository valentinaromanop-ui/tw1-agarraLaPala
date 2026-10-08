package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioOfertaEmpleadorImpl implements ServicioOfertaEmpleador {

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;

  @Autowired
  public ServicioOfertaEmpleadorImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
  }

  @Override
  public Usuario obtenerEmpleador(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (usuario == null || !"EMPLEADOR".equalsIgnoreCase(usuario.getRol())) {
      return null;
    }
    return usuario;
  }

  @Override
  public void publicar(Vacante vacante) {
    repositorioVacante.guardar(vacante);
  }

  @Override
  public List<Vacante> listarPorEmpleador(Long empleadorId) {
    return repositorioVacante.buscarPorEmpleador(empleadorId);
  }

  @Override
  public List<Vacante> listarOfertasPublicadas() {
    return repositorioVacante.buscarOfertasPublicadasPorEmpleadores();
  }
}
