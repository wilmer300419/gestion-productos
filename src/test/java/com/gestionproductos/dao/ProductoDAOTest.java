package com.gestionproductos.dao;

import com.gestionproductos.config.ConexionFactory;
import com.gestionproductos.exception.AccesoDatosException;
import com.gestionproductos.exception.CodigoDuplicadoException;
import com.gestionproductos.model.EstadisticasInventario;
import com.gestionproductos.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del DAO contra un archivo SQLite temporal (nunca toca la base real). */
class ProductoDAOTest {

    @TempDir
    Path tmp;

    ConexionFactory fabrica;
    ProductoDAO dao;

    @BeforeEach
    void preparar() {
        fabrica = new ConexionFactory(tmp.resolve("sub/directorio/productos.db").toString());
        fabrica.inicializarEsquema();
        dao = new ProductoDAO(fabrica);
    }

    private Producto producto(String codigo, String nombre, String precio, int cantidad) {
        return new Producto(codigo, nombre, "Descripción de " + nombre, new BigDecimal(precio), cantidad);
    }

    @Test
    void creaElDirectorioYLaTablaAutomaticamente() throws Exception {
        assertTrue(Files.exists(tmp.resolve("sub/directorio/productos.db")));
        try (Connection c = fabrica.obtener();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='productos'")) {
            assertTrue(rs.next());
        }
    }

    @Test
    void inicializarDosVecesNoBorraDatos() {
        dao.crear(producto("A1", "Arroz", "2500", 10));
        fabrica.inicializarEsquema();
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void registraYConsultaUnProducto() {
        dao.crear(producto("A1", "Arroz", "2500.50", 10));
        Producto leido = dao.buscarPorCodigo("A1").orElseThrow();
        assertEquals("Arroz", leido.getNombre());
        assertEquals(new BigDecimal("2500.50"), leido.getPrecio());
        assertEquals(10, leido.getCantidad());
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void rechazaCodigosDuplicados() {
        dao.crear(producto("A1", "Arroz", "2500", 10));
        assertThrows(CodigoDuplicadoException.class, () -> dao.crear(producto("A1", "Otro", "1", 1)));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void buscaPorCodigoYPorNombreSinDistinguirMayusculas() {
        dao.crear(producto("ARR-01", "Arroz Blanco", "2500", 10));
        dao.crear(producto("LEC-01", "Leche entera", "4000", 5));

        assertEquals(1, dao.buscarPorNombre("ARROZ").size());
        assertEquals(1, dao.buscarPorNombre("leche").size());
        assertEquals(1, dao.buscarPorCodigoONombre("arr-0").size());
        assertEquals(2, dao.buscarPorCodigoONombre("-01").size());
        assertTrue(dao.buscarPorCodigoONombre("zzz").isEmpty());
        assertTrue(dao.buscarPorCodigo("NO-EXISTE").isEmpty());
    }

    @Test
    void losComodinesLikeSeTratanComoTextoLiteral() {
        dao.crear(producto("A1", "Arroz", "2500", 10));
        assertTrue(dao.buscarPorNombre("%").isEmpty());
        assertTrue(dao.buscarPorNombre("_").isEmpty());
    }

    @Test
    void resisteUnIntentoDeInyeccionSql() {
        dao.crear(producto("A1", "Arroz", "2500", 10));
        List<Producto> resultado = dao.buscarPorCodigoONombre("' OR '1'='1");
        assertTrue(resultado.isEmpty());
        assertFalse(dao.eliminarPorCodigo("A1' OR '1'='1"));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void actualizaUnProductoExistente() {
        dao.crear(producto("A1", "Arroz", "2500", 10));
        boolean actualizado = dao.actualizar(producto("A1", "Arroz Premium", "3000", 7));
        assertTrue(actualizado);
        Producto leido = dao.buscarPorCodigo("A1").orElseThrow();
        assertEquals("Arroz Premium", leido.getNombre());
        assertEquals(7, leido.getCantidad());
    }

    @Test
    void actualizarUnProductoInexistenteDevuelveFalse() {
        assertFalse(dao.actualizar(producto("NOPE", "X", "1", 1)));
    }

    @Test
    void eliminaUnProducto() {
        dao.crear(producto("A1", "Arroz", "2500", 10));
        assertTrue(dao.eliminarPorCodigo("A1"));
        assertTrue(dao.listarTodos().isEmpty());
        assertFalse(dao.eliminarPorCodigo("A1"));
    }

    @Test
    void laBaseDeDatosRechazaPreciosYCantidadesNegativas() {
        assertThrows(AccesoDatosException.class, () -> dao.crear(producto("A1", "Arroz", "-1", 10)));
        assertThrows(AccesoDatosException.class, () -> dao.crear(producto("A2", "Arroz", "1", -5)));
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void laTransaccionRevierteTodoSiUnaInsercionFalla() {
        List<Producto> lote = List.of(
                producto("A1", "Uno", "1", 1),
                producto("A1", "Repetido", "1", 1));
        assertThrows(CodigoDuplicadoException.class, () -> dao.crearVarios(lote));
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void calculaLasEstadisticasDelInventario() {
        dao.crear(producto("A1", "Arroz", "2500.50", 10));
        dao.crear(producto("A2", "Leche", "4000", 5));
        EstadisticasInventario e = dao.obtenerEstadisticas();
        assertEquals(2, e.totalProductos());
        assertEquals(15, e.unidadesDisponibles());
        assertEquals(new BigDecimal("45005.00"), e.valorInventario());
    }

    @Test
    void estadisticasDeUnaBaseVaciaSonCero() {
        EstadisticasInventario e = dao.obtenerEstadisticas();
        assertEquals(0, e.totalProductos());
        assertEquals(0, e.unidadesDisponibles());
        assertEquals(new BigDecimal("0.00"), e.valorInventario());
    }

    @Test
    void losDatosPersistenAlCerrarYVolverAAbrirLasConexiones() {
        dao.crear(producto("A1", "Arroz", "2500", 10));

        // Simula "cerrar y reiniciar la aplicación": objetos nuevos sobre el mismo archivo.
        ConexionFactory fabrica2 = new ConexionFactory(tmp.resolve("sub/directorio/productos.db").toString());
        fabrica2.inicializarEsquema();
        ProductoDAO dao2 = new ProductoDAO(fabrica2);

        assertEquals(1, dao2.listarTodos().size());
        assertEquals("Arroz", dao2.buscarPorCodigo("A1").orElseThrow().getNombre());
    }
}
