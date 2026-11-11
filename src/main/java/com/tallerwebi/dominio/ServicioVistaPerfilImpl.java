package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioVistaPerfilImpl implements ServicioVistaPerfil {

  private static final String ROL_EMPLEADOR = "EMPLEADOR";
  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVistaPerfil repositorioVista;

  @Autowired
  public ServicioVistaPerfilImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVistaPerfil repositorioVista
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVista = repositorioVista;
  }

  @Override
  public void registrarVista(
    Long postulanteId,
    Long empleadorId,
    String empresa,
    Long ofertaId,
    String ofertaTitulo,
    TipoVistaPerfil tipo
  ) {
    Usuario postulante = repositorioUsuario.buscarPorId(postulanteId);
    Usuario empleador = repositorioUsuario.buscarPorId(empleadorId);
    if (!esVistaValida(postulante, empleador, tipo)) {
      return;
    }
    LocalDateTime ahora = LocalDateTime.now();
    VistaPerfil existente = repositorioVista.buscarReciente(
      postulanteId,
      empleadorId,
      ofertaId,
      tipo,
      ahora.minusHours(24)
    );
    if (existente != null) {
      existente.setFecha(ahora.minusNanos(1));
      return;
    }
    VistaPerfil vista = new VistaPerfil();
    vista.setPostulante(postulante);
    vista.setEmpleador(empleador);
    vista.setEmpresa(empresa);
    vista.setOfertaId(ofertaId);
    vista.setOfertaTitulo(ofertaTitulo);
    vista.setTipo(tipo);
    vista.setFecha(ahora);
    repositorioVista.guardar(vista);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VistaPerfilDTO> listarUltimas(Long postulanteId) {
    return repositorioVista
      .listarUltimas(postulanteId, 5)
      .stream()
      .map(VistaPerfilDTO::new)
      .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long contar(Long postulanteId, TipoVistaPerfil tipo) {
    return repositorioVista.contar(postulanteId, tipo);
  }

  private boolean esVistaValida(Usuario postulante, Usuario empleador, TipoVistaPerfil tipo) {
    return (
      postulante != null &&
      empleador != null &&
      tipo != null &&
      !postulante.getId().equals(empleador.getId()) &&
      ROL_EMPLEADOR.equalsIgnoreCase(empleador.getRol())
    );
  }
}
