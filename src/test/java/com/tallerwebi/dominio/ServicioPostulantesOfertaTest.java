package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServicioPostulantesOfertaTest {

  @Test
  void priorizaCandidatosPremiumYDespuesOrdenaPorFecha() {
    RepositorioPostulacion repositorioPostulacion = mock(RepositorioPostulacion.class);
    RepositorioVacante repositorioVacante = mock(RepositorioVacante.class);
    RepositorioCurriculumGenerado repositorioCurriculum = mock(RepositorioCurriculumGenerado.class);
    RepositorioPerfilPostulante repositorioPerfil = mock(RepositorioPerfilPostulante.class);
    ServicioPago servicioPago = mock(ServicioPago.class);
    Usuario premium = usuario(1L);
    Usuario gratuito = usuario(2L);
    Postulacion postulacionRecienteGratuita = postulacion(gratuito, LocalDateTime.now());
    Postulacion postulacionAnteriorPremium = postulacion(premium, LocalDateTime.now().minusDays(1));
    Vacante oferta = new Vacante();
    oferta.setId(10L);
    oferta.setEmpleadorId(42L);
    when(repositorioVacante.obtenerPorId(10L)).thenReturn(oferta);
    when(repositorioPostulacion.buscarPorVacante(10L))
      .thenReturn(List.of(postulacionRecienteGratuita, postulacionAnteriorPremium));
    when(repositorioCurriculum.buscarUsuarioIdsConCvOriginal(anyList())).thenReturn(List.of());
    when(servicioPago.esPremiumActivo(1L)).thenReturn(true);

    ServicioPostulantesOferta servicio = new ServicioPostulantesOferta(
      repositorioPostulacion,
      repositorioVacante,
      repositorioCurriculum,
      repositorioPerfil,
      servicioPago
    );

    List<PostulanteOfertaDTO> resultado = servicio.obtenerPostulantes(10L, 42L);

    assertEquals(1L, resultado.getFirst().getPostulacionId());
    assertTrue(resultado.getFirst().isPremium());
    assertEquals(2L, resultado.getLast().getPostulacionId());
    assertEquals(false, resultado.getLast().isPremium());
  }

  private Usuario usuario(Long id) {
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setRol("CANDIDATO");
    usuario.setNombre("Candidato");
    return usuario;
  }

  private Postulacion postulacion(Usuario usuario, LocalDateTime fecha) {
    Postulacion postulacion = new Postulacion();
    postulacion.setId(usuario.getId());
    postulacion.setUsuario(usuario);
    postulacion.setFechaPostulacion(fecha);
    return postulacion;
  }
}
