package com.notiurba.admin;

public enum FiltroVigencia {
    VIGENTES("vigentes"), FUERA_DE_PLAZO("fuera_de_plazo"), TODOS("todos");

    private final String valor;

    FiltroVigencia(String valor) { this.valor = valor; }

    public String valor() { return valor; }
}
