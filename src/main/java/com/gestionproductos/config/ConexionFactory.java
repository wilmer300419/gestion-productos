package com.gestionproductos.config;

import com.gestionproductos.exception.AccesoDatosException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Crea conexiones JDBC hacia el archivo SQLite y prepara el esquema.
 * Es una clase Java normal (sin dependencias de Spring) para poder probarla de forma aislada.
 */
public class ConexionFactory {

    private final Path rutaDb;
    private boolean baseNueva;

    public ConexionFactory(String ruta) {
        this.rutaDb = Path.of(ruta).toAbsolutePath();
    }

    public Path getRuta() {
        return rutaDb;
    }

    /** true si el archivo de base de datos no existía antes de inicializar el esquema. */
    public boolean isBaseNueva() {
        return baseNueva;
    }

    /** Abre una conexión NUEVA. Quien la llama debe cerrarla (try-with-resources). */
    public Connection obtener() throws SQLException {
        crearDirectorioSiHaceFalta();
        Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + rutaDb);
        try (Statement st = conexion.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            conexion.close();
            throw e;
        }
        return conexion;
    }

    /**
     * Ejecuta schema.sql. Usa CREATE TABLE IF NOT EXISTS, por lo que nunca borra ni
     * sobrescribe datos existentes.
     */
    public void inicializarEsquema() {
        baseNueva = !Files.exists(rutaDb);
        try (Connection conexion = obtener()) {
            conexion.setAutoCommit(false);
            try (Statement st = conexion.createStatement()) {
                for (String sentencia : leerSentencias()) {
                    st.execute(sentencia);
                }
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("No se pudo preparar la base de datos.", e);
        }
    }

    private String[] leerSentencias() {
        try (InputStream in = ConexionFactory.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new AccesoDatosException("No se encontró schema.sql.", null);
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder limpio = new StringBuilder();
            for (String linea : sql.split("\\R")) {
                if (!linea.trim().startsWith("--")) {
                    limpio.append(linea).append('\n');
                }
            }
            return java.util.Arrays.stream(limpio.toString().split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } catch (IOException e) {
            throw new AccesoDatosException("No se pudo leer schema.sql.", e);
        }
    }

    private void crearDirectorioSiHaceFalta() throws SQLException {
        Path padre = rutaDb.getParent();
        if (padre != null) {
            try {
                Files.createDirectories(padre);
            } catch (IOException e) {
                throw new SQLException("No se pudo crear el directorio de la base de datos: " + padre, e);
            }
        }
    }
}
