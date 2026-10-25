package com.gestionproductos.controller;

import com.gestionproductos.exception.ProductoNoEncontradoException;
import com.gestionproductos.model.Producto;
import com.gestionproductos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prueba de integración: Spring Boot completo + SQLite temporal + peticiones HTTP simuladas. */
@SpringBootTest(properties = "app.seed=false")
@AutoConfigureMockMvc
class ProductoControllerIntegrationTest {

    private static final Path DIRECTORIO;

    static {
        try {
            DIRECTORIO = Files.createTempDirectory("gestion-productos-it");
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.db.path", () -> DIRECTORIO.resolve("productos.db").toString());
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ProductoService servicio;

    private String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MvcResult registrar(String codigo, String nombre, String precio, String cantidad) throws Exception {
        return mvc.perform(post("/productos")
                .param("codigo", codigo).param("nombre", nombre).param("descripcion", "desc")
                .param("precio", precio).param("cantidad", cantidad)).andReturn();
    }

    @Test
    void laRaizRedirigeAlListado() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/productos"));
    }

    @Test
    void registraUnProductoYApareceEnElListado() throws Exception {
        mvc.perform(post("/productos")
                        .param("codigo", "IT1").param("nombre", "Café molido").param("descripcion", "500 g")
                        .param("precio", "15000").param("cantidad", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("exito"));

        assertEquals("Café molido", servicio.obtener("IT1").getNombre());
        String html = cuerpo(mvc.perform(get("/productos")).andReturn());
        assertTrue(html.contains("Café molido"));
        assertTrue(html.contains("Sistema de Gestión de Productos"));
    }

    @Test
    void rechazaUnCodigoDuplicadoConUnMensajeComprensible() throws Exception {
        registrar("IT2", "Azúcar", "3000", "5");
        MvcResult segundo = registrar("IT2", "Otro", "1", "1");
        assertEquals(200, segundo.getResponse().getStatus());
        assertTrue(cuerpo(segundo).contains("Ya existe un producto con el código"));
        assertEquals("Azúcar", servicio.obtener("IT2").getNombre());
    }

    @Test
    void rechazaPreciosNegativosYCamposVacios() throws Exception {
        MvcResult negativo = registrar("IT3", "Sal", "-5", "2");
        assertEquals(200, negativo.getResponse().getStatus());
        assertTrue(cuerpo(negativo).contains("El precio no puede ser negativo."));

        MvcResult vacio = registrar("IT4", "", "5", "2");
        assertTrue(cuerpo(vacio).contains("El nombre es obligatorio."));

        MvcResult cantidad = registrar("IT5", "Pan", "5", "-2");
        assertTrue(cuerpo(cantidad).contains("La cantidad no puede ser negativa."));

        assertThrows(ProductoNoEncontradoException.class, () -> servicio.obtener("IT3"));
        assertThrows(ProductoNoEncontradoException.class, () -> servicio.obtener("IT4"));
        assertThrows(ProductoNoEncontradoException.class, () -> servicio.obtener("IT5"));
    }

    @Test
    void actualizaUnProductoDesdeElFormularioDeEdicion() throws Exception {
        servicio.crear(new Producto("IT6", "Harina", "1 kg", new BigDecimal("4000"), 8));

        String formulario = cuerpo(mvc.perform(get("/productos/IT6/editar")).andReturn());
        assertTrue(formulario.contains("Harina"));

        mvc.perform(post("/productos/IT6/editar")
                        .param("codigo", "IT6").param("nombre", "Harina de trigo").param("descripcion", "2 kg")
                        .param("precio", "6500").param("cantidad", "12"))
                .andExpect(status().is3xxRedirection());

        Producto actualizado = servicio.obtener("IT6");
        assertEquals("Harina de trigo", actualizado.getNombre());
        assertEquals(12, actualizado.getCantidad());
    }

    @Test
    void editarUnProductoInexistenteInformaAlUsuario() throws Exception {
        mvc.perform(get("/productos/NO-EXISTE/editar"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void eliminaUnProductoExistenteYInformaSiNoExiste() throws Exception {
        servicio.crear(new Producto("IT7", "Fríjol", "", new BigDecimal("7000"), 2));

        mvc.perform(post("/productos/IT7/eliminar"))
                .andExpect(redirectedUrl("/productos"))
                .andExpect(flash().attributeExists("exito"));
        assertThrows(ProductoNoEncontradoException.class, () -> servicio.obtener("IT7"));

        mvc.perform(post("/productos/IT7/eliminar"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void unCodigoMalFormadoNoEliminaNada() throws Exception {
        servicio.crear(new Producto("IT8", "Lenteja", "", new BigDecimal("5000"), 2));
        mvc.perform(post("/productos/{c}/eliminar", "IT8' OR '1'='1"))
                .andExpect(flash().attributeExists("error"));
        assertEquals("Lenteja", servicio.obtener("IT8").getNombre());
    }

    @Test
    void elBuscadorFiltraPorNombreYMuestraEstadoVacio() throws Exception {
        servicio.crear(new Producto("IT9", "Chocolate", "", new BigDecimal("9000"), 2));
        String encontrado = cuerpo(mvc.perform(get("/productos").param("q", "chocol")).andReturn());
        assertTrue(encontrado.contains("Chocolate"));

        String vacio = cuerpo(mvc.perform(get("/productos").param("q", "zzz-no-existe")).andReturn());
        assertTrue(vacio.contains("No se encontraron productos"));
    }

    @Test
    void escapaElContenidoHtmlParaEvitarXss() throws Exception {
        servicio.crear(new Producto("IT10", "<script>alert(1)</script>", "", new BigDecimal("1"), 1));
        String html = cuerpo(mvc.perform(get("/productos")).andReturn());
        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }
}
