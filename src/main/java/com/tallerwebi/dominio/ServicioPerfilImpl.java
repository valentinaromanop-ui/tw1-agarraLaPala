package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioPerfilImpl implements ServicioPerfil {

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioSkill repositorioSkill;
  private final RepositorioPerfilPostulante repositorioPerfil;

  @Autowired
  public ServicioPerfilImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioSkill repositorioSkill,
    RepositorioPerfilPostulante repositorioPerfil
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioSkill = repositorioSkill;
    this.repositorioPerfil = repositorioPerfil;
  }

  @Override
  public PerfilPostulante obtenerPerfil(Long usuarioId) {
    PerfilPostulante perfil = repositorioPerfil.buscarPorUsuarioId(usuarioId);
    if (perfil != null) {
      Hibernate.initialize(perfil.getDisponibilidadHoraria());
      Hibernate.initialize(perfil.getDiasSemana());
      Hibernate.initialize(perfil.getModalidades());
      Hibernate.initialize(perfil.getPostulanteSkills());
    }
    return perfil;
  }

  @Override
  public List<Skill> obtenerSkills() {
    return repositorioSkill.buscarTodos();
  }

  @Override
  public void guardarPerfil(Long usuarioId, PerfilPostulante datos) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    PerfilPostulante perfil = repositorioPerfil.buscarPorUsuarioId(usuarioId);
    if (perfil == null) {
      perfil = new PerfilPostulante();
      perfil.setUsuario(usuario);
    }
    perfil.setTelefono(datos.getTelefono());
    perfil.setUbicacion(datos.getUbicacion());
    perfil.setDisponibilidadHoraria(datos.getDisponibilidadHoraria());
    perfil.setDiasSemana(datos.getDiasSemana());
    perfil.setModalidades(datos.getModalidades());
    perfil.setNivelExperiencia(datos.getNivelExperiencia());
    perfil.setTipoBusqueda(datos.getTipoBusqueda());
    perfil.setNivelIngles(datos.getNivelIngles());
    perfil.setLinkedin(datos.getLinkedin());
    perfil.setGithub(datos.getGithub());
    perfil.setPresentacion(datos.getPresentacion());
    perfil.setIdiomas(datos.getIdiomas());
    usuario.setTelefono(datos.getTelefono());
    usuario.setUbicacion(datos.getUbicacion());
    usuario.setIdiomas(datos.getIdiomas());
    usuario.setHabilidades(obtenerNombresSkills(datos.getSkillIds()));
    repositorioUsuario.modificar(usuario);
    perfil = repositorioPerfil.guardar(perfil);
    repositorioPerfil.eliminarSkills(perfil.getId());
    for (Skill skill : repositorioSkill.buscarTodos()) {
      if (datos.getSkillIds() != null && datos.getSkillIds().contains(skill.getId())) {
        PostulanteSkill postulanteSkill = new PostulanteSkill();
        postulanteSkill.setPostulante(perfil);
        postulanteSkill.setSkill(skill);
        repositorioPerfil.guardarSkill(postulanteSkill);
      }
    }
  }

  private List<String> obtenerNombresSkills(List<Long> skillIds) {
    List<String> nombres = new ArrayList<>();
    if (skillIds == null) {
      return nombres;
    }
    for (Skill skill : repositorioSkill.buscarTodos()) {
      if (skillIds.contains(skill.getId())) {
        nombres.add(skill.getNombre());
      }
    }
    return nombres;
  }
}
