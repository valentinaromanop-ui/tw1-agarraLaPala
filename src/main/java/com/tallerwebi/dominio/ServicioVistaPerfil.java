package com.tallerwebi.dominio;

import java.util.List;

public interface ServicioVistaPerfil {
  void registrarVista(
    Long postulanteId,
    Long empleadorId,
    String empresa,
    Long ofertaId,
    String ofertaTitulo,
    TipoVistaPerfil tipo
  );
  List<VistaPerfilDTO> listarUltimas(Long postulanteId);
  long contar(Long postulanteId, TipoVistaPerfil tipo);
}
