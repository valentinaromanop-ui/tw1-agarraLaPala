package com.tallerwebi.dominio;

import java.util.List;

public final class PreguntasEntrevista {

  private static final int MAXIMO = 3;

  private PreguntasEntrevista() {}

  public static List<String> paraHabilidades(List<String> habilidades) {
    if (habilidades == null || habilidades.isEmpty()) {
      return List.of("Contanos tu experiencia más relevante para este puesto.");
    }
    return habilidades
      .stream()
      .limit(MAXIMO)
      .map(habilidad -> "Explicá qué es " + habilidad + " y contá una situación donde lo usaste.")
      .toList();
  }
}
