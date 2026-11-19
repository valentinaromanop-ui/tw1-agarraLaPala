package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CatalogoCursosPremiumTest {

  @Test
  void buscaUnCursoExistente() {
    assertTrue(CatalogoCursosPremium.buscar("java-backend").isPresent());
  }

  @Test
  void devuelveVacioSiNoExiste() {
    assertTrue(CatalogoCursosPremium.buscar("inexistente").isEmpty());
  }

  @Test
  void relacionaSkillsConCursosPremium() {
    assertEquals(
      "java-backend",
      CatalogoCursosPremium.idsPorHabilidad(List.of("Java")).get("Java")
    );
    assertEquals("sql-datos", CatalogoCursosPremium.idsPorHabilidad(List.of("SQL")).get("SQL"));
  }
}
