package com.gestionproductos.controller;

import com.gestionproductos.exception.CodigoDuplicadoException;
import com.gestionproductos.exception.ProductoNoEncontradoException;
import com.gestionproductos.exception.ValidacionException;
import com.gestionproductos.model.Producto;
import com.gestionproductos.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador web. Recibe peticiones HTTP, valida los formularios, delega en el servicio
 * y elige la vista. No contiene SQL ni reglas de negocio.
 * Patrón Post/Redirect/Get: tras una operación exitosa se redirige para evitar reenvíos del formulario.
 */
@Controller
public class ProductoController {

    private final ProductoService servicio;
    private final int umbralInventarioBajo;

    public ProductoController(ProductoService servicio,
                              @Value("${app.inventario-bajo:5}") int umbralInventarioBajo) {
        this.servicio = servicio;
        this.umbralInventarioBajo = umbralInventarioBajo;
    }

    /** Recorta espacios y convierte textos vacíos en null (así @NotBlank funciona bien). */
    @InitBinder
    public void configurarBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/productos";
    }

    // ------------------------------------------------------------ consultar / buscar

    @GetMapping("/productos")
    public String listar(@RequestParam(name = "q", required = false) String q, Model model) {
        if (!model.containsAttribute("producto")) {
            model.addAttribute("producto", new Producto());
        }
        cargarPanel(model, q);
        return "productos";
    }

    // ------------------------------------------------------------ registrar

    @PostMapping("/productos")
    public String crear(@Valid @ModelAttribute("producto") Producto producto,
                        BindingResult resultado, Model model, RedirectAttributes flash) {
        if (!resultado.hasErrors()) {
            try {
                Producto guardado = servicio.crear(producto);
                flash.addFlashAttribute("exito",
                        "Producto «" + guardado.getNombre() + "» registrado correctamente.");
                return "redirect:/productos";
            } catch (CodigoDuplicadoException e) {
                resultado.rejectValue("codigo", "duplicado", e.getMessage());
            } catch (ValidacionException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        cargarPanel(model, null);
        return "productos";
    }

    // ------------------------------------------------------------ actualizar

    @GetMapping("/productos/{codigo}/editar")
    public String editar(@PathVariable String codigo, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("producto", servicio.obtener(codigo));
            model.addAttribute("codigoOriginal", codigo);
            return "producto-form";
        } catch (ProductoNoEncontradoException | ValidacionException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos";
        }
    }

    @PostMapping("/productos/{codigo}/editar")
    public String actualizar(@PathVariable String codigo,
                             @Valid @ModelAttribute("producto") Producto producto,
                             BindingResult resultado, Model model, RedirectAttributes flash) {
        model.addAttribute("codigoOriginal", codigo);
        if (resultado.hasErrors()) {
            return "producto-form";
        }
        producto.setCodigo(codigo); // el código identifica el registro y se toma de la URL
        try {
            Producto actualizado = servicio.actualizar(producto);
            flash.addFlashAttribute("exito",
                    "Producto «" + actualizado.getNombre() + "» actualizado correctamente.");
            return "redirect:/productos";
        } catch (ProductoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos";
        } catch (ValidacionException e) {
            model.addAttribute("error", e.getMessage());
            return "producto-form";
        }
    }

    // ------------------------------------------------------------ eliminar

    @PostMapping("/productos/{codigo}/eliminar")
    public String eliminar(@PathVariable String codigo, RedirectAttributes flash) {
        try {
            servicio.eliminar(codigo);
            flash.addFlashAttribute("exito", "Producto eliminado correctamente.");
        } catch (ProductoNoEncontradoException | ValidacionException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }

    // ------------------------------------------------------------ utilidades

    private void cargarPanel(Model model, String busqueda) {
        model.addAttribute("productos", servicio.listar(busqueda));
        model.addAttribute("stats", servicio.estadisticas());
        model.addAttribute("q", busqueda == null ? "" : busqueda);
        model.addAttribute("umbralBajo", umbralInventarioBajo);
    }
}
