package com.tallerwebi.dominio;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioTendenciasImpl implements ServicioTendencias {

  private final RepositorioVacante repositorioVacante;

  @Autowired
  public ServicioTendenciasImpl(RepositorioVacante repositorioVacante) {
    this.repositorioVacante = repositorioVacante;
  }

  @Override
  public List<HabilidadTop> obtenerTop(int limite) {
    return repositorioVacante.topHabilidades(limite);
  }
}
