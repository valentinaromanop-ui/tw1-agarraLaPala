package com.tallerwebi.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class CalculadorCoincidenciaCv {

  private static final Set<String> PALABRAS_COMUNES = Set.of(
    "para",
    "como",
    "desde",
    "entre",
    "sobre",
    "con",
    "por",
    "una",
    "uno",
    "las",
    "los",
    "del",
    "que",
    "the",
    "and",
    "with",
    "from",
    "this",
    "that",
    "experiencia",
    "responsable",
    "perfil",
    "objetivo",
    "trabajo",
    "trabajar",
    "años",
    "ano"
  );

  private CalculadorCoincidenciaCv() {}

  public static Set<String> extraerPalabrasClave(String texto) {
    if (texto == null || texto.isBlank()) {
      return Collections.emptySet();
    }
    String normalizado = normalizar(texto);
    return Arrays
      .stream(normalizado.split("[^\\p{L}\\p{N}+#]+"))
      .filter(palabra -> palabra.length() >= 3)
      .filter(palabra -> !PALABRAS_COMUNES.contains(palabra))
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static CoincidenciaCv calcular(String textoCv, VacanteDTO vacante) {
    Set<String> palabrasCv = extraerPalabrasClave(textoCv);
    if (palabrasCv.isEmpty() || vacante == null) {
      return new CoincidenciaCv(0, List.of());
    }

    String textoOferta = String.join(
      " ",
      vacante.getTitulo(),
      vacante.getDescripcion(),
      vacante.getSkills() == null ? "" : String.join(" ", vacante.getSkills())
    );
    Set<String> palabrasOferta = extraerPalabrasClave(textoOferta);
    List<String> coincidentes = palabrasCv.stream().filter(palabrasOferta::contains).toList();
    int porcentaje = (int) Math.round((coincidentes.size() * 100.0) / palabrasCv.size());
    return new CoincidenciaCv(porcentaje, coincidentes);
  }

  public static String normalizar(String texto) {
    if (texto == null) {
      return "";
    }
    return Normalizer
      .normalize(texto, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .toLowerCase(Locale.ROOT);
  }
}
