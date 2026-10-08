package com.tallerwebi.dominio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class ServicioPerfilImplTest {

  @Test
  void muestraSeleccionadasEnPerfilLasHabilidadesElegidasAlRegistrarse() {
    RepositorioUsuario repositorioUsuario = mock(RepositorioUsuario.class);
    RepositorioSkill repositorioSkill = mock(RepositorioSkill.class);
    RepositorioPerfilPostulante repositorioPerfil = mock(RepositorioPerfilPostulante.class);

    Usuario usuario = new Usuario();
    usuario.setHabilidades(List.of("Java", "HTML/CSS"));
    when(repositorioUsuario.buscarPorId(5L)).thenReturn(usuario);
    when(repositorioPerfil.buscarPorUsuarioId(5L)).thenReturn(null);
    when(repositorioSkill.buscarTodos())
      .thenReturn(
        List.of(skill(1L, "java"), skill(2L, "HTML"), skill(3L, "CSS"), skill(4L, "SQL"))
      );
    ServicioPerfilImpl servicio = new ServicioPerfilImpl(
      repositorioUsuario,
      repositorioSkill,
      repositorioPerfil
    );

    PerfilPostulante perfil = servicio.obtenerPerfil(5L);

    assertThat(perfil.getSkillIds(), containsInAnyOrder(1L, 2L, 3L));
  }

  @Test
  void guardarPerfilPersisteLasSkillsSeleccionadasParaLaSiguienteVisita() {
    RepositorioUsuario repositorioUsuario = mock(RepositorioUsuario.class);
    RepositorioSkill repositorioSkill = mock(RepositorioSkill.class);
    RepositorioPerfilPostulante repositorioPerfil = mock(RepositorioPerfilPostulante.class);
    Usuario usuario = new Usuario();
    PerfilPostulante perfilPersistido = new PerfilPostulante();
    perfilPersistido.setId(9L);
    when(repositorioUsuario.buscarPorId(5L)).thenReturn(usuario);
    when(repositorioPerfil.buscarPorUsuarioId(5L)).thenReturn(null);
    when(repositorioPerfil.guardar(any(PerfilPostulante.class))).thenReturn(perfilPersistido);
    when(repositorioSkill.buscarTodos()).thenReturn(List.of(skill(1L, "Java")));
    ServicioPerfilImpl servicio = new ServicioPerfilImpl(
      repositorioUsuario,
      repositorioSkill,
      repositorioPerfil
    );
    PerfilPostulante datos = new PerfilPostulante();
    datos.setSkillIds(List.of(1L));

    servicio.guardarPerfil(5L, datos);

    assertThat(usuario.getHabilidades(), containsInAnyOrder("Java"));
    verify(repositorioUsuario).modificar(usuario);
    verify(repositorioPerfil).guardarSkill(any(PostulanteSkill.class));
  }

  private Skill skill(Long id, String nombre) {
    Skill skill = new Skill();
    skill.setId(id);
    skill.setNombre(nombre);
    return skill;
  }
}
