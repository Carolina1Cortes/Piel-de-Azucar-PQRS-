package co.edu.unal.apolo.presentacion;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import co.edu.unal.apolo.servicios.PqrsService;
import co.edu.unal.apolo.servicios.ReglaNegocioException;

/** Pantalla de un caso: ver la conversación, tomar el caso y responder. Las reglas viven en PqrsService. */
@Controller
@RequestMapping("/pqrs")
public class CasoController {

    private final PqrsService pqrsService;

    public CasoController(PqrsService pqrsService) {
        this.pqrsService = pqrsService;
    }

    @GetMapping("/{id}")
    public String ver(@PathVariable Long id, Principal principal, Model model) {
        cargarCaso(id, principal, model);
        if (!model.containsAttribute("formulario")) {
            model.addAttribute("formulario", new RespuestaFormulario());
        }
        return "caso";
    }

    @PostMapping("/{id}/tomar")
    public String tomar(@PathVariable Long id, Principal principal, RedirectAttributes redireccion) {
        try {
            pqrsService.tomarCaso(id, principal.getName());
            redireccion.addFlashAttribute("mensajeOk", "Caso tomado. Ya puedes responder.");
        } catch (ReglaNegocioException e) {
            redireccion.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/pqrs/" + id;
    }

    @PostMapping("/{id}/responder")
    public String responder(@PathVariable Long id,
                            @Valid @ModelAttribute("formulario") RespuestaFormulario formulario,
                            BindingResult errores,
                            Principal principal,
                            Model model,
                            RedirectAttributes redireccion) {
        if (errores.hasErrors()) {
            cargarCaso(id, principal, model);
            return "caso";
        }
        try {
            pqrsService.enviarRespuesta(id, principal.getName(), formulario.getAsunto(), formulario.getCuerpo());
            redireccion.addFlashAttribute("mensajeOk", "Respuesta enviada por el canal simulado.");
        } catch (ReglaNegocioException e) {
            redireccion.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/pqrs/" + id;
    }

    private void cargarCaso(Long id, Principal principal, Model model) {
        model.addAttribute("caso", pqrsService.detalle(id, principal.getName()));
        model.addAttribute("usuario", principal.getName());
    }
}
