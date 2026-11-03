package com.tallerwebi.dominio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SuscriptorNewsletter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 250)
  private String email;

  @Column(nullable = false)
  private LocalDateTime fechaAlta;

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public LocalDateTime getFechaAlta() {
    return fechaAlta;
  }

  public void setFechaAlta(LocalDateTime fechaAlta) {
    this.fechaAlta = fechaAlta;
  }
}
