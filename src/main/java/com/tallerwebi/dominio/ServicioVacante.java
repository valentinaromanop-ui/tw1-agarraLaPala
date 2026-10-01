package com.tallerwebi.dominio;

import java.util.List;

@FunctionalInterface
public interface ServicioVacante {
  List<VacanteDTO> obtenerVacantesPorSkills(BusquedaVacanteDTO busqueda);
}
