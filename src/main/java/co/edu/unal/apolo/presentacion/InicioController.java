package co.edu.unal.apolo.presentacion;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.unal.apolo.servicios.PqrsService;

/** Bandeja de PQRS con el filtro Todos / Críticos. */
@Controller
public class InicioController {

    private static final String FILTRO_TODOS = "todos";
    private static final String FILTRO_CRITICOS = "criticos";

    private final PqrsService pqrsService;

    public InicioController(PqrsService pqrsService) {
        this.pqrsService = pqrsService;
    }

    @GetMapping("/")
    public String inicio(@RequestParam(name = "filtro", defaultValue = FILTRO_TODOS) String filtro,
                         Principal principal, Model model) {
        boolean soloCriticos = FILTRO_CRITICOS.equalsIgnoreCase(filtro);
        model.addAttribute("pqrs", pqrsService.listar(soloCriticos));
        model.addAttribute("filtro", soloCriticos ? FILTRO_CRITICOS : FILTRO_TODOS);
        model.addAttribute("usuario", principal.getName());
        return "inicio";
    }
}
