package com.gestionproductos.model;

import java.math.BigDecimal;

/**
 * Resumen del inventario. El valor total es una ESTIMACIÓN (suma de precio × cantidad),
 * no una valoración contable.
 */
public record EstadisticasInventario(long totalProductos, long unidadesDisponibles, BigDecimal valorInventario) {
}
