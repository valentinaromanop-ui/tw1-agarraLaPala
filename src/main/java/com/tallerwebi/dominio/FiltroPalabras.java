package com.tallerwebi.dominio;

import java.util.Set;

public final class FiltroPalabras {

  private static final Set<String> PROHIBIDAS = Set.of(
    "idiota",
    "estupido",
    "mierda",
    "puta",
    "puto",
    "boludo",
    "pelotudo",
    "imbecil",
    "forro",
    "hijodeputa",
    "hijueputa"
  );

  public static boolean contiene(String... textos) {
    for (String t : textos) {
      if (t == null) continue;
      String normalizado = java.text.Normalizer
        .normalize(t.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
      for (String p : normalizado.split("[^a-z0-9]+")) if (PROHIBIDAS.contains(p)) return true;
    }
    return false;
  }
}
