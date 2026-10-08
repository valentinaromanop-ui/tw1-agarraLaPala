package com.tallerwebi.dominio;

import java.util.ArrayList;
import java.util.List;

public class BusquedaVacanteDTO {

  private CondicionesLaborales condiciones = new CondicionesLaborales();

  private List<String> skills = new ArrayList<>();

  public List<String> getSkills() {
    return skills;
  }

  public void setSkills(List<String> skills) {
    this.skills = skills;
  }

  public CondicionesLaborales getCondiciones() {
    return condiciones;
  }
}
