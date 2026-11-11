package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.config.GoogleCalendarConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CifradorTokensGoogleTest {

  private CifradorTokensGoogle cifrador;

  @BeforeEach
  public void init() {
    GoogleCalendarConfig configuracion = mock(GoogleCalendarConfig.class);
    when(configuracion.getClave()).thenReturn(new byte[32]);
    cifrador = new CifradorTokensGoogle(configuracion);
  }

  @Test
  public void deberiaRecuperarElTokenCifrado() {
    String cifrado = cifrador.cifrar("token", 7L);
    assertNotEquals("token", cifrado);
    assertEquals("token", cifrador.descifrar(cifrado, 7L));
  }

  @Test
  public void deberiaGenerarCifradosDistintosParaElMismoToken() {
    String primero = cifrador.cifrar("token", 7L);
    String segundo = cifrador.cifrar("token", 7L);
    assertNotEquals(primero, segundo);
  }

  @Test
  public void deberiaRechazarElTokenDeOtroEmpleador() {
    String cifrado = cifrador.cifrar("token", 7L);
    assertThrows(IllegalStateException.class, () -> cifrador.descifrar(cifrado, 8L));
  }

  @Test
  public void deberiaRechazarUnTokenAlterado() {
    String cifrado = cifrador.cifrar("token", 7L);
    String alterado = cifrado.substring(0, cifrado.length() - 4) + "AAAA";
    assertThrows(IllegalStateException.class, () -> cifrador.descifrar(alterado, 7L));
  }
}
