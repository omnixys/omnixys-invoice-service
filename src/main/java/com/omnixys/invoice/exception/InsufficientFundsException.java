package com.omnixys.invoice.exception;

public class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException() {
    super("Du hast nicht genügend Geld");
  }
}
