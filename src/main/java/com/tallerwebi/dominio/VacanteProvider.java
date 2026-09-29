package com.tallerwebi.dominio;

import java.util.List;

@FunctionalInterface
public interface VacanteProvider {
  List<VacanteDTO> buscarVacantes(BusquedaVacanteDTO busqueda);
}
