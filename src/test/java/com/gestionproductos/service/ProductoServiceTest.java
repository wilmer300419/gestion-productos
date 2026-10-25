package com.gestionproductos.service;

import com.gestionproductos.config.ConexionFactory;
import com.gestionproductos.dao.ProductoDAO;
import com.gestionproductos.exception.CodigoDuplicadoException;
import com.gestionproductos.exception.ProductoNoEncontradoException;
import com.gestionproductos.exception.ValidacionException;
import com.gestionproductos.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de las reglas de negocio con SQLite temporal. */
class ProductoServiceTest {

    @TempDir
    Path tmp;

    ProductoService servicio;

    @BeforeEach
    void preparar() {
        ConexionFactory fabrica = new ConexionFactory(tmp.resolve("productos.db").toString());
        fabrica.inicializarEsquema();
        servicio = new ProductoService(new ProductoDAO(fabrica));
    }

    private Producto valido() {
        return new Producto("p1", "  Arroz  ", "Blanco", new BigDecimal("2500"), 10);
    }

    @Test
    void registraNormalizandoCodigoYNombre() {
        servicio.crear(valido());
        Producto leido = servicio.obtener("P1");
        assertEquals("P1", leido.getCodigo());
        assertEquals("Arroz", leido.getNombre());
        assertEquals(new BigDecimal("2500.00"), leido.getPrecio());
    }

    @Test
    void rechazaCodigosDuplicadosSinDistinguirMayusculas() {
        servicio.crear(valido());
        Producto otro = valido();
        otro.setCodigo("P1");
        assertThrows(CodigoDuplicadoException.class, () -> servicio.crear(otro));
    }

    @Test
    void rechazaPreciosNegativos() {
        Producto p = valido();
        p.setPrecio(new BigDecimal("-0.01"));
        assertThrows(ValidacionException.class, () -> servicio.crear(p));
        assertTrue(servicio.listar(null).isEmpty());
    }

    @Test
    void rechazaCantidadesNegativas() {
        Producto p = valido();
        p.setCantidad(-1);
        assertThrows(ValidacionException.class, () -> servicio.crear(p));
    }

    @Test
    void rechazaCamposObligatoriosVacios() {
        Producto p = new Producto("  ", "   ", null, null, null);
        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.crear(p));
        assertEquals(4, e.getErrores().size());
    }

    @Test
    void rechazaCodigosConCaracteresRaros() {
        Producto p = valido();
        p.setCodigo("A'; DROP TABLE productos;--");
        assertThrows(ValidacionException.class, () -> servicio.crear(p));
    }

    @Test
    void listaYBuscaProductos() {
        servicio.crear(valido());
        servicio.crear(new Producto("L1", "Leche", "", new BigDecimal("4000"), 3));
        assertEquals(2, servicio.listar("").size());
        assertEquals(1, servicio.listar("leche").size());
        assertEquals(1, servicio.listar("p1").size());
    }

    @Test
    void actualizaUnProducto() {
        servicio.crear(valido());
        Producto cambios = new Producto("P1", "Arroz Premium", "Nuevo", new BigDecimal("3000"), 4);
        servicio.actualizar(cambios);
        assertEquals("Arroz Premium", servicio.obtener("P1").getNombre());
    }

    @Test
    void actualizarUnProductoInexistenteLanzaExcepcion() {
        assertThrows(ProductoNoEncontradoException.class, () -> servicio.actualizar(valido()));
    }

    @Test
    void consultarUnProductoInexistenteLanzaExcepcion() {
        assertThrows(ProductoNoEncontradoException.class, () -> servicio.obtener("NOPE"));
    }

    @Test
    void eliminaUnProductoYLuegoYaNoExiste() {
        servicio.crear(valido());
        servicio.eliminar("p1");
        assertThrows(ProductoNoEncontradoException.class, () -> servicio.eliminar("P1"));
    }

    @Test
    void eliminarConUnCodigoInvalidoNoTocaNingunProducto() {
        servicio.crear(valido());
        assertThrows(ValidacionException.class, () -> servicio.eliminar("P1' OR '1'='1"));
        assertThrows(ValidacionException.class, () -> servicio.eliminar("  "));
        assertEquals(1, servicio.listar(null).size());
    }

    @Test
    void calculaEstadisticas() {
        servicio.crear(valido());
        assertEquals(1, servicio.estadisticas().totalProductos());
        assertEquals(new BigDecimal("25000.00"), servicio.estadisticas().valorInventario());
    }
}
