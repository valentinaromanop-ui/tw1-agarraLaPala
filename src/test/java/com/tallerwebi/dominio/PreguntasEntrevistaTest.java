package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PreguntasEntrevistaTest {

  @Test
  void generaPreguntasEnOrdenEstableAunqueLasHabilidadesLleguenDesordenadas() {
    assertEquals(
      List.of(
        "Explicá qué es Git y contá una situación donde lo usaste.",
        "Explicá qué es JavaScript y contá una situación donde lo usaste.",
        "Explicá qué es SQL y contá una situación donde lo usaste."
      ),
      PreguntasEntrevista.paraHabilidades(List.of("SQL", "Git", "JavaScript"))
    );
    assertEquals(
      PreguntasEntrevista.paraHabilidades(List.of("SQL", "Git", "JavaScript")),
      PreguntasEntrevista.paraHabilidades(List.of("JavaScript", "SQL", "Git"))
    );
  }
}
