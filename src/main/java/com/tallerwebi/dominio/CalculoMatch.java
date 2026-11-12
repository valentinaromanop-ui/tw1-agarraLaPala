package com.tallerwebi.dominio;

import java.text.Normalizer;
import java.util.*;

public class CalculoMatch {

  private CalculoMatch() {}

  public static ResultadoMatch calcular(List<String> postulante, List<String> oferta) {
    Map<String, String> requeridas = indexar(oferta);
    Set<String> tiene = indexar(postulante).keySet();
    List<String> coincidentes = new ArrayList<>();
    List<String> faltantes = new ArrayList<>();
    for (Map.Entry<String, String> e : requeridas.entrySet()) {
      (tiene.contains(e.getKey()) ? coincidentes : faltantes).add(e.getValue());
    }
    Integer porcentaje = requeridas.isEmpty()
      ? null
      : Math.round((coincidentes.size() * 100f) / requeridas.size());
    return new ResultadoMatch(porcentaje, coincidentes, faltantes);
  }

  private static Map<String, String> indexar(List<String> skills) {
    Map<String, String> mapa = new LinkedHashMap<>();
    if (skills != null) {
      for (String s : skills) {
        if (s != null && !s.isBlank()) {
          String clave = Normalizer
            .normalize(s.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
          mapa.putIfAbsent(clave, s.trim());
        }
      }
    }
    return mapa;
  }
}
