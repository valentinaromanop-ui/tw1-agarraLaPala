package com.tallerwebi.dominio;

import java.util.List;

public record ResultadoMatch(
  Integer porcentaje,
  List<String> coincidentes,
  List<String> faltantes
) {}
