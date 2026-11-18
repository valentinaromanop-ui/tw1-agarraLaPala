package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FiltroPalabrasTest {

  @Test
  void detectaPalabraProhibida() {
    assertTrue(FiltroPalabras.contiene("sos un idiota"));
  }

  @Test
  void ignoraMayusculasYTildes() {
    assertTrue(FiltroPalabras.contiene("ESTÚPIDO"));
  }

  @Test
  void textoLimpioNoDispara() {
    assertFalse(FiltroPalabras.contiene("Buen día", "Java dev"));
  }

  @Test
  void toleraNulos() {
    assertFalse(FiltroPalabras.contiene((String) null));
  }
}
