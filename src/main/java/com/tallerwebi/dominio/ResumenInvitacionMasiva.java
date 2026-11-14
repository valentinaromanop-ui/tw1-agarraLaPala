package com.tallerwebi.dominio;

public class ResumenInvitacionMasiva {

  private final int invitados;
  private final int omitidosPorPostulacion;
  private final int omitidosPorInvitacionExistente;
  private final int omitidosPorVisibilidad;

  public ResumenInvitacionMasiva(
    int invitados,
    int omitidosPorPostulacion,
    int omitidosPorInvitacionExistente,
    int omitidosPorVisibilidad
  ) {
    this.invitados = invitados;
    this.omitidosPorPostulacion = omitidosPorPostulacion;
    this.omitidosPorInvitacionExistente = omitidosPorInvitacionExistente;
    this.omitidosPorVisibilidad = omitidosPorVisibilidad;
  }

  public int getInvitados() {
    return invitados;
  }

  public int getOmitidos() {
    return omitidosPorPostulacion + omitidosPorInvitacionExistente + omitidosPorVisibilidad;
  }

  public int getOmitidosPorPostulacion() {
    return omitidosPorPostulacion;
  }

  public int getOmitidosPorInvitacionExistente() {
    return omitidosPorInvitacionExistente;
  }

  public int getOmitidosPorVisibilidad() {
    return omitidosPorVisibilidad;
  }
}
