package com.tallerwebi.dominio;

import java.time.LocalDateTime;
import java.util.List;

public interface RepositorioVistaPerfil {
  VistaPerfil guardar(VistaPerfil vista);
  VistaPerfil buscarReciente(
    Long postulanteId,
    Long empleadorId,
    Long ofertaId,
    TipoVistaPerfil tipo,
    LocalDateTime desde
  );
  List<VistaPerfil> listarUltimas(Long postulanteId, int limite);
  long contar(Long postulanteId, TipoVistaPerfil tipo);
}
