package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioOfertaEmpleadorImpl implements ServicioOfertaEmpleador {

  private static final String FUENTE_LOCAL = "LOCAL";
  private static final int PORCENTAJE_MINIMO = 75;

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;
  private final ServicioPerfil servicioPerfil;
  private final ApplicationEventPublisher eventos;
  private final String urlBase;

  @Autowired
  public ServicioOfertaEmpleadorImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante,
    ServicioPerfil servicioPerfil,
    ApplicationEventPublisher eventos,
    @Value("${app.url:http://localhost:8080/spring}") String urlBase
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
    this.servicioPerfil = servicioPerfil;
    this.eventos = eventos;
    this.urlBase = urlBase;
  }

  @Override
  public Usuario obtenerEmpleador(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (usuario == null || !"EMPLEADOR".equalsIgnoreCase(usuario.getRol())) {
      return null;
    }
    return usuario;
  }

  @Override
  public void publicar(Vacante vacante) {
    repositorioVacante.guardar(vacante);
    if (FUENTE_LOCAL.equals(vacante.getFuente())) {
      avisarPostulantes(vacante);
    }
  }

  private void avisarPostulantes(Vacante vacante) {
    Set<String> skills = new HashSet<>();
    for (VacanteSkill relacion : vacante.getVacanteSkills()) {
      skills.add(relacion.getSkill().getNombre().trim().toLowerCase(Locale.ROOT));
    }
    if (skills.isEmpty()) {
      return;
    }
    List<String> destinatarios = obtenerDestinatarios(skills);
    if (!destinatarios.isEmpty()) {
      String mensaje =
        "Se publicó una vacante acorde a tus habilidades: " +
        vacante.getTitulo() +
        "\nEmpresa: " +
        vacante.getEmpresa() +
        "\nVer oferta: " +
        urlBase +
        "/vacantes/detalle?fuente=LOCAL&id=" +
        vacante.getId();
      // Spring entrega este correo solo si la transacción se confirma.
      eventos.publishEvent(
        new Correo(destinatarios, "Nueva vacante: " + vacante.getTitulo(), mensaje)
      );
    }
  }

  private List<String> obtenerDestinatarios(Set<String> skills) {
    List<String> destinatarios = new ArrayList<>();
    for (Usuario usuario : repositorioUsuario.buscarPostulantes()) {
      Set<String> coincidencias = new HashSet<>();
      for (String habilidad : servicioPerfil.obtenerHabilidades(usuario.getId())) {
        String nombre = habilidad.trim().toLowerCase(Locale.ROOT);
        if (skills.contains(nombre)) {
          coincidencias.add(nombre);
        }
      }
      double porcentaje = (coincidencias.size() * 100.0) / skills.size();
      if (porcentaje >= PORCENTAJE_MINIMO && !destinatarios.contains(usuario.getEmail())) {
        destinatarios.add(usuario.getEmail());
      }
    }
    return destinatarios;
  }

  @Override
  public List<Vacante> listarPorEmpleador(Long empleadorId) {
    return repositorioVacante.buscarPorEmpleador(empleadorId);
  }

  @Override
  public List<Vacante> listarOfertasPublicadas() {
    return repositorioVacante.buscarOfertasPublicadasPorEmpleadores();
  }
}
