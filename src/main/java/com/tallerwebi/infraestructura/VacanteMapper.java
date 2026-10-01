package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteDTO;
import com.tallerwebi.dominio.VacanteSkill;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VacanteMapper {

  public VacanteDTO toDTO(Vacante vacante, List<String> skillsBuscadas) {
    VacanteDTO dto = new VacanteDTO();
    dto.setId(vacante.getId());
    dto.setTitulo(vacante.getTitulo());
    dto.setEmpresa(vacante.getEmpresa());
    dto.setDescripcion(vacante.getDescripcion());
    dto.setUbicacion(vacante.getUbicacion());
    dto.setModalidad(vacante.getModalidad());
    dto.setFechaPublicacion(vacante.getFechaPublicacion());
    dto.setFuente("LOCAL");
    List<String> coincidencias = new ArrayList<>();

    for (VacanteSkill relacion : vacante.getVacanteSkills()) {
      String nombre = relacion.getSkill().getNombre().toLowerCase();
      if (skillsBuscadas.contains(nombre) && !coincidencias.contains(nombre)) {
        coincidencias.add(nombre);
      }
    }
    dto.setSkills(coincidencias);
    return dto;
  }
}
