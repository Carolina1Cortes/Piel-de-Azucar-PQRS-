package co.edu.unal.apolo.presentacion;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import co.edu.unal.apolo.servicios.PqrsService;

/** Página de inicio: bandeja de solo lectura con las PQRS de la base de datos. */
@Controller
public class InicioController {

    private final PqrsService pqrsService;

    public InicioController(PqrsService pqrsService) {
        this.pqrsService = pqrsService;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("pqrs", pqrsService.listarConDetalle());
        return "inicio";
    }
}
