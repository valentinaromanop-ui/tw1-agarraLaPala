package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EvaluadorIaImplTest {

  @Test
  void evaluaRespuestasConSusPreguntasCorrespondientes() {
    ServicioIA servicioIA = mock(ServicioIA.class);
    when(servicioIA.generarTexto(org.mockito.ArgumentMatchers.anyString()))
      .thenReturn("{\"puntaje\":85,\"devolucion\":\"Explicación correcta y pertinente.\"}");
    EvaluadorIaImpl evaluador = new EvaluadorIaImpl(servicioIA);

    ResultadoCuestionario resultado = evaluador.evaluar(
      java.util.List.of("¿Qué es SQL?", "¿Qué es Git?"),
      java.util.List.of("SQL sirve para consultar bases de datos.", "Git controla versiones.")
    );

    assertEquals(85, resultado.puntaje());
    assertEquals("Explicación correcta y pertinente.", resultado.devolucion());

    ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
    verify(servicioIA).generarTexto(prompt.capture());
    assertTrue(prompt.getValue().contains("no mezcles una respuesta con otra pregunta."));
    assertTrue(prompt.getValue().contains("máximo 2 oraciones y 25 palabras en total."));
    assertTrue(prompt.getValue().contains("Acierto: ... Mejora: ..."));
    assertTrue(
      prompt
        .getValue()
        .contains("Pregunta 1: ¿Qué es SQL?\nRespuesta 1: SQL sirve para consultar bases de datos.")
    );
    assertTrue(
      prompt.getValue().contains("Pregunta 2: ¿Qué es Git?\nRespuesta 2: Git controla versiones.")
    );
  }
}
