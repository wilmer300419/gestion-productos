package com.gestionproductos.exception;

import java.util.List;

/** Se lanza cuando los datos no cumplen las reglas de negocio. */
public class ValidacionException extends RuntimeException {
    private final List<String> errores;

    public ValidacionException(List<String> errores) {
        super(String.join(" ", errores));
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
