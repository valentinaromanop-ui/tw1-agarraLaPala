package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioSkill;
import com.tallerwebi.dominio.Skill;
import java.util.List;
import java.util.Locale;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioSkill")
public class RepositorioSkillImpl implements RepositorioSkill {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioSkillImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<Skill> buscarTodos() {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Skill order by nombre", Skill.class)
      .getResultList();
  }

  @Override
  public List<Skill> buscarPorNombre(String nombre) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Skill where lower(nombre) like :nombre order by nombre", Skill.class)
      .setParameter("nombre", "%" + nombre.toLowerCase(Locale.ROOT) + "%")
      .getResultList();
  }
}
