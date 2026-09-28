package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.BusquedaVacanteInvalida;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ServicioVacanteImpl implements ServicioVacante {

  private final List<VacanteProvider> providers;

  public ServicioVacanteImpl(List<VacanteProvider> providers) {
    this.providers = providers;
  }

  @Override
  public List<VacanteDTO> obtenerVacantesPorSkills(BusquedaVacanteDTO busqueda) {
    if (busqueda == null || busqueda.getSkills() == null) {
      throw new BusquedaVacanteInvalida();
    }
    List<String> skills = normalizarSkills(busqueda.getSkills());
    if (skills.isEmpty()) {
      throw new BusquedaVacanteInvalida();
    }
    BusquedaVacanteDTO normalizada = new BusquedaVacanteDTO();
    normalizada.setSkills(skills);
    List<VacanteDTO> vacantes = new ArrayList<>();
    for (VacanteProvider provider : providers) {
      vacantes.addAll(provider.buscarVacantes(normalizada));
    }
    return getVacantesOrdenados(vacantes);
  }

  private static List<VacanteDTO> getVacantesOrdenados(List<VacanteDTO> vacantes) {
    vacantes.sort(
      Comparator
        .comparingInt((VacanteDTO vacante) -> vacante.getSkills().size())
        .reversed()
        .thenComparing(
          VacanteDTO::getFechaPublicacion,
          Comparator.nullsLast(Comparator.reverseOrder())
        )
    );
    return vacantes;
  }

  private List<String> normalizarSkills(List<String> skillsIngresadas) {
    List<String> skills = new ArrayList<>();
    for (String skill : skillsIngresadas) {
      if (skill != null && !skill.trim().isEmpty()) {
        String normalizada = skill.trim().toLowerCase(Locale.ROOT);
        if (!skills.contains(normalizada)) {
          skills.add(normalizada);
        }
      }
    }
    return skills;
  }
}
