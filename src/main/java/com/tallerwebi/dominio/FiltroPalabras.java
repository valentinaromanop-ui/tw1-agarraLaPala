package com.tallerwebi.dominio;

import java.util.Set;

public final class FiltroPalabras {

  private static final Set<String> PROHIBIDAS = Set.of("idiota", "estupido", "mierda", "puta");

  private FiltroPalabras() {}

  public static boolean contiene(String... textos) {
    for (String texto : textos) {
      if (texto == null) {
        continue;
      }
      for (String palabra : CalculadorCoincidenciaCv.extraerPalabrasClaveNormalizadas(texto)) {
        if (PROHIBIDAS.contains(palabra)) {
          return true;
        }
      }
    }
    return false;
  }
}
