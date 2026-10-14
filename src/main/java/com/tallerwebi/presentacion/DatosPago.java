package com.tallerwebi.presentacion;

public class DatosPago {

  private String titular;
  private String numero;
  private String vencimiento;
  private String cvv;

  public String getTitular() {
    return titular;
  }

  public void setTitular(String titular) {
    this.titular = titular;
  }

  public String getNumero() {
    return numero;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }

  public String getVencimiento() {
    return vencimiento;
  }

  public void setVencimiento(String vencimiento) {
    this.vencimiento = vencimiento;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }
}
