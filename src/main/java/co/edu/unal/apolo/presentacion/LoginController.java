package co.edu.unal.apolo.presentacion;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantalla de inicio de sesión (el proceso de autenticación lo hace Spring Security). */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
