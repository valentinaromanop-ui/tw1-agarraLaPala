package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioVistaPerfil;
import com.tallerwebi.dominio.TipoVistaPerfil;
import com.tallerwebi.dominio.VistaPerfil;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioVistaPerfilImpl implements RepositorioVistaPerfil {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioVistaPerfilImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public VistaPerfil guardar(VistaPerfil vista) {
    sessionFactory.getCurrentSession().persist(vista);
    return vista;
  }

  @Override
  public VistaPerfil buscarReciente(
    Long postulanteId,
    Long empleadorId,
    Long ofertaId,
    TipoVistaPerfil tipo,
    LocalDateTime desde
  ) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from VistaPerfil where postulante.id = :postulanteId and empleador.id = :empleadorId " +
        "and ((ofertaId = :ofertaId) or (ofertaId is null and :ofertaId is null)) " +
        "and tipo = :tipo and fecha >= :desde order by fecha desc",
        VistaPerfil.class
      )
      .setParameter("postulanteId", postulanteId)
      .setParameter("empleadorId", empleadorId)
      .setParameter("ofertaId", ofertaId)
      .setParameter("tipo", tipo)
      .setParameter("desde", desde)
      .setMaxResults(1)
      .uniqueResult();
  }

  @Override
  public List<VistaPerfil> listarUltimas(Long postulanteId, int limite) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "from VistaPerfil where postulante.id = :postulanteId order by fecha desc",
        VistaPerfil.class
      )
      .setParameter("postulanteId", postulanteId)
      .setMaxResults(limite)
      .getResultList();
  }

  @Override
  public long contar(Long postulanteId, TipoVistaPerfil tipo) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(v.id) from VistaPerfil v where v.postulante.id = :postulanteId and v.tipo = :tipo",
        Long.class
      )
      .setParameter("postulanteId", postulanteId)
      .setParameter("tipo", tipo)
      .getSingleResult();
  }
}
