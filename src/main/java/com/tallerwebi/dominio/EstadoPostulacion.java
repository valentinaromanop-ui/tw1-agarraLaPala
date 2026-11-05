package com.tallerwebi.dominio;

public enum EstadoPostulacion {
  ENVIADA,
  EN_REVISION,
  ENTREVISTA,
  REGISTRADA,
  PENDIENTE,
  ACEPTADA,
  RECHAZADA;

  public String getEtiqueta() {
    return switch (this) {
      case ENVIADA, EN_REVISION, ENTREVISTA, REGISTRADA, PENDIENTE -> "Pendiente";
      case ACEPTADA -> "Aceptada";
      case RECHAZADA -> "Rechazada";
    };
  }

  public String getClaseCss() {
    return "estado-" + name().toLowerCase(java.util.Locale.ROOT);
  }

  public static EstadoPostulacion desdeValor(String valor) {
    String normalizado = valor.trim().toUpperCase(java.util.Locale.ROOT);
    return switch (normalizado) {
      case "PENDIENTE", "ENVIADA", "EN_REVISION", "ENTREVISTA", "REGISTRADA" -> PENDIENTE;
      case "ACEPTADA" -> ACEPTADA;
      case "RECHAZADA" -> RECHAZADA;
      default -> throw new IllegalArgumentException("Estado de postulación no válido.");
    };
  }
}
