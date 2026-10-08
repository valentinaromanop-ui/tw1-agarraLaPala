package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.ComparadorTituloVacante;
import com.tallerwebi.dominio.VacanteDTO;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class CatalogoVacantesPrueba {

  private static final String MODALIDAD_REMOTA = "Remota";
  private static final List<String> CARGOS_PRUEBA = crearCargosPrueba();
  private final List<VacanteDTO> vacantes = crearVacantesPrueba();

  public List<String> obtenerTitulos(String consulta) {
    return obtenerTitulos(consulta, List.of());
  }

  public List<String> obtenerTitulos(String consulta, List<String> titulosAdicionales) {
    Set<String> titulos = new TreeSet<>(new ComparadorTituloVacante());
    String consultaNormalizada = normalizar(consulta);
    for (String cargo : CARGOS_PRUEBA) {
      if (normalizar(cargo).contains(consultaNormalizada)) {
        titulos.add(cargo);
      }
    }
    for (String cargo : titulosAdicionales) {
      if (normalizar(cargo).contains(consultaNormalizada)) {
        titulos.add(cargo);
      }
    }
    return new ArrayList<>(titulos);
  }

  public List<VacanteDTO> buscar(
    String texto,
    String zona,
    String modalidad,
    String skills,
    String fecha
  ) {
    return buscar(texto, zona, modalidad, skills, fecha, List.of());
  }

  public List<VacanteDTO> buscar(
    String texto,
    String zona,
    String modalidad,
    String skills,
    String fecha,
    List<VacanteDTO> ofertasAdicionales
  ) {
    List<VacanteDTO> resultado = new ArrayList<>();
    List<VacanteDTO> todasLasVacantes = new ArrayList<>(vacantes);
    todasLasVacantes.addAll(ofertasAdicionales);
    for (VacanteDTO vacante : todasLasVacantes) {
      if (
        coincideTexto(vacante, texto) &&
        coincide(vacante.getUbicacion(), zona) &&
        coincideModalidad(vacante.getModalidad(), modalidad) &&
        coincideSkills(vacante, skills) &&
        coincideFecha(vacante, fecha)
      ) {
        resultado.add(vacante);
      }
    }
    return resultado;
  }

  private boolean coincideTexto(VacanteDTO vacante, String texto) {
    String busqueda = normalizar(texto);
    return (
      busqueda.isEmpty() ||
      normalizar(vacante.getTitulo()).contains(busqueda) ||
      normalizar(vacante.getEmpresa()).contains(busqueda) ||
      normalizar(vacante.getDescripcion()).contains(busqueda)
    );
  }

  private boolean coincide(String valorVacante, String filtro) {
    String valorFiltro = normalizar(filtro);
    return valorFiltro.isEmpty() || normalizar(valorVacante).contains(valorFiltro);
  }

  private boolean coincideModalidad(String modalidadVacante, String filtro) {
    String modalidad = normalizar(modalidadVacante);
    String modalidadFiltrada = normalizar(filtro);
    if (modalidad.startsWith("hibrid")) {
      modalidad = "hibrida";
    } else if (modalidad.startsWith("remot")) {
      modalidad = "remota";
    }
    if (modalidadFiltrada.startsWith("hibrid")) {
      modalidadFiltrada = "hibrida";
    } else if (modalidadFiltrada.startsWith("remot")) {
      modalidadFiltrada = "remota";
    }
    return modalidadFiltrada.isEmpty() || modalidad.equals(modalidadFiltrada);
  }

  private boolean coincideSkills(VacanteDTO vacante, String skills) {
    if (skills == null || skills.trim().isEmpty()) {
      return true;
    }
    for (String skill : skills.split(",")) {
      String skillNormalizada = normalizar(skill);
      for (String skillVacante : vacante.getSkills()) {
        if (normalizar(skillVacante).equals(skillNormalizada)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean coincideFecha(VacanteDTO vacante, String fecha) {
    if (fecha == null || fecha.isEmpty()) {
      return true;
    }
    LocalDateTime limite =
      switch (fecha) {
        case "24h" -> LocalDateTime.now().minusHours(24);
        case "semana" -> LocalDateTime.now().minusWeeks(1);
        case "mes" -> LocalDateTime.now().minusMonths(1);
        default -> null;
      };
    if (limite == null) {
      return true;
    }
    return vacante.getFechaPublicacion() != null && !vacante.getFechaPublicacion().isBefore(limite);
  }

  private static String normalizar(String valor) {
    if (valor == null) {
      return "";
    }
    String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return sinTildes.toLowerCase(Locale.ROOT).trim();
  }

  private static List<String> crearCargosPrueba() {
    // Reemplazar esta lista fija por títulos del repositorio cuando esté disponible.
    List<String> cargos = new ArrayList<>(
      Arrays.asList(
        "Analista de Datos",
        "Desarrollador Backend",
        "Desarrollador Frontend",
        "DevOps",
        "Diseñador UX/UI",
        "QA Tester",
        "Project Manager"
      )
    );
    Collections.sort(cargos, new ComparadorTituloVacante());
    return Collections.unmodifiableList(cargos);
  }

  private static List<VacanteDTO> crearVacantesPrueba() {
    List<VacanteDTO> muestras = new ArrayList<>();
    muestras.add(
      crearVacante(
        1L,
        "Analista de Datos",
        "Sur Analytics",
        "Buenos Aires",
        "Híbrida",
        Arrays.asList("SQL", "Python", "Power BI"),
        8
      )
    );
    muestras.add(
      crearVacante(
        2L,
        "Desarrollador Backend",
        "Cauce Tech",
        MODALIDAD_REMOTA,
        MODALIDAD_REMOTA,
        Arrays.asList("Java", "Spring", "SQL"),
        2
      )
    );
    muestras.add(
      crearVacante(
        3L,
        "Desarrollador Frontend",
        "Andes Digital",
        "Buenos Aires",
        "Híbrida",
        Arrays.asList("JavaScript", "HTML", "CSS"),
        1
      )
    );
    muestras.add(
      crearVacante(
        4L,
        "DevOps",
        "Nube Sur",
        MODALIDAD_REMOTA,
        MODALIDAD_REMOTA,
        Arrays.asList("AWS", "Docker", "Kubernetes"),
        16
      )
    );
    muestras.add(
      crearVacante(
        5L,
        "Diseñador UX/UI",
        "Estudio Norte",
        "Córdoba",
        "Presencial",
        Arrays.asList("Figma", "UX", "Investigación"),
        3
      )
    );
    muestras.add(
      crearVacante(
        6L,
        "QA Tester",
        "Prisma Software",
        "Buenos Aires",
        "Presencial",
        Arrays.asList("QA", "Selenium", "Automatización"),
        10
      )
    );
    muestras.add(
      crearVacante(
        7L,
        "Project Manager",
        "Verde Estudio",
        "Rosario",
        "Híbrida",
        Arrays.asList("Agile", "Scrum", "Jira"),
        25
      )
    );
    return Collections.unmodifiableList(muestras);
  }

  private static VacanteDTO crearVacante(
    Long id,
    String titulo,
    String empresa,
    String ubicacion,
    String modalidad,
    List<String> skills,
    int horasAtras
  ) {
    VacanteDTO vacante = new VacanteDTO();
    vacante.setId(id);
    vacante.setTitulo(titulo);
    vacante.setEmpresa(empresa);
    vacante.setDescripcion(titulo + " en " + empresa);
    vacante.setUbicacion(ubicacion);
    vacante.setModalidad(modalidad);
    vacante.setSkills(skills);
    vacante.setUrl("https://example.com/ofertas/" + id);
    vacante.setFechaPublicacion(LocalDateTime.now().minusHours(horasAtras));
    vacante.setFuente("Oferta de prueba");
    return vacante;
  }
}
