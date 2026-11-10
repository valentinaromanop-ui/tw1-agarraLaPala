package com.tallerwebi.dominio;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CalculadorCoincidenciaSkills {

  private CalculadorCoincidenciaSkills() {}

  public static Set<String> obtenerSkillsOferta(Vacante vacante) {
    Set<String> skills = new HashSet<>();
    if (vacante == null || vacante.getVacanteSkills() == null) {
      return skills;
    }
    for (VacanteSkill relacion : vacante.getVacanteSkills()) {
      if (relacion.getSkill() != null && relacion.getSkill().getNombre() != null) {
        String nombre = normalizar(relacion.getSkill().getNombre());
        if (!nombre.isEmpty()) {
          skills.add(nombre);
        }
      }
    }
    return skills;
  }

  public static double porcentaje(List<String> habilidades, Set<String> skillsOferta) {
    if (skillsOferta == null || skillsOferta.isEmpty() || habilidades == null) {
      return 0;
    }
    Set<String> coincidencias = new HashSet<>();
    for (String habilidad : habilidades) {
      String nombre = normalizar(habilidad);
      if (!nombre.isEmpty() && skillsOferta.contains(nombre)) {
        coincidencias.add(nombre);
      }
    }
    return (coincidencias.size() * 100.0) / skillsOferta.size();
  }

  public static String normalizar(String habilidad) {
    return habilidad == null ? "" : habilidad.trim().toLowerCase(Locale.ROOT);
  }
}
