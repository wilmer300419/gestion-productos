package com.gestionproductos.config;

import com.gestionproductos.dao.ProductoDAO;
import com.gestionproductos.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/** Configura SQLite: ruta del archivo, creación del esquema y datos de ejemplo iniciales. */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    public ConexionFactory conexionFactory(@Value("${app.db.path}") String ruta) {
        ConexionFactory fabrica = new ConexionFactory(ruta);
        fabrica.inicializarEsquema(); // crea la tabla si no existe; no toca datos existentes
        log.info("Base de datos SQLite en: {}", fabrica.getRuta());
        return fabrica;
    }

    /** Inserta datos de ejemplo únicamente cuando el archivo .db se acaba de crear. */
    @Bean
    public ApplicationRunner datosDeEjemplo(ConexionFactory fabrica, ProductoDAO dao,
                                            @Value("${app.seed:true}") boolean sembrar) {
        return args -> {
            if (sembrar && fabrica.isBaseNueva()) {
                dao.crearVarios(List.of(
                        new Producto("P001", "Arroz 500 g", "Arroz blanco de grano largo", new BigDecimal("2800.00"), 40),
                        new Producto("P002", "Aceite de girasol 1 L", "Aceite vegetal para cocina", new BigDecimal("12500.00"), 4),
                        new Producto("P003", "Leche entera 1 L", "Leche larga vida", new BigDecimal("4300.00"), 0),
                        new Producto("P004", "Panela 500 g", "Panela en bloque", new BigDecimal("3200.00"), 25)));
                log.info("Se insertaron datos de ejemplo en la base nueva.");
            }
        };
    }
}
