package co.edu.unal.apolo.presentacion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unal.apolo.servicios.PqrsService;

@WebMvcTest(InicioController.class)
class InicioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PqrsService pqrsService;

    @Test
    void laPaginaDeInicioMuestraLaBandeja() throws Exception {
        when(pqrsService.listarConDetalle()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio"))
                .andExpect(model().attributeExists("pqrs"));
    }
}
