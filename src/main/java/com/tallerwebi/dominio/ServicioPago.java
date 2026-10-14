package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;

public interface ServicioPago {
  Suscripcion pagar(Long usuarioId, String titular, String numero, String vencimiento, String cvv);
  List<Pago> listarPagos(Long usuarioId);
  LocalDateTime fechaFinPremium(Long usuarioId);
}
