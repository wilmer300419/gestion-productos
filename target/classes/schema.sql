-- Estructura de la base de datos SQLite (productos.db).
-- La aplicación ejecuta este script al iniciar. IF NOT EXISTS evita
-- recrear la tabla o borrar datos existentes.
CREATE TABLE IF NOT EXISTS productos (
    codigo      TEXT    PRIMARY KEY NOT NULL CHECK (length(trim(codigo)) > 0),
    nombre      TEXT    NOT NULL CHECK (length(trim(nombre)) > 0),
    descripcion TEXT    NOT NULL DEFAULT '',
    precio      REAL    NOT NULL CHECK (precio >= 0),
    cantidad    INTEGER NOT NULL CHECK (cantidad >= 0)
);
