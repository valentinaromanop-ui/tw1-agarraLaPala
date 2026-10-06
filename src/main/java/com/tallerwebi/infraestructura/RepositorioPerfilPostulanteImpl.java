package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.PerfilPostulante;
import com.tallerwebi.dominio.PostulanteSkill;
import com.tallerwebi.dominio.RepositorioPerfilPostulante;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPerfilPostulante")
public class RepositorioPerfilPostulanteImpl implements RepositorioPerfilPostulante {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioPerfilPostulanteImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public PerfilPostulante buscarPorUsuarioId(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select distinct p from PerfilPostulante p " +
        "left join fetch p.postulanteSkills ps " +
        "left join fetch ps.skill where p.usuario.id = :usuarioId",
        PerfilPostulante.class
      )
      .setParameter("usuarioId", usuarioId)
      .uniqueResult();
  }

  @Override
  public PerfilPostulante guardar(PerfilPostulante perfil) {
    if (perfil.getId() == null) {
      sessionFactory.getCurrentSession().persist(perfil);
      return perfil;
    }
    return sessionFactory.getCurrentSession().merge(perfil);
  }

  @Override
  public void eliminarSkills(Long perfilId) {
    sessionFactory
      .getCurrentSession()
      .createMutationQuery("delete from PostulanteSkill where postulante.id = :perfilId")
      .setParameter("perfilId", perfilId)
      .executeUpdate();
  }

  @Override
  public void guardarSkill(PostulanteSkill postulanteSkill) {
    sessionFactory.getCurrentSession().persist(postulanteSkill);
  }
}
