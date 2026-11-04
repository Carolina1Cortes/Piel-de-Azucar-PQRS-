package co.edu.unal.apolo.presentacion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.servicios.EntradaSimuladaService;
import co.edu.unal.apolo.servicios.ReglaNegocioException;

@WebMvcTest(SimuladorController.class)
@WithMockUser(username = "operador1@pieldeazucar.co")
class SimuladorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntradaSimuladaService entradaSimuladaService;

    @Test
    void muestraElFormularioConLosTiposDeConsulta() throws Exception {
        mockMvc.perform(get("/simulador"))
                .andExpect(status().isOk())
                .andExpect(view().name("simulador"))
                .andExpect(model().attributeExists("formulario", "tipos", "usuario"));
    }

    @Test
    void unCorreoValidoCreaElCasoYLlevaAlDetalle() throws Exception {
        Pqrs creada = new Pqrs();
        creada.setId(1000L);
        when(entradaSimuladaService.registrarCorreoEntrante("Ana Torres", "ana@example.com", "Mi pedido", "Hola", 7L))
                .thenReturn(creada);

        mockMvc.perform(post("/simulador").with(csrf())
                        .param("nombre", "Ana Torres")
                        .param("correo", "ana@example.com")
                        .param("asunto", "Mi pedido")
                        .param("cuerpo", "Hola")
                        .param("tipoConsultaId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pqrs/1000"));
    }

    @Test
    void unFormularioIncompletoVuelveConLosErrores() throws Exception {
        mockMvc.perform(post("/simulador").with(csrf())
                        .param("nombre", "Ana Torres")
                        .param("correo", "no-es-un-correo")
                        .param("asunto", "")
                        .param("cuerpo", "Hola"))
                .andExpect(status().isOk())
                .andExpect(view().name("simulador"))
                .andExpect(model().attributeHasFieldErrors("formulario", "correo", "asunto", "tipoConsultaId"));

        verify(entradaSimuladaService, never()).registrarCorreoEntrante(any(), any(), any(), any(), any());
    }

    @Test
    void siElServicioRechazaElCorreoSeMuestraElErrorEnElFormulario() throws Exception {
        when(entradaSimuladaService.registrarCorreoEntrante(any(), any(), any(), any(), any()))
                .thenThrow(new ReglaNegocioException("Debe elegir un tipo de consulta"));

        mockMvc.perform(post("/simulador").with(csrf())
                        .param("nombre", "Ana Torres")
                        .param("correo", "ana@example.com")
                        .param("asunto", "Mi pedido")
                        .param("cuerpo", "Hola")
                        .param("tipoConsultaId", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("simulador"))
                .andExpect(model().attributeHasErrors("formulario"));
    }
}
