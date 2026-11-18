package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
