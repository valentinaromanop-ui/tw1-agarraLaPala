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
    List<VacanteDTO> filtradas = new ArrayList<>();
    for (VacanteDTO vacante : vacantes) {
      if (cumpleFiltros(vacante, busqueda)) {
        filtradas.add(vacante);
      }
    }
    return getVacantesOrdenados(filtradas);
  }

  private boolean cumpleFiltros(VacanteDTO vacante, BusquedaVacanteDTO busqueda) {
    if (!coincide(busqueda.getModalidad(), vacante.getModalidad())) {
      return false;
    }
    if (!coincide(busqueda.getSeniority(), vacante.getSeniority())) {
      return false;
    }
    if (!coincide(busqueda.getJornada(), vacante.getJornada())) {
      return false;
    }
    if (busqueda.getSueldoMinimo() != null) {
      if (busqueda.getMoneda() == null || busqueda.getMoneda().isBlank()) {
        return false;
      }
      if (vacante.getSalario() == null || !coincide(busqueda.getMoneda(), vacante.getMoneda())) {
        return false;
      }
      return (vacante.getSalario().compareTo(busqueda.getSueldoMinimo()) >= 0);
    }
    return true;
  }

  private boolean coincide(String filtro, String valor) {
    return filtro == null || filtro.isBlank() || filtro.equalsIgnoreCase(valor);
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
