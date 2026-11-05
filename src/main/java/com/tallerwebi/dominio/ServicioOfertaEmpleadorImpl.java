package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  public static final int LIMITE_OFERTAS_GRATUITAS = 3;
  public static final int LIMITE_OFERTAS_PREMIUM = 15;

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioVacante repositorioVacante;
  private final RepositorioPostulacion repositorioPostulacion;
  private final ServicioPerfil servicioPerfil;
  private final ServicioPago servicioPago;
  private final ServicioNotificacionOferta servicioNotificacionOferta;
  private final ApplicationEventPublisher eventos;
  private final String urlBase;

  @Autowired
  public ServicioOfertaEmpleadorImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioVacante repositorioVacante,
    RepositorioPostulacion repositorioPostulacion,
    ServicioPerfil servicioPerfil,
    ServicioPago servicioPago,
    ServicioNotificacionOferta servicioNotificacionOferta,
    ApplicationEventPublisher eventos,
    @Value("${app.url:http://localhost:8080/spring}") String urlBase
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioVacante = repositorioVacante;
    this.repositorioPostulacion = repositorioPostulacion;
    this.servicioPerfil = servicioPerfil;
    this.servicioPago = servicioPago;
    this.servicioNotificacionOferta = servicioNotificacionOferta;
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
    validarLimiteOfertas(vacante);
    repositorioVacante.guardar(vacante);
    if (FUENTE_LOCAL.equals(vacante.getFuente())) {
      notificarCandidatosPremium(vacante);
      avisarPostulantes(vacante);
    }
  }

  private void validarLimiteOfertas(Vacante vacante) {
    Long empleadorId = vacante.getEmpleadorId();
    if (empleadorId == null) {
      return;
    }
    long activas = repositorioVacante
      .buscarPorEmpleador(empleadorId)
      .stream()
      .filter(oferta -> oferta.getEstado() == EstadoVacante.ABIERTA)
      .filter(oferta -> !Boolean.FALSE.equals(oferta.getActiva()))
      .count();
    int limite = servicioPago.esPremiumActivo(empleadorId)
      ? LIMITE_OFERTAS_PREMIUM
      : LIMITE_OFERTAS_GRATUITAS;
    if (activas >= limite) {
      throw new LimiteOfertasAlcanzadoException(
        "Alcanzaste el límite de " +
        limite +
        " ofertas activas. Cerrá una oferta o hacete Premium para publicar más."
      );
    }
  }

  private void notificarCandidatosPremium(Vacante vacante) {
    Set<String> skills = obtenerSkills(vacante);
    if (skills.isEmpty()) {
      return;
    }
    String enlace = "/vacantes/detalle?fuente=LOCAL&id=" + vacante.getId();
    for (Usuario usuario : repositorioUsuario.buscarPostulantes()) {
      if (
        servicioPago.esPremiumActivo(usuario.getId()) &&
        coincideSkill(servicioPerfil.obtenerHabilidades(usuario.getId()), skills)
      ) {
        servicioNotificacionOferta.notificar(
          usuario,
          "Nueva oferta acorde a tus habilidades: " + vacante.getTitulo(),
          enlace
        );
      }
    }
  }

  private Set<String> obtenerSkills(Vacante vacante) {
    Set<String> skills = new HashSet<>();
    for (VacanteSkill relacion : vacante.getVacanteSkills()) {
      if (relacion.getSkill() != null && relacion.getSkill().getNombre() != null) {
        skills.add(relacion.getSkill().getNombre().trim().toLowerCase(Locale.ROOT));
      }
    }
    return skills;
  }

  private boolean coincideSkill(List<String> habilidades, Set<String> skillsOferta) {
    for (String habilidad : habilidades) {
      if (habilidad != null && skillsOferta.contains(habilidad.trim().toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
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
    List<Vacante> ofertas = new ArrayList<>(
      repositorioVacante.buscarOfertasPublicadasPorEmpleadores()
    );
    Map<Long, Boolean> estadoPremiumPorEmpleador = new HashMap<>();
    for (Vacante oferta : ofertas) {
      Long empleadorId = oferta.getEmpleadorId();
      boolean empresaPremium =
        empleadorId != null &&
        estadoPremiumPorEmpleador.computeIfAbsent(empleadorId, servicioPago::esPremiumActivo);
      oferta.setEmpresaPremium(empresaPremium);
    }
    ofertas.sort(
      Comparator
        .comparing(Vacante::isEmpresaPremium)
        .reversed()
        .thenComparing(
          Vacante::getFechaPublicacion,
          Comparator.nullsLast(Comparator.reverseOrder())
        )
    );
    return ofertas;
  }

  @Override
  public void cerrar(Long vacanteId, Long empleadorId) {
    Vacante vacante = repositorioVacante.obtenerPorIdParaActualizar(vacanteId);
    if (vacante == null) {
      throw new VacanteNoEncontradaException("La postulación no existe.");
    }
    if (empleadorId == null || !empleadorId.equals(vacante.getEmpleadorId())) {
      throw new AccesoOfertaDenegadoException();
    }
    if (vacante.getEstado() == EstadoVacante.CERRADA) {
      throw new OfertaCerradaException();
    }
    vacante.setEstado(EstadoVacante.CERRADA);
  }

  @Override
  public ResumenEmpleador obtenerResumen(Long empleadorId) {
    long activas = 0;
    long cerradas = 0;
    long postulaciones = 0;
    for (Vacante vacante : repositorioVacante.buscarPorEmpleador(empleadorId)) {
      if (vacante.getEstado() == EstadoVacante.ABIERTA) {
        activas++;
      } else {
        cerradas++;
      }
      postulaciones += repositorioPostulacion.contarPorVacante(vacante.getId());
    }
    return new ResumenEmpleador(activas, cerradas, postulaciones);
  }

  @Override
  public void registrarVista(Long vacanteId) {
    repositorioVacante.incrementarVistas(vacanteId);
  }

  @Override
  public void registrarClickPostular(Long vacanteId) {
    repositorioVacante.incrementarClicksPostular(vacanteId);
  }

  @Override
  public long obtenerVistas(Long vacanteId) {
    Vacante vacante = repositorioVacante.obtenerPorId(vacanteId);
    return vacante == null ? 0 : vacante.getVistas();
  }

  @Override
  public long obtenerClicksPostular(Long vacanteId) {
    Vacante vacante = repositorioVacante.obtenerPorId(vacanteId);
    return vacante == null ? 0 : vacante.getClicksPostular();
  }

  @Override
  public long obtenerPostulaciones(Long vacanteId) {
    return repositorioPostulacion.contarPorVacante(vacanteId);
  }
}
