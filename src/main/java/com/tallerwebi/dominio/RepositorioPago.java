package com.tallerwebi.dominio;

import java.util.List;

public interface RepositorioPago {
  Pago guardar(Pago pago);
  List<Pago> buscarPorUsuarioId(Long usuarioId);
}
