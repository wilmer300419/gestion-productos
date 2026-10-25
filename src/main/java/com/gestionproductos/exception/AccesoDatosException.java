package com.gestionproductos.exception;

/** Error técnico de acceso a datos. El mensaje es seguro para mostrar al usuario. */
public class AccesoDatosException extends RuntimeException {
    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
