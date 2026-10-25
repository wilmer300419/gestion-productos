package com.gestionproductos.dao;

import com.gestionproductos.config.ConexionFactory;
import com.gestionproductos.exception.AccesoDatosException;
import com.gestionproductos.exception.CodigoDuplicadoException;
import com.gestionproductos.model.EstadisticasInventario;
import com.gestionproductos.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Acceso a datos con JDBC puro. Todas las consultas usan PreparedStatement con parámetros (?)
 * y cada conexión/sentencia/resultado se cierra con try-with-resources.
 */
@Repository
public class ProductoDAO {

    private static final Logger log = LoggerFactory.getLogger(ProductoDAO.class);

    private static final String SQL_INSERTAR =
            "INSERT INTO productos (codigo, nombre, descripcion, precio, cantidad) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_LISTAR =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos ORDER BY nombre COLLATE NOCASE, codigo";
    private static final String SQL_POR_CODIGO =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos WHERE codigo = ?";
    private static final String SQL_POR_NOMBRE =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos "
                    + "WHERE LOWER(nombre) LIKE ? ESCAPE '\\' ORDER BY nombre COLLATE NOCASE, codigo";
    private static final String SQL_POR_CODIGO_O_NOMBRE =
            "SELECT codigo, nombre, descripcion, precio, cantidad FROM productos "
                    + "WHERE LOWER(codigo) LIKE ? ESCAPE '\\' OR LOWER(nombre) LIKE ? ESCAPE '\\' "
                    + "ORDER BY nombre COLLATE NOCASE, codigo";
    private static final String SQL_ACTUALIZAR =
            "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, cantidad = ? WHERE codigo = ?";
    private static final String SQL_ELIMINAR = "DELETE FROM productos WHERE codigo = ?";
    private static final String SQL_ESTADISTICAS =
            "SELECT COUNT(*), COALESCE(SUM(cantidad), 0), COALESCE(SUM(precio * cantidad), 0) FROM productos";

    private final ConexionFactory conexiones;

    public ProductoDAO(ConexionFactory conexiones) {
        this.conexiones = conexiones;
    }

    /** Inserta un producto. Lanza CodigoDuplicadoException si el código ya existe. */
    public void crear(Producto producto) {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_INSERTAR)) {
            asignarParametrosInsercion(ps, producto);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw traducir(e, producto.getCodigo(), "No se pudo guardar el producto.");
        }
    }

    /** Inserta varios productos en UNA transacción: o se guardan todos o ninguno. */
    public void crearVarios(List<Producto> productos) {
        String codigoActual = null; // para informar qué código provocó el error
        try (Connection c = conexiones.obtener()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(SQL_INSERTAR)) {
                for (Producto p : productos) {
                    codigoActual = p.getCodigo();
                    asignarParametrosInsercion(ps, p);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw traducir(e, codigoActual, "No se pudieron guardar los productos.");
        }
    }

    public List<Producto> listarTodos() {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return leer(rs);
        } catch (SQLException e) {
            throw traducir(e, null, "No se pudieron consultar los productos.");
        }
    }

    public Optional<Producto> buscarPorCodigo(String codigo) {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_POR_CODIGO)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw traducir(e, codigo, "No se pudo consultar el producto.");
        }
    }

    /** Búsqueda parcial por nombre, sin distinguir mayúsculas (SQLite solo lo garantiza para ASCII). */
    public List<Producto> buscarPorNombre(String nombre) {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_POR_NOMBRE)) {
            ps.setString(1, patronLike(nombre));
            try (ResultSet rs = ps.executeQuery()) {
                return leer(rs);
            }
        } catch (SQLException e) {
            throw traducir(e, null, "No se pudo realizar la búsqueda.");
        }
    }

    /** Búsqueda parcial por código O nombre (la usa el buscador de la interfaz). */
    public List<Producto> buscarPorCodigoONombre(String texto) {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_POR_CODIGO_O_NOMBRE)) {
            String patron = patronLike(texto);
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return leer(rs);
            }
        } catch (SQLException e) {
            throw traducir(e, null, "No se pudo realizar la búsqueda.");
        }
    }

    /** @return true si se actualizó una fila; false si el código no existe. */
    public boolean actualizar(Producto producto) {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, producto.getNombre());
            ps.setString(2, producto.getDescripcion());
            ps.setDouble(3, producto.getPrecio().doubleValue());
            ps.setInt(4, producto.getCantidad());
            ps.setString(5, producto.getCodigo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducir(e, producto.getCodigo(), "No se pudo actualizar el producto.");
        }
    }

    /** @return true si se eliminó una fila; false si el código no existe. */
    public boolean eliminarPorCodigo(String codigo) {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_ELIMINAR)) {
            ps.setString(1, codigo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw traducir(e, codigo, "No se pudo eliminar el producto.");
        }
    }

    public EstadisticasInventario obtenerEstadisticas() {
        try (Connection c = conexiones.obtener();
             PreparedStatement ps = c.prepareStatement(SQL_ESTADISTICAS);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new EstadisticasInventario(
                    rs.getLong(1),
                    rs.getLong(2),
                    BigDecimal.valueOf(rs.getDouble(3)).setScale(2, RoundingMode.HALF_UP));
        } catch (SQLException e) {
            throw traducir(e, null, "No se pudieron calcular las estadísticas.");
        }
    }

    // ------------------------------------------------------------------ utilidades

    private void asignarParametrosInsercion(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.getCodigo());
        ps.setString(2, p.getNombre());
        ps.setString(3, p.getDescripcion() == null ? "" : p.getDescripcion());
        ps.setDouble(4, p.getPrecio().doubleValue());
        ps.setInt(5, p.getCantidad());
    }

    private List<Producto> leer(ResultSet rs) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                BigDecimal.valueOf(rs.getDouble("precio")).setScale(2, RoundingMode.HALF_UP),
                rs.getInt("cantidad"));
    }

    /** Escapa % _ \ para que el texto del usuario se trate como texto literal dentro de LIKE. */
    private String patronLike(String texto) {
        String limpio = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        String escapado = limpio.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    /** Convierte SQLException en excepciones propias con mensajes seguros para el usuario. */
    private RuntimeException traducir(SQLException e, String codigo, String mensajeGenerico) {
        String detalle = e.getMessage() == null ? "" : e.getMessage();
        if (codigo != null && detalle.contains("UNIQUE constraint failed")) {
            return new CodigoDuplicadoException(codigo);
        }
        log.error("{} Detalle técnico: {}", mensajeGenerico, detalle);
        return new AccesoDatosException(mensajeGenerico, e);
    }
}
