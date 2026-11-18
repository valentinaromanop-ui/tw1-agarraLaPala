package com.tallerwebi.dominio;

import java.util.List;

@FunctionalInterface
public interface EvaluadorIa {
  ResultadoCuestionario evaluar(List<String> preguntas, List<String> respuestas);
}
