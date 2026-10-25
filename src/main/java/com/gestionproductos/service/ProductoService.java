package com.gestionproductos.service;

import com.gestionproductos.dao.ProductoDAO;
import com.gestionproductos.exception.ProductoNoEncontradoException;
import com.gestionproductos.exception.ValidacionException;
import com.gestionproductos.model.EstadisticasInventario;
import com.gestionproductos.model.Producto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Reglas de negocio: normaliza y valida los datos y coordina las operaciones del DAO. */
@Service
public class ProductoService {

    private static final Pattern CODIGO_VALIDO = Pattern.compile(Producto.PATRON_CODIGO);

    private final ProductoDAO dao;

    public ProductoService(ProductoDAO dao) {
        this.dao = dao;
    }

    public Producto crear(Producto entrada) {
        Producto producto = normalizar(entrada);
        validar(producto);
        dao.crear(producto); // lanza CodigoDuplicadoException si el código ya existe
        return producto;
    }

    public List<Producto> listar(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return dao.listarTodos();
        }
        return dao.buscarPorCodigoONombre(busqueda);
    }

    public Producto obtener(String codigo) {
        String normalizado = normalizarCodigo(codigo);
        exigirCodigoValido(normalizado);
        return dao.buscarPorCodigo(normalizado)
                .orElseThrow(() -> new ProductoNoEncontradoException(normalizado));
    }

    public Producto actualizar(Producto entrada) {
        Producto producto = normalizar(entrada);
        validar(producto);
        if (!dao.actualizar(producto)) {
            throw new ProductoNoEncontradoException(producto.getCodigo());
        }
        return producto;
    }

    public void eliminar(String codigo) {
        String normalizado = normalizarCodigo(codigo);
        exigirCodigoValido(normalizado);
        if (!dao.eliminarPorCodigo(normalizado)) {
            throw new ProductoNoEncontradoException(normalizado);
        }
    }

    public EstadisticasInventario estadisticas() {
        return dao.obtenerEstadisticas();
    }

    // ------------------------------------------------------------------ validación

    private void exigirCodigoValido(String codigo) {
        if (codigo == null || !CODIGO_VALIDO.matcher(codigo).matches()) {
            throw new ValidacionException(List.of("El código del producto no es válido."));
        }
    }

    void validar(Producto p) {
        List<String> errores = new ArrayList<>();
        if (p.getCodigo() == null || p.getCodigo().isBlank()) {
            errores.add("El código es obligatorio.");
        } else if (!CODIGO_VALIDO.matcher(p.getCodigo()).matches()) {
            errores.add("El código solo admite letras, números, guion y guion bajo (máx. 30).");
        }
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            errores.add("El nombre es obligatorio.");
        } else if (p.getNombre().length() > 120) {
            errores.add("El nombre admite máximo 120 caracteres.");
        }
        if (p.getDescripcion() != null && p.getDescripcion().length() > 500) {
            errores.add("La descripción admite máximo 500 caracteres.");
        }
        if (p.getPrecio() == null) {
            errores.add("El precio es obligatorio.");
        } else if (p.getPrecio().signum() < 0) {
            errores.add("El precio no puede ser negativo.");
        }
        if (p.getCantidad() == null) {
            errores.add("La cantidad es obligatoria.");
        } else if (p.getCantidad() < 0) {
            errores.add("La cantidad no puede ser negativa.");
        }
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
    }

    private Producto normalizar(Producto p) {
        if (p == null) {
            throw new ValidacionException(List.of("No se recibieron datos del producto."));
        }
        BigDecimal precio = p.getPrecio() == null ? null : p.getPrecio().setScale(2, RoundingMode.HALF_UP);
        return new Producto(
                normalizarCodigo(p.getCodigo()),
                p.getNombre() == null ? null : p.getNombre().trim(),
                p.getDescripcion() == null ? "" : p.getDescripcion().trim(),
                precio,
                p.getCantidad());
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }
}
