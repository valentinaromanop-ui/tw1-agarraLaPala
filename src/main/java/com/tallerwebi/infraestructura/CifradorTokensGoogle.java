package com.tallerwebi.infraestructura;

import com.tallerwebi.config.GoogleCalendarConfig;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CifradorTokensGoogle {

  private static final int PARTES_TOKEN = 2;

  private final GoogleCalendarConfig configuracion;

  @Autowired
  public CifradorTokensGoogle(GoogleCalendarConfig configuracion) {
    this.configuracion = configuracion;
  }

  public String cifrar(String token, Long empleadorId) {
    byte[] nonce = new byte[12];
    new SecureRandom().nextBytes(nonce);
    byte[] cifrado = transformar(
      Cipher.ENCRYPT_MODE,
      token.getBytes(StandardCharsets.UTF_8),
      nonce,
      empleadorId
    );
    return (
      Base64.getEncoder().encodeToString(nonce) + ":" + Base64.getEncoder().encodeToString(cifrado)
    );
  }

  public String descifrar(String token, Long empleadorId) {
    String[] partes = token.split(":", PARTES_TOKEN);
    if (partes.length != PARTES_TOKEN) {
      throw new IllegalStateException("La conexion de Google necesita reconectarse.");
    }
    byte[] nonce = Base64.getDecoder().decode(partes[0]);
    byte[] cifrado = Base64.getDecoder().decode(partes[1]);
    return new String(
      transformar(Cipher.DECRYPT_MODE, cifrado, nonce, empleadorId),
      StandardCharsets.UTF_8
    );
  }

  private byte[] transformar(int modo, byte[] datos, byte[] nonce, Long empleadorId) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
        modo,
        new SecretKeySpec(configuracion.getClave(), "AES"),
        new GCMParameterSpec(128, nonce)
      );
      cipher.updateAAD(empleadorId.toString().getBytes(StandardCharsets.UTF_8));
      return cipher.doFinal(datos);
    } catch (GeneralSecurityException | IllegalArgumentException excepcion) {
      throw new IllegalStateException(
        "No se pudo proteger o recuperar la conexion de Google.",
        excepcion
      );
    }
  }
}
