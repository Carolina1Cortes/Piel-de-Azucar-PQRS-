package co.edu.unal.apolo.presentacion;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.Cliente;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.servicios.PqrsService;
import co.edu.unal.apolo.servicios.ResumenPqrs;

@WebMvcTest(InicioController.class)
@WithMockUser(username = "operador1@pieldeazucar.co")
class InicioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PqrsService pqrsService;

    @Test
    void laBandejaMuestraTodosLosCasosPorDefecto() throws Exception {
        when(pqrsService.listar(false)).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio"))
                .andExpect(model().attributeExists("pqrs"))
                .andExpect(model().attribute("filtro", "todos"))
                .andExpect(model().attribute("usuario", "operador1@pieldeazucar.co"));
    }

    @Test
    void elFiltroCriticosPideSoloLosCasosCriticos() throws Exception {
        when(pqrsService.listar(true)).thenReturn(List.of());

        mockMvc.perform(get("/").param("filtro", "criticos"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtro", "criticos"));

        verify(pqrsService).listar(true);
    }

    @Test
    void unFiltroDesconocidoCaeEnTodos() throws Exception {
        when(pqrsService.listar(false)).thenReturn(List.of());

        mockMvc.perform(get("/").param("filtro", "cualquiera"))
                .andExpect(model().attribute("filtro", "todos"));
    }

    @Test
    void laBandejaMuestraIdClientaCategoriaPrioridadEstadoYPlazoEnDiasHabiles() throws Exception {
        when(pqrsService.listar(false)).thenReturn(List.of(
                new ResumenPqrs(pqrs(2L, "Valentina Ortiz", Prioridad.CRITICA, EstadoPqrs.PENDIENTE), 4),
                new ResumenPqrs(pqrs(7L, "Camila Rojas", Prioridad.ALTA, EstadoPqrs.EN_ATENCION), -3)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Valentina Ortiz")))
                .andExpect(content().string(containsString("Reacciones o molestias con productos")))
                .andExpect(content().string(containsString("Crítica")))
                .andExpect(content().string(containsString("Pendiente")))
                .andExpect(content().string(containsString("09/11/2026")))
                .andExpect(content().string(containsString("4 día(s) hábil(es)")))
                .andExpect(content().string(containsString("Vencida hace 3 día(s) hábil(es)")))
                .andExpect(content().string(containsString("/pqrs/2")));
    }

    @Test
    @WithAnonymousUser
    void sinSesionRedirigeAlLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    private Pqrs pqrs(Long id, String clienta, Prioridad prioridad, EstadoPqrs estado) {
        Cliente cliente = new Cliente();
        cliente.setNombre(clienta);
        TipoConsulta tipo = new TipoConsulta();
        tipo.setCategoria(Categoria.REACCIONES);
        Pqrs pqrs = new Pqrs();
        pqrs.setId(id);
        pqrs.setAsunto("Asunto " + id);
        pqrs.setCliente(cliente);
        pqrs.setTipoConsulta(tipo);
        pqrs.setPrioridad(prioridad);
        pqrs.setEstado(estado);
        pqrs.setFechaLimite(LocalDate.of(2026, 11, 9));
        return pqrs;
    }
}
