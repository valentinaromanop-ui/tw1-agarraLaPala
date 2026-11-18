package com.tallerwebi.dominio;

import java.util.List;

@FunctionalInterface
public interface ServicioTendencias {
  List<HabilidadTop> obtenerTop(int limite);
}
