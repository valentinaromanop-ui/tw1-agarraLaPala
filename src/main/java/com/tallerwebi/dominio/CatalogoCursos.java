package com.tallerwebi.dominio;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CatalogoCursos {

  private CatalogoCursos() {}

  public static Map<String, List<CursoSugerido>> paraHabilidades(List<String> habilidades) {
    Map<String, List<CursoSugerido>> lista = new LinkedHashMap<>();
    for (String habilidad : habilidades) {
      String url = URLEncoder.encode(habilidad, StandardCharsets.UTF_8);
      lista.put(
        habilidad,
        List.of(
          new CursoSugerido("Coursera", "https://www.coursera.org/search?query=" + url),
          new CursoSugerido("Udemy", "https://www.udemy.com/courses/search/?q=" + url),
          new CursoSugerido("YouTube", "https://www.youtube.com/results?search_query=curso+" + url)
        )
      );
    }
    return lista;
  }
}
