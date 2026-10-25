package com.gestionproductos.controller;

import com.gestionproductos.exception.AccesoDatosException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Muestra una página de error amable (sin trazas internas) cuando falla el acceso a la base de datos. */
@ControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(AccesoDatosException.class)
    public String errorDeDatos(AccesoDatosException e, Model model, HttpServletResponse respuesta) {
        respuesta.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }
}
