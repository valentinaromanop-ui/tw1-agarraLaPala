package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioVacante;
import com.tallerwebi.dominio.Skill;
import com.tallerwebi.dominio.Vacante;
import com.tallerwebi.dominio.VacanteSkill;
import java.util.List;
import java.util.Locale;
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
  public void guardar(Vacante vacante) {
    sessionFactory.getCurrentSession().persist(vacante);
    for (VacanteSkill relacion : vacante.getVacanteSkills()) {
      Skill skill = relacion.getSkill();
      if (skill.getId() == null) {
        String nombre = skill.getNombre().trim();
        Skill existente = sessionFactory
          .getCurrentSession()
          .createQuery("from Skill where lower(nombre) = :nombre", Skill.class)
          .setParameter("nombre", nombre.toLowerCase(Locale.ROOT))
          .uniqueResult();
        if (existente == null) {
          skill.setNombre(nombre);
          sessionFactory.getCurrentSession().persist(skill);
        } else {
          skill = existente;
          relacion.setSkill(skill);
        }
      }
      relacion.setVacante(vacante);
      sessionFactory.getCurrentSession().persist(relacion);
    }
  }

  @Override
  public boolean existePorIdExterno(String idExterno) {
    return false;
  }

  @Override
  public List<Vacante> buscarPorCategoria(String categoria) {
    return List.of();
  }

  @Override
  public Vacante obtenerPorId(Long id) {
    return sessionFactory.getCurrentSession().get(Vacante.class, id);
  }

  @Override
  public Vacante buscarPorFuenteEIdExterno(String fuente, Long idExterno) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Vacante where fuente = :fuente and idExterno = :idExterno", Vacante.class)
      .setParameter("fuente", fuente)
      .setParameter("idExterno", idExterno)
      .uniqueResult();
  }

  @Override
  public List<Vacante> buscarPorSkills(List<String> skills) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "SELECT DISTINCT v FROM Vacante v " +
        "JOIN VacanteSkill vs ON v.id = vs.vacante.id " +
        "JOIN Skill s ON vs.skill.id = s.id " +
        "WHERE v.activa = true AND (v.fuente = 'LOCAL' OR v.fuente IS NULL) AND LOWER(s.nombre) IN (:skills)",
        Vacante.class
      )
      .setParameterList("skills", skills)
      .getResultList();
  }

  @Override
  public List<Vacante> buscarPorEmpleador(Long empleadorId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from Vacante where empleadorId = :empleadorId order by fechaPublicacion desc",
        Vacante.class
      )
      .setParameter("empleadorId", empleadorId)
      .getResultList();
  }

  @Override
  public List<Vacante> buscarOfertasPublicadasPorEmpleadores() {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "SELECT DISTINCT v FROM Vacante v " +
        "LEFT JOIN FETCH v.vacanteSkills vs " +
        "LEFT JOIN FETCH vs.skill " +
        "WHERE v.empleadorId IS NOT NULL AND v.activa = true " +
        "ORDER BY v.fechaPublicacion DESC",
        Vacante.class
      )
      .getResultList();
  }
}
