package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.EstadoReporte;
import com.tallerwebi.dominio.Reporte;
import com.tallerwebi.dominio.RepositorioReporte;
import com.tallerwebi.dominio.TipoReporte;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioReporte")
public class RepositorioReporteImpl implements RepositorioReporte {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioReporteImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardar(Reporte reporte) {
    sessionFactory.getCurrentSession().persist(reporte);
  }

  @Override
  public Reporte obtener(Long id) {
    return sessionFactory.getCurrentSession().get(Reporte.class, id);
  }

  @Override
  public List<Reporte> listarPendientes() {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Reporte r where r.estado = :estado order by r.fecha desc", Reporte.class)
      .setParameter("estado", EstadoReporte.PENDIENTE)
      .getResultList();
  }

  @Override
  public boolean existe(TipoReporte tipo, Long objetoId, Long reportanteId) {
    Long cantidad = sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(r) from Reporte r where r.tipo = :tipo " +
        "and r.objetoId = :objetoId and r.reportanteId = :reportanteId",
        Long.class
      )
      .setParameter("tipo", tipo)
      .setParameter("objetoId", objetoId)
      .setParameter("reportanteId", reportanteId)
      .getSingleResult();
    return cantidad > 0;
  }

  @Override
  public long contarDesde(Long reportanteId, LocalDateTime desde) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(r) from Reporte r where r.reportanteId = :reportanteId and r.fecha >= :desde",
        Long.class
      )
      .setParameter("reportanteId", reportanteId)
      .setParameter("desde", desde)
      .getSingleResult();
  }

  @Override
  public long contarResueltosContra(Long usuarioId) {
    return sessionFactory
      .getCurrentSession()
      .createQuery(
        "select count(r) from Reporte r where r.usuarioReportadoId = :usuarioId and r.estado = :estado",
        Long.class
      )
      .setParameter("usuarioId", usuarioId)
      .setParameter("estado", EstadoReporte.RESUELTO)
      .getSingleResult();
  }
}
