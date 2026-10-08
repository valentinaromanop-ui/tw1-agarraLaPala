package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioCurriculumGeneradoImpl implements ServicioCurriculumGenerado {

  private static final int TAMANIO_MAXIMO_CV = 20 * 1024 * 1024;

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPerfilPostulante repositorioPerfil;
  private final RepositorioCurriculumGenerado repositorioCurriculum;
  private final ServicioCV servicioCV;
  private final ServicioIA servicioIA;
  private final GeneradorPdfAts generadorPdf;

  @Autowired
  public ServicioCurriculumGeneradoImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioPerfilPostulante repositorioPerfil,
    RepositorioCurriculumGenerado repositorioCurriculum,
    ServicioCV servicioCV,
    ServicioIA servicioIA,
    GeneradorPdfAts generadorPdf
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPerfil = repositorioPerfil;
    this.repositorioCurriculum = repositorioCurriculum;
    this.servicioCV = servicioCV;
    this.servicioIA = servicioIA;
    this.generadorPdf = generadorPdf;
  }

  @Override
  public CurriculumGenerado subirOriginal(Long usuarioId, String nombreArchivo, byte[] archivo)
    throws IOException {
    validarTamanioArchivo(archivo);
    Usuario usuario = obtenerPostulanteParaActualizar(usuarioId);
    verificarGeneracionDisponible(usuarioId);
    validarArchivo(nombreArchivo, archivo);
    String textoOriginal = extraerTexto(archivo);
    return guardarOriginal(usuario, nombreArchivo, archivo, textoOriginal);
  }

  @Override
  public CurriculumGenerado generarAts(Long usuarioId) {
    Usuario usuario = obtenerPostulanteParaActualizar(usuarioId);
    CurriculumGenerado curriculum = repositorioCurriculum.buscarPorUsuarioIdParaActualizar(
      usuarioId
    );
    if (curriculum == null) {
      throw new IllegalArgumentException("Primero tenés que subir tu CV original.");
    }
    if (curriculum.getContenidoAts() != null) {
      if (curriculum.getArchivoAtsPdf() == null) {
        curriculum.setArchivoAtsPdf(crearPdfAts(curriculum.getContenidoAts()));
        return repositorioCurriculum.guardar(curriculum);
      }
      throw new CurriculumGratisYaGeneradoException();
    }
    String contenidoAts = servicioIA.generarCvAts(
      curriculum.getTextoOriginal(),
      obtenerHabilidades(usuarioId, usuario)
    );
    if (contenidoAts == null || contenidoAts.isBlank()) {
      throw new IllegalStateException("La IA no devolvió un CV ATS.");
    }
    curriculum.setContenidoAts(contenidoAts);
    curriculum.setArchivoAtsPdf(crearPdfAts(contenidoAts));
    return repositorioCurriculum.guardar(curriculum);
  }

  @Override
  public CurriculumGenerado obtener(Long usuarioId) {
    return repositorioCurriculum.buscarPorUsuarioId(usuarioId);
  }

  private String obtenerHabilidades(Long usuarioId, Usuario usuario) {
    Set<String> habilidades = new LinkedHashSet<>(usuario.getHabilidades());
    PerfilPostulante perfil = repositorioPerfil.buscarPorUsuarioId(usuarioId);
    if (perfil == null) {
      return String.join(", ", habilidades);
    }
    for (PostulanteSkill habilidad : perfil.getPostulanteSkills()) {
      if (habilidad.getSkill() != null && habilidad.getSkill().getNombre() != null) {
        habilidades.add(habilidad.getSkill().getNombre());
      }
    }
    List<String> ordenadas = new ArrayList<>(habilidades);
    return String.join(", ", ordenadas);
  }

  private byte[] crearPdfAts(String contenido) {
    try {
      return generadorPdf.generar(contenido);
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo crear el PDF ATS.", e);
    }
  }

  private void validarTamanioArchivo(byte[] archivo) {
    if (archivo == null || archivo.length == 0 || archivo.length > TAMANIO_MAXIMO_CV) {
      throw new ArchivoCvInvalidoException();
    }
  }

  private Usuario obtenerPostulanteParaActualizar(Long usuarioId) {
    Usuario usuario = repositorioUsuario.buscarPorIdParaActualizar(usuarioId);
    if (usuario == null || !esPostulante(usuario.getRol())) {
      throw new IllegalArgumentException("Se requiere una cuenta de postulante.");
    }
    return usuario;
  }

  private void verificarGeneracionDisponible(Long usuarioId) {
    if (repositorioCurriculum.buscarPorUsuarioId(usuarioId) != null) {
      throw new IllegalStateException("Ya existe un CV original guardado en esta cuenta.");
    }
  }

  private void validarArchivo(String nombreArchivo, byte[] archivo) throws IOException {
    if (!servicioCV.esArchivoValido(nombreArchivo, archivo)) {
      throw new ArchivoCvInvalidoException();
    }
  }

  private String extraerTexto(byte[] archivo) throws IOException {
    String texto = servicioCV.extraerTexto(archivo);
    if (texto == null || texto.isBlank()) {
      throw new ArchivoCvInvalidoException();
    }
    return texto;
  }

  private CurriculumGenerado guardarOriginal(
    Usuario usuario,
    String nombreArchivo,
    byte[] archivo,
    String textoOriginal
  ) {
    CurriculumGenerado curriculum = new CurriculumGenerado();
    curriculum.setUsuario(usuario);
    curriculum.setNombreArchivoOriginal(nombreSeguro(nombreArchivo));
    curriculum.setArchivoOriginal(archivo);
    curriculum.setTextoOriginal(textoOriginal);
    curriculum.setGeneradoEn(LocalDateTime.now());
    curriculum.setDescargaUsada(false);
    return repositorioCurriculum.guardar(curriculum);
  }

  private boolean esPostulante(String rol) {
    return ("CANDIDATO".equalsIgnoreCase(rol) || "POSTULANTE".equalsIgnoreCase(rol));
  }

  private String nombreSeguro(String nombreArchivo) {
    if (nombreArchivo == null) {
      return "curriculum";
    }
    String nombre = nombreArchivo.replace('\\', '/');
    int separador = nombre.lastIndexOf('/');
    String base = separador >= 0 ? nombre.substring(separador + 1) : nombre;
    return base.length() <= 255 ? base : base.substring(0, 255);
  }
}
