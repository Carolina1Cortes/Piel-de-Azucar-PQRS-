package co.edu.unal.apolo.seguridad;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** S1-03: sin sesión no se entra a ninguna pantalla y un operador bloqueado no puede ingresar. Usa los usuarios semilla. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:apolo-seguridad;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SeguridadIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinSesionNoSeAccedeANingunaPantalla() throws Exception {
        for (String ruta : new String[] {"/", "/pqrs/2", "/simulador", "/h2-console"}) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void laPantallaDeLoginEsPublica() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void unOperadorConCredencialesValidasIngresa() throws Exception {
        mockMvc.perform(formLogin("/login").user("operador1@pieldeazucar.co").password("apolo2026"))
                .andExpect(authenticated().withUsername("operador1@pieldeazucar.co"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void elAdminIngresaConSuRol() throws Exception {
        mockMvc.perform(formLogin("/login").user("paula@pieldeazucar.co").password("apolo2026"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void unOperadorBloqueadoNoPuedeIngresarAunqueLaClaveSeaCorrecta() throws Exception {
        mockMvc.perform(formLogin("/login").user("operador2@pieldeazucar.co").password("apolo2026"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unaClaveIncorrectaNoIngresa() throws Exception {
        mockMvc.perform(formLogin("/login").user("operador1@pieldeazucar.co").password("incorrecta"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unCorreoQueNoExisteNoIngresa() throws Exception {
        mockMvc.perform(formLogin("/login").user("nadie@pieldeazucar.co").password("apolo2026"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }
}
