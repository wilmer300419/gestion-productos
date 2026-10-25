package com.gestionproductos.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Producto del inventario. Las anotaciones de Bean Validation se usan en el formulario web. */
public class Producto {

    /** Formato permitido para el código (también se usa en el servicio). */
    public static final String PATRON_CODIGO = "^[A-Za-z0-9_-]{1,30}$";

    @NotBlank(message = "El código es obligatorio.")
    @Pattern(regexp = PATRON_CODIGO,
             message = "El código solo admite letras, números, guion y guion bajo (máx. 30).")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 120, message = "El nombre admite máximo 120 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripción admite máximo 500 caracteres.")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo.")
    private BigDecimal precio;

    @NotNull(message = "La cantidad es obligatoria.")
    @Min(value = 0, message = "La cantidad no puede ser negativa.")
    private Integer cantidad;

    public Producto() {
    }

    public Producto(String codigo, String nombre, String descripcion, BigDecimal precio, Integer cantidad) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
