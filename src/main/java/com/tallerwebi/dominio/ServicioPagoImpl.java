package com.tallerwebi.dominio;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioPagoImpl implements ServicioPago {

  private static final BigDecimal MONTO = new BigDecimal("5.00");
  private static final int DIAS = 30;
  private static final String MONEDA = "USD";
  private static final String NUMERO_RECHAZADO = "4000000000000002";
  private static final Pattern NUMERO_VALIDO = Pattern.compile("^\\d{16}$");
  private static final Pattern CVV_VALIDO = Pattern.compile("^\\d{3}$");
  private static final DateTimeFormatter FORMATO_VENCIMIENTO = DateTimeFormatter.ofPattern("MM/yy");

  private final RepositorioUsuario repositorioUsuario;
  private final RepositorioPago repositorioPago;
  private final RepositorioSuscripcion repositorioSuscripcion;

  @Autowired
  public ServicioPagoImpl(
    RepositorioUsuario repositorioUsuario,
    RepositorioPago repositorioPago,
    RepositorioSuscripcion repositorioSuscripcion
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.repositorioPago = repositorioPago;
    this.repositorioSuscripcion = repositorioSuscripcion;
  }

  @Override
  public Suscripcion pagar(
    Long usuarioId,
    String titular,
    String numero,
    String vencimiento,
    String cvv
  ) {
    Usuario usuario = repositorioUsuario.buscarPorId(usuarioId);
    if (usuario == null || !esPostulante(usuario.getRol())) {
      throw new IllegalArgumentException("Iniciá sesión como postulante.");
    }
    validar(numero, vencimiento, cvv);

    Pago pago = crearPago(usuario, numero);
    if (NUMERO_RECHAZADO.equals(numero)) {
      pago.setEstado(EstadoPago.RECHAZADO);
      repositorioPago.guardar(pago);
      throw new IllegalArgumentException("El pago fue rechazado.");
    }

    pago.setEstado(EstadoPago.APROBADO);
    repositorioPago.guardar(pago);
    return activarSuscripcion(usuario, usuarioId);
  }

  private Pago crearPago(Usuario usuario, String numero) {
    Pago pago = new Pago();
    pago.setUsuario(usuario);
    pago.setMonto(MONTO);
    pago.setMoneda(MONEDA);
    pago.setFecha(LocalDateTime.now());
    pago.setUltimosCuatro(numero.substring(12));
    return pago;
  }

  private Suscripcion activarSuscripcion(Usuario usuario, Long usuarioId) {
    LocalDateTime ahora = LocalDateTime.now();
    Suscripcion suscripcion = repositorioSuscripcion.buscarPorUsuarioId(usuarioId);
    LocalDateTime inicio = obtenerInicio(suscripcion, ahora);
    if (suscripcion == null) {
      suscripcion = new Suscripcion();
      suscripcion.setUsuario(usuario);
    }
    suscripcion.setFechaInicio(inicio);
    suscripcion.setFechaFin(inicio.plusDays(DIAS));
    return repositorioSuscripcion.guardar(suscripcion);
  }

  private LocalDateTime obtenerInicio(Suscripcion suscripcion, LocalDateTime ahora) {
    if (
      suscripcion != null &&
      suscripcion.getFechaFin() != null &&
      suscripcion.getFechaFin().isAfter(ahora)
    ) {
      return suscripcion.getFechaFin();
    }
    return ahora;
  }

  @Override
  public List<Pago> listarPagos(Long usuarioId) {
    return repositorioPago.buscarPorUsuarioId(usuarioId);
  }

  @Override
  public LocalDateTime fechaFinPremium(Long usuarioId) {
    Suscripcion suscripcion = repositorioSuscripcion.buscarPorUsuarioId(usuarioId);
    if (
      suscripcion == null ||
      suscripcion.getFechaFin() == null ||
      !suscripcion.getFechaFin().isAfter(LocalDateTime.now())
    ) {
      return null;
    }
    return suscripcion.getFechaFin();
  }

  private void validar(String numero, String vencimiento, String cvv) {
    validarNumero(numero);
    validarCvv(cvv);
    validarVencimiento(vencimiento);
  }

  private void validarNumero(String numero) {
    if (numero == null || !NUMERO_VALIDO.matcher(numero).matches()) {
      throw new IllegalArgumentException("El número debe tener 16 dígitos.");
    }
  }

  private void validarCvv(String cvv) {
    if (cvv == null || !CVV_VALIDO.matcher(cvv).matches()) {
      throw new IllegalArgumentException("El CVV debe tener 3 dígitos.");
    }
  }

  private void validarVencimiento(String vencimiento) {
    YearMonth fecha;
    try {
      fecha = YearMonth.parse(vencimiento, FORMATO_VENCIMIENTO);
    } catch (DateTimeParseException excepcion) {
      throw new IllegalArgumentException("El vencimiento debe tener formato MM/AA.", excepcion);
    }
    if (!fecha.atEndOfMonth().atTime(23, 59, 59).isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("La tarjeta está vencida.");
    }
  }

  private boolean esPostulante(String rol) {
    return "CANDIDATO".equalsIgnoreCase(rol) || "POSTULANTE".equalsIgnoreCase(rol);
  }
}
