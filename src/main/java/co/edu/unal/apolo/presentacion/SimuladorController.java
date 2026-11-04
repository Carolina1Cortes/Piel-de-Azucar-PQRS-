package co.edu.unal.apolo.presentacion;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.servicios.EntradaSimuladaService;
import co.edu.unal.apolo.servicios.ReglaNegocioException;

/** Simula la llegada de un correo de una clienta (no requiere internet) y lleva al caso recién creado. */
@Controller
@RequestMapping("/simulador")
public class SimuladorController {

    private final EntradaSimuladaService entradaSimuladaService;

    public SimuladorController(EntradaSimuladaService entradaSimuladaService) {
        this.entradaSimuladaService = entradaSimuladaService;
    }

    @GetMapping
    public String formulario(Principal principal, Model model) {
        model.addAttribute("formulario", new CorreoSimuladoFormulario());
        cargarVista(principal, model);
        return "simulador";
    }

    @PostMapping
    public String recibir(@Valid @ModelAttribute("formulario") CorreoSimuladoFormulario formulario,
                          BindingResult errores,
                          Principal principal,
                          Model model,
                          RedirectAttributes redireccion) {
        if (!errores.hasErrors()) {
            try {
                Pqrs pqrs = entradaSimuladaService.registrarCorreoEntrante(formulario.getNombre(),
                        formulario.getCorreo(), formulario.getAsunto(), formulario.getCuerpo(),
                        formulario.getTipoConsultaId());
                redireccion.addFlashAttribute("mensajeOk", "Correo simulado recibido: caso #" + pqrs.getId() + ".");
                return "redirect:/pqrs/" + pqrs.getId();
            } catch (ReglaNegocioException e) {
                errores.reject("simulador.regla", e.getMessage());
            }
        }
        cargarVista(principal, model);
        return "simulador";
    }

    private void cargarVista(Principal principal, Model model) {
        model.addAttribute("tipos", entradaSimuladaService.listarTiposConsulta());
        model.addAttribute("usuario", principal.getName());
    }
}
