package com.tallerwebi.dominio;

import java.util.List;

public record CursoPremium(
  String id,
  String skill,
  String titulo,
  String instructor,
  String nivel,
  String duracion,
  String descripcion,
  List<String> aprenderas,
  List<Leccion> lecciones
) {}
