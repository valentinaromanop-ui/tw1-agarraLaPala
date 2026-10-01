package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.BusquedaVacanteDTO;
import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.VacanteProvider;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class VacanteLocalProvider implements VacanteProvider {

  private RepositorioVacante repositorioVacante;
  private VacanteMapper vacanteMapper;

  @Autowired
  public VacanteLocalProvider(RepositorioVacante repositorioVacante, VacanteMapper vacanteMapper) {
    this.repositorioVacante = repositorioVacante;
    this.vacanteMapper = vacanteMapper;
  }

  @Override
  public List<VacanteDTO> buscarVacantes(BusquedaVacanteDTO busqueda) {
    List<VacanteDTO> resultado = new ArrayList<>();
    for (Vacante vacante : repositorioVacante.buscarPorSkills(busqueda.getSkills())) {
      resultado.add(vacanteMapper.toDTO(vacante, busqueda.getSkills()));
    }
    return resultado;
  }
}
