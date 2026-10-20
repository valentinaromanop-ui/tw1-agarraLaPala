package com.tallerwebi.dominio;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioPostulantesOferta {

  private final RepositorioPostulacion repositorioPostulacion;
  private final RepositorioVacante repositorioVacante;
  private final RepositorioCurriculumGenerado repositorioCurriculum;
  private final RepositorioPerfilPostulante repositorioPerfil;

  @Autowired
  public ServicioPostulantesOferta(
    RepositorioPostulacion repositorioPostulacion,
    RepositorioVacante repositorioVacante,
    RepositorioCurriculumGenerado repositorioCurriculum,
    RepositorioPerfilPostulante repositorioPerfil
  ) {
    this.repositorioPostulacion = repositorioPostulacion;
    this.repositorioVacante = repositorioVacante;
    this.repositorioCurriculum = repositorioCurriculum;
    this.repositorioPerfil = repositorioPerfil;
  }

  public List<PostulanteOfertaDTO> obtenerPostulantes(Long vacanteId, Long empleadorId) {
    validarPropiedadOferta(vacanteId, empleadorId);
    List<Postulacion> postulaciones = repositorioPostulacion.buscarPorVacante(vacanteId);
    List<Long> usuarioIds = postulaciones
      .stream()
      .map(postulacion -> postulacion.getUsuario().getId())
      .distinct()
      .toList();
    Set<Long> usuarioIdsConCv = new HashSet<>(
      repositorioCurriculum.buscarUsuarioIdsConCvOriginal(usuarioIds)
    );
    return postulaciones
      .stream()
      .map(postulacion -> {
        Usuario usuario = postulacion.getUsuario();
        return new PostulanteOfertaDTO(
          postulacion.getId(),
          nombreCompleto(usuario),
          usuario.getEmail(),
          dividirHabilidades(usuario.getHabilidades()),
          postulacion.getFechaPostulacion(),
          usuarioIdsConCv.contains(usuario.getId())
        );
      })
      .toList();
  }

  public ArchivoCvPostulanteDTO obtenerCvPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    Postulacion postulacion = buscarPostulacionPropia(vacanteId, postulacionId, empleadorId);
    CurriculumGenerado curriculum = repositorioCurriculum.buscarPorUsuarioId(
      postulacion.getUsuario().getId()
    );
    if (
      curriculum == null ||
      curriculum.getArchivoOriginal() == null ||
      curriculum.getNombreArchivoOriginal() == null
    ) {
      return null;
    }
    return new ArchivoCvPostulanteDTO(
      curriculum.getNombreArchivoOriginal(),
      curriculum.getArchivoOriginal()
    );
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  public PerfilPostulanteDTO obtenerPerfilPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    Postulacion postulacion = buscarPostulacionPropia(vacanteId, postulacionId, empleadorId);
    Usuario usuario = postulacion.getUsuario();
    PerfilPostulante perfil = repositorioPerfil.buscarPorUsuarioIdParaLectura(usuario.getId());
    Vacante vacante = repositorioVacante.obtenerPorId(vacanteId);
    return new PerfilPostulanteDTO(
      nombreCompleto(usuario),
      usuario.getEmail(),
      iniciales(usuario),
      usuario.tieneFotoPerfil(),
      tieneCv(usuario.getId()),
      habilidades(usuario, perfil),
      perfil == null ? null : perfil.getPresentacion(),
      perfil == null ? usuario.getTelefono() : perfil.getTelefono(),
      perfil == null ? usuario.getUbicacion() : perfil.getUbicacion(),
      perfil == null ? null : perfil.getNivelExperiencia(),
      perfil == null ? null : perfil.getTipoBusqueda(),
      perfil == null ? null : perfil.getNivelIngles(),
      perfil == null ? null : enlaceSeguro(perfil.getLinkedin()),
      perfil == null ? null : enlaceSeguro(perfil.getGithub()),
      perfil == null ? List.of() : perfil.getDisponibilidadHoraria(),
      perfil == null ? List.of() : perfil.getDiasSemana(),
      perfil == null ? List.of() : perfil.getModalidades(),
      perfil == null ? Map.of() : perfil.getIdiomas(),
      vacante.getTitulo(),
      postulacion.getFechaPostulacion()
    );
  }

  public FotoPerfilPostulanteDTO obtenerFotoPerfilPostulante(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    Usuario usuario = buscarPostulacionPropia(vacanteId, postulacionId, empleadorId).getUsuario();
    if (!usuario.tieneFotoPerfil()) {
      return null;
    }
    return new FotoPerfilPostulanteDTO(usuario.getFotoPerfilTipo(), usuario.getFotoPerfil());
  }

  private Postulacion buscarPostulacionPropia(
    Long vacanteId,
    Long postulacionId,
    Long empleadorId
  ) {
    validarPropiedadOferta(vacanteId, empleadorId);
    Postulacion postulacion = repositorioPostulacion.buscarPorIdYVacante(postulacionId, vacanteId);
    if (postulacion == null) {
      throw new VacanteNoEncontradaException();
    }
    return postulacion;
  }

  private boolean tieneCv(Long usuarioId) {
    CurriculumGenerado curriculum = repositorioCurriculum.buscarPorUsuarioId(usuarioId);
    return (
      curriculum != null &&
      curriculum.getArchivoOriginal() != null &&
      curriculum.getNombreArchivoOriginal() != null
    );
  }

  private List<String> habilidades(Usuario usuario, PerfilPostulante perfil) {
    Set<String> habilidades = new LinkedHashSet<>();
    if (perfil != null) {
      for (PostulanteSkill postulanteSkill : perfil.getPostulanteSkills()) {
        if (
          postulanteSkill.getSkill() != null &&
          postulanteSkill.getSkill().getNombre() != null &&
          !postulanteSkill.getSkill().getNombre().isBlank()
        ) {
          habilidades.add(postulanteSkill.getSkill().getNombre().trim());
        }
      }
    }
    if (habilidades.isEmpty()) {
      habilidades.addAll(dividirHabilidades(usuario.getHabilidades()));
    }
    return List.copyOf(habilidades);
  }

  private List<String> dividirHabilidades(List<String> valores) {
    if (valores == null) {
      return List.of();
    }
    return valores
      .stream()
      .filter(valor -> valor != null && !valor.isBlank())
      .flatMap(valor -> java.util.Arrays.stream(valor.split("[,/;]")))
      .map(String::trim)
      .filter(valor -> !valor.isEmpty())
      .distinct()
      .toList();
  }

  private String enlaceSeguro(String enlace) {
    if (enlace == null || enlace.isBlank()) {
      return null;
    }
    try {
      URI uri = new URI(enlace.trim());
      String esquema = uri.getScheme();
      return (
          uri.isAbsolute() &&
          ("https".equalsIgnoreCase(esquema) || "http".equalsIgnoreCase(esquema)) &&
          uri.getHost() != null
        )
        ? uri.toASCIIString()
        : null;
    } catch (URISyntaxException excepcion) {
      return null;
    }
  }

  private String iniciales(Usuario usuario) {
    String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
    String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
    String primera = nombre.isEmpty() ? "" : nombre.substring(0, 1);
    String segunda = apellido.isEmpty() ? "" : apellido.substring(0, 1);
    String resultado = (primera + segunda).toUpperCase(java.util.Locale.ROOT);
    return resultado.isEmpty() ? "?" : resultado;
  }

  private void validarPropiedadOferta(Long vacanteId, Long empleadorId) {
    Vacante vacante = repositorioVacante.obtenerPorId(vacanteId);
    if (vacante == null || empleadorId == null || !empleadorId.equals(vacante.getEmpleadorId())) {
      throw new AccesoOfertaDenegadoException();
    }
  }

  private String nombreCompleto(Usuario usuario) {
    String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
    String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
    String completo = (nombre + " " + apellido).trim();
    return completo.isEmpty() ? "Nombre no informado" : completo;
  }
}
