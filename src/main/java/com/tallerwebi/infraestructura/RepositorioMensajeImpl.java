package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Mensaje;
import com.tallerwebi.dominio.RepositorioMensaje;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioMensajeImpl implements RepositorioMensaje {

  @Override
  public Mensaje guardarMensaje(Mensaje mensaje) {
    return null;
  }

  @Override
  public List<Mensaje> listarPorPostulacion(Long id) {
    return List.of();
  }
}
