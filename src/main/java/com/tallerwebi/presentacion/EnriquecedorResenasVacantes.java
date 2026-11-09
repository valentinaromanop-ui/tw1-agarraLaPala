package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ResumenResenas;
import com.tallerwebi.dominio.ServicioResena;
import com.tallerwebi.dominio.VacanteDTO;
import java.util.List;
import java.util.Map;

final class EnriquecedorResenasVacantes {

  private EnriquecedorResenasVacantes() {}

  static Map<Long, ResumenResenas> obtenerResumenes(
    List<Long> empleadorIds,
    ServicioResena servicioResena
  ) {
    if (servicioResena == null) {
      return Map.of();
    }
    return servicioResena.obtenerResumenes(empleadorIds);
  }

  static void agregarResumenes(List<?> resultados, ServicioResena servicioResena) {
    if (servicioResena == null || resultados == null) {
      return;
    }
    List<VacanteDTO> vacantes = resultados
      .stream()
      .filter(VacanteDTO.class::isInstance)
      .map(VacanteDTO.class::cast)
      .toList();
    List<Long> empleadorIds = vacantes
      .stream()
      .map(VacanteDTO::getEmpleadorId)
      .filter(id -> id != null)
      .distinct()
      .toList();
    Map<Long, ResumenResenas> resumenes = obtenerResumenes(empleadorIds, servicioResena);
    for (VacanteDTO vacante : vacantes) {
      ResumenResenas resumen = resumenes.get(vacante.getEmpleadorId());
      if (resumen != null) {
        vacante.setPromedioResenas(resumen.getPromedio());
        vacante.setCantidadResenas(resumen.getCantidad());
      }
    }
  }
}
