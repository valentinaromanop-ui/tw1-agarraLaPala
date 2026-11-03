package com.tallerwebi.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

@Entity
public class ConexionGoogleCalendar {

  @Id
  private Long empleadorId;

  @OneToOne(optional = false)
  @MapsId
  private Usuario empleador;

  @Column(nullable = false, length = 4096)
  private String refreshTokenCifrado;

  public ConexionGoogleCalendar() {}

  public ConexionGoogleCalendar(Usuario empleador, String refreshTokenCifrado) {
    this.empleador = empleador;
    this.empleadorId = empleador.getId();
    this.refreshTokenCifrado = refreshTokenCifrado;
  }

  public String getRefreshTokenCifrado() {
    return refreshTokenCifrado;
  }

  public void setRefreshTokenCifrado(String refreshTokenCifrado) {
    this.refreshTokenCifrado = refreshTokenCifrado;
  }
}
