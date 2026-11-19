package com.tallerwebi.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CatalogoCursosPremium {

  private static final String VIDEO_PENDIENTE = "ID_VIDEO";
  private static final List<CursoPremium> CURSOS = List.of(
    new CursoPremium(
      "java-backend",
      "Java",
      "Java para Backend: de cero a tu primera API",
      "Laura Benítez · Senior Java Developer",
      "Principiante",
      "4 h",
      "Aprendé los fundamentos de Java y armá una API REST como las que piden las empresas.",
      List.of("Sintaxis y orientación a objetos", "Colecciones y streams", "Tu primera API REST"),
      List.of(
        new Leccion("Introducción y entorno", "12 min", "_WJkT42l7Wk"),
        new Leccion("Clases y objetos", "25 min", "UP0gmnj4cOo"),
        new Leccion("Colecciones y streams", "30 min", VIDEO_PENDIENTE),
        new Leccion("Proyecto final: API REST", "45 min", VIDEO_PENDIENTE)
      )
    ),
    new CursoPremium(
      "sql-datos",
      "SQL",
      "SQL para trabajar con datos reales",
      "Martín Ocampo · Data Engineer",
      "Principiante",
      "3 h",
      "Consultas, joins y buenas prácticas para analizar y modelar datos.",
      List.of("SELECT, filtros y orden", "JOIN entre tablas", "Agrupaciones y subconsultas"),
      List.of(
        new Leccion("Primeras consultas", "15 min", VIDEO_PENDIENTE),
        new Leccion("JOIN paso a paso", "28 min", VIDEO_PENDIENTE),
        new Leccion("GROUP BY y subconsultas", "30 min", VIDEO_PENDIENTE)
      )
    ),
    new CursoPremium(
      "spring-boot",
      "Spring",
      "Spring MVC: construí una app web completa",
      "Carolina Rey · Backend Lead",
      "Intermedio",
      "5 h",
      "Controladores, servicios y persistencia con la arquitectura que usan los equipos.",
      List.of("Arquitectura en capas", "Hibernate y repositorios", "Vistas con Thymeleaf"),
      List.of(
        new Leccion("Arquitectura en capas", "20 min", VIDEO_PENDIENTE),
        new Leccion("Persistencia con Hibernate", "35 min", VIDEO_PENDIENTE),
        new Leccion("Vistas con Thymeleaf", "30 min", VIDEO_PENDIENTE)
      )
    )
  );

  private CatalogoCursosPremium() {}

  public static List<CursoPremium> todos() {
    return CURSOS;
  }

  public static Optional<CursoPremium> buscar(String id) {
    return CURSOS.stream().filter(curso -> curso.id().equals(id)).findFirst();
  }

  public static Map<String, String> idsPorHabilidad(List<String> habilidades) {
    Map<String, String> ids = new LinkedHashMap<>();
    for (String habilidad : habilidades) {
      String normalizada = habilidad.toLowerCase(Locale.ROOT).trim();
      Optional<CursoPremium> curso = CURSOS
        .stream()
        .filter(item ->
          normalizada.contains(item.skill().toLowerCase(Locale.ROOT)) ||
          item.skill().toLowerCase(Locale.ROOT).contains(normalizada)
        )
        .findFirst();
      ids.put(habilidad, curso.map(CursoPremium::id).orElse(""));
    }
    return ids;
  }
}
