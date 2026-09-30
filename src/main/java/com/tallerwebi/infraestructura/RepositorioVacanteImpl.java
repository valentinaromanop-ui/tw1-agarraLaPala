package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.Vacante;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioVacanteImpl implements RepositorioVacante {

  private SessionFactory sessionFactory;

  @Autowired
  public RepositorioVacanteImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public List<Vacante> buscarPorSkills(List<String> skills) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "SELECT DISTINCT v FROM Vacante v " +
        "JOIN VacanteSkill vs ON v.id = vs.vacante.id " +
        "JOIN Skill s ON vs.skill.id = s.id " +
        "WHERE v.activa = true AND LOWER(s.nombre) IN (:skills)", Vacante.class)
      .setParameterList("skills", skills)
      .getResultList();
  }
}
