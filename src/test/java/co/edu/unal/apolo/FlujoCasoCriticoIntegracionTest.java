package co.edu.unal.apolo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unal.apolo.persistencia.BitacoraRepository;
import co.edu.unal.apolo.persistencia.MensajeRepository;

/**
 * Recorre con el contexto completo (login, capas, H2 y canal simulado) el objetivo del incremento:
 * un operador autenticado abre un caso crítico, lo toma, envía una respuesta y completa un paso del protocolo,
 * y todo queda en la bitácora.
 * Usa su propia base en memoria porque modifica los datos semilla.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:apolo-flujo;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class FlujoCasoCriticoIntegracionTest {

    private final RequestPostProcessor operador = user("operador1@pieldeazucar.co").roles("OPERADOR");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private BitacoraRepository bitacoraRepository;

    @Test
    void elOperadorAbreUnCasoCriticoLoTomaYLoResponde() throws Exception {
        // La bandeja filtra los casos críticos.
        mockMvc.perform(get("/").param("filtro", "criticos").with(operador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("enrojecimiento")));

        // Un caso crítico sin asignar muestra el banner y ofrece tomarlo.
        mockMvc.perform(get("/pqrs/2").with(operador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Automatización pausada")))
                .andExpect(content().string(containsString("Tomar caso")));

        // Responder antes de tomarlo es rechazado y no deja rastro.
        mockMvc.perform(post("/pqrs/2/responder").with(operador).with(csrf()).param("cuerpo", "Hola"))
                .andExpect(redirectedUrl("/pqrs/2"))
                .andExpect(flash().attributeExists("mensajeError"));
        assertThat(mensajeRepository.findByPqrsIdOrderByFechaAsc(2L)).hasSize(1);

        // Tomar el caso lo pasa a "Atención inmediata" y quita el banner.
        mockMvc.perform(post("/pqrs/2/tomar").with(operador).with(csrf()))
                .andExpect(redirectedUrl("/pqrs/2"))
                .andExpect(flash().attributeExists("mensajeOk"));
        mockMvc.perform(get("/pqrs/2").with(operador))
                .andExpect(content().string(not(containsString("Automatización pausada"))))
                .andExpect(content().string(containsString("Atención inmediata")));

        // Ahora sí puede responder por el canal simulado.
        mockMvc.perform(post("/pqrs/2/responder").with(operador).with(csrf())
                        .param("asunto", "Sobre tu reacción")
                        .param("cuerpo", "Hola Valentina, lamentamos lo ocurrido."))
                .andExpect(redirectedUrl("/pqrs/2"))
                .andExpect(flash().attributeExists("mensajeOk"));

        assertThat(mensajeRepository.findByPqrsIdOrderByFechaAsc(2L))
                .extracting(m -> m.getDireccion().name()).containsExactly("ENTRANTE", "SALIENTE");
        assertThat(bitacoraRepository.findByPqrsIdOrderByFechaAsc(2L))
                .extracting(r -> r.getAccion().name()).containsExactly("CASO_TOMADO", "RESPUESTA_ENVIADA");

        // La conversación y la bitácora se ven en la pantalla del caso.
        mockMvc.perform(get("/pqrs/2").with(operador))
                .andExpect(content().string(containsString("Hola Valentina, lamentamos lo ocurrido.")))
                .andExpect(content().string(containsString("Caso tomado")))
                .andExpect(content().string(containsString("Respuesta enviada")));

        // Completar un paso del protocolo crítico queda en la bitácora de la página.
        mockMvc.perform(post("/pqrs/2/protocolo/SUSPENDER_USO").with(operador).with(csrf()))
                .andExpect(redirectedUrl("/pqrs/2"))
                .andExpect(flash().attributeExists("mensajeOk"));
        assertThat(bitacoraRepository.findByPqrsIdOrderByFechaAsc(2L))
                .extracting(r -> r.getAccion().name()).contains("PASO_PROTOCOLO_COMPLETADO");
        mockMvc.perform(get("/pqrs/2").with(operador))
                .andExpect(content().string(containsString("Protocolo crítico")))
                .andExpect(content().string(containsString("Paso del protocolo completado")))
                .andExpect(content().string(containsString("✔ Pedir a la persona cliente que suspenda el uso del producto")));
    }

    @Test
    void unCorreoSimuladoDeUnaReaccionLlegaComoCasoCriticoSinAsignar() throws Exception {
        MvcResult creada = mockMvc.perform(post("/simulador").with(operador).with(csrf())
                        .param("nombre", "Ana Torres")
                        .param("correo", "ana.torres@example.com")
                        .param("asunto", "Alergia con la crema nocturna")
                        .param("cuerpo", "Me salieron ronchas después de usarla.")
                        .param("tipoConsultaId", "18"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/pqrs/*"))
                .andReturn();
        String ruta = creada.getResponse().getRedirectedUrl();

        mockMvc.perform(get(ruta).with(operador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Automatización pausada")))
                .andExpect(content().string(containsString("Ana Torres")))
                .andExpect(content().string(containsString("Me salieron ronchas")));

        mockMvc.perform(get("/").param("filtro", "criticos").with(operador))
                .andExpect(content().string(containsString("Alergia con la crema nocturna")));
    }

    @Test
    void unCasoInexistenteMuestraUna404() throws Exception {
        mockMvc.perform(get("/pqrs/987654").with(operador))
                .andExpect(status().isNotFound());
    }
}
