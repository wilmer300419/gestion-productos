package com.gestionproductos.exception;

public class ProductoNoEncontradoException extends RuntimeException {
    public ProductoNoEncontradoException(String codigo) {
        super("No existe un producto con el código «" + codigo + "».");
    }
}
