package co.edu.unal.apolo.presentacion;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.Cliente;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.servicios.DetalleCaso;
import co.edu.unal.apolo.servicios.DetalleCaso.MensajeVista;
import co.edu.unal.apolo.servicios.PqrsService;
import co.edu.unal.apolo.servicios.RecursoNoEncontradoException;
import co.edu.unal.apolo.servicios.ReglaNegocioException;

@WebMvcTest(CasoController.class)
@WithMockUser(username = "operador1@pieldeazucar.co")
class CasoControllerTest {

    private static final String CORREO = "operador1@pieldeazucar.co";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PqrsService pqrsService;

    @Test
    void unCasoCriticoSinAsignarMuestraElBannerYTieneElEnvioDeshabilitado() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(
                detalle(true, true, false, "Caso crítico: toma el caso antes de responder"));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(content().string(containsString("Automatización pausada")))
                .andExpect(content().string(containsString("Tomar caso")))
                .andExpect(content().string(containsString("toma el caso antes de responder")))
                .andExpect(content().string(containsString("disabled")))
                .andExpect(content().string(containsString("Valentina Ortiz")))
                .andExpect(content().string(containsString("me salió enrojecimiento")));
    }

    @Test
    void unCasoTomadoNoMuestraElBannerYHabilitaElEnvio() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(get("/pqrs/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Automatización pausada"))))
                .andExpect(content().string(not(containsString("Tomar caso"))))
                .andExpect(content().string(not(containsString("disabled"))))
                .andExpect(content().string(containsString("Enviar respuesta")));
    }

    @Test
    void unCasoInexistenteDevuelve404() throws Exception {
        when(pqrsService.detalle(eq(404L), any())).thenThrow(new RecursoNoEncontradoException("PQRS", 404L));

        mockMvc.perform(get("/pqrs/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("no-encontrado"));
    }

    @Test
    void tomarElCasoLlamaAlServicioConElOperadorDeLaSesionYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/tomar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).tomarCaso(10L, CORREO);
    }

    @Test
    void siNoSePuedeTomarElCasoSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("El caso ya tiene responsable")).when(pqrsService).tomarCaso(10L, CORREO);

        mockMvc.perform(post("/pqrs/10/tomar").with(csrf()))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "El caso ya tiene responsable"));
    }

    @Test
    void tomarSinTokenCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/pqrs/10/tomar"))
                .andExpect(status().isForbidden());

        verify(pqrsService, never()).tomarCaso(any(), any());
    }

    @Test
    void responderEnviaElMensajeYVuelveAlCaso() throws Exception {
        mockMvc.perform(post("/pqrs/10/responder").with(csrf())
                        .param("asunto", "Sobre tu reacción")
                        .param("cuerpo", "Hola Valentina, lamentamos lo ocurrido."))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attributeExists("mensajeOk"));

        verify(pqrsService).enviarRespuesta(10L, CORREO, "Sobre tu reacción", "Hola Valentina, lamentamos lo ocurrido.");
    }

    @Test
    void responderConElMensajeVacioVuelveAlFormularioConElError() throws Exception {
        when(pqrsService.detalle(10L, CORREO)).thenReturn(detalle(false, false, true, null));

        mockMvc.perform(post("/pqrs/10/responder").with(csrf()).param("cuerpo", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("caso"))
                .andExpect(model().attributeHasFieldErrors("formulario", "cuerpo"));

        verify(pqrsService, never()).enviarRespuesta(any(), any(), any(), any());
    }

    @Test
    void siElServicioRechazaElEnvioSeMuestraElMotivo() throws Exception {
        doThrow(new ReglaNegocioException("Caso crítico: toma el caso antes de responder"))
                .when(pqrsService).enviarRespuesta(10L, CORREO, null, "Hola");

        mockMvc.perform(post("/pqrs/10/responder").with(csrf()).param("cuerpo", "Hola"))
                .andExpect(redirectedUrl("/pqrs/10"))
                .andExpect(flash().attribute("mensajeError", "Caso crítico: toma el caso antes de responder"));
    }

    private DetalleCaso detalle(boolean criticoSinAsignar, boolean puedeTomar, boolean puedeResponder, String motivo) {
        Cliente cliente = new Cliente();
        cliente.setNombre("Valentina Ortiz");
        cliente.setCorreo("valentina.ortiz@example.com");
        TipoConsulta tipo = new TipoConsulta();
        tipo.setCategoria(Categoria.REACCIONES);
        tipo.setDescripcion("Enrojecimiento, irritación o alergia");
        Pqrs pqrs = new Pqrs();
        pqrs.setId(10L);
        pqrs.setAsunto("Me salió enrojecimiento con el sérum");
        pqrs.setCanal(Canal.GMAIL);
        pqrs.setEstado(criticoSinAsignar ? EstadoPqrs.PENDIENTE : EstadoPqrs.EN_ATENCION);
        pqrs.setPrioridad(Prioridad.CRITICA);
        pqrs.setFechaLimite(LocalDate.of(2026, 11, 9));
        pqrs.setCliente(cliente);
        pqrs.setTipoConsulta(tipo);
        MensajeVista mensaje = new MensajeVista(LocalDateTime.of(2026, 11, 3, 10, 0), DireccionMensaje.ENTRANTE,
                "Me salió enrojecimiento con el sérum", "Hola, me salió enrojecimiento y ardor.", false, null);
        return new DetalleCaso(pqrs, List.of(mensaje), List.of(), 4, criticoSinAsignar, puedeTomar, puedeResponder, motivo);
    }
}
