package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.Cliente;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.integracion.CanalMensajeria;
import co.edu.unal.apolo.persistencia.ClienteRepository;
import co.edu.unal.apolo.persistencia.MensajeRepository;
import co.edu.unal.apolo.persistencia.PqrsRepository;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;

@ExtendWith(MockitoExtension.class)
class EntradaSimuladaServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    @Mock private ClienteRepository clienteRepository;
    @Mock private TipoConsultaRepository tipoConsultaRepository;
    @Mock private PqrsRepository pqrsRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private DiasHabilesService diasHabiles;
    @Mock private CanalMensajeria canal;

    private EntradaSimuladaService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneId.of("UTC"));
        servicio = new EntradaSimuladaService(clienteRepository, tipoConsultaRepository, pqrsRepository,
                mensajeRepository, diasHabiles, reloj, canal);
    }

    @Test
    void creaLaPqrsConSuClientaSuMensajeEntranteYElPlazoEnDiasHabiles() {
        TipoConsulta reaccion = tipo(7L, Prioridad.CRITICA);
        when(tipoConsultaRepository.findById(7L)).thenReturn(Optional.of(reaccion));
        when(clienteRepository.findByCorreo("ana@example.com")).thenReturn(Optional.empty());
        when(diasHabiles.calcularFechaLimite(HOY)).thenReturn(LocalDate.of(2026, 11, 25));
        asignarIdAlGuardar();

        Pqrs pqrs = servicio.registrarCorreoEntrante("  Ana Torres ", " Ana@Example.com ", " Me salió una alergia ",
                " Me ardió la cara después de usar el sérum. ", 7L);

        assertThat(pqrs.getIdExterno()).startsWith("simulado-");
        assertThat(pqrs.getCanal()).isEqualTo(Canal.GMAIL);
        assertThat(pqrs.getEstado()).isEqualTo(EstadoPqrs.PENDIENTE);
        assertThat(pqrs.getPrioridad()).isEqualTo(Prioridad.CRITICA);
        assertThat(pqrs.getAsunto()).isEqualTo("Me salió una alergia");
        assertThat(pqrs.getFechaLlegada()).isEqualTo(LocalDateTime.of(2026, 11, 4, 15, 0));
        assertThat(pqrs.getFechaLimite()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(pqrs.getResponsable()).isNull();
        assertThat(pqrs.getTipoConsulta()).isSameAs(reaccion);
        assertThat(pqrs.getCliente().getNombre()).isEqualTo("Ana Torres");
        assertThat(pqrs.getCliente().getCorreo()).isEqualTo("ana@example.com");
        verify(pqrsRepository).save(pqrs);

        assertThat(pqrs.getRadicado()).isEqualTo("PQRS-2026-001005");

        ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
        verify(mensajeRepository, times(2)).save(captor.capture());
        List<Mensaje> guardados = captor.getAllValues();

        Mensaje entrante = guardados.get(0);
        assertThat(entrante.getDireccion()).isEqualTo(DireccionMensaje.ENTRANTE);
        assertThat(entrante.getCuerpo()).isEqualTo("Me ardió la cara después de usar el sérum.");
        assertThat(entrante.getPqrs()).isSameAs(pqrs);
        assertThat(entrante.getAutor()).isNull();

        Mensaje saliente = guardados.get(1);
        assertThat(saliente.getDireccion()).isEqualTo(DireccionMensaje.SALIENTE);
        assertThat(saliente.isAutomatico()).isTrue();
        assertThat(saliente.getAutor()).isNull();
        assertThat(saliente.getPqrs()).isSameAs(pqrs);
        assertThat(saliente.getAsunto()).isEqualTo("Recibimos tu solicitud - Radicado PQRS-2026-001005");
        assertThat(saliente.getCuerpo()).contains("Hola Ana Torres", "PQRS-2026-001005", "25/11/2026");
        verify(canal).enviar(saliente);
    }

    @Test
    void generaElRadicadoConElAnioYElIdConCerosALaIzquierda() {
        assertThat(EntradaSimuladaService.generarRadicado(2026, 7)).isEqualTo("PQRS-2026-000007");
    }

    @Test
    void reutilizaLaClientaSiYaExistePorCorreo() {
        Cliente existente = new Cliente();
        existente.setNombre("Camila Rojas");
        existente.setCorreo("camila.rojas@example.com");
        when(tipoConsultaRepository.findById(3L)).thenReturn(Optional.of(tipo(3L, Prioridad.MEDIA)));
        when(clienteRepository.findByCorreo("camila.rojas@example.com")).thenReturn(Optional.of(existente));
        when(diasHabiles.calcularFechaLimite(HOY)).thenReturn(LocalDate.of(2026, 11, 25));
        asignarIdAlGuardar();

        Pqrs pqrs = servicio.registrarCorreoEntrante("Camila R.", "camila.rojas@example.com", "Mi envío", "¿Dónde está?", 3L);

        assertThat(pqrs.getCliente()).isSameAs(existente);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void exigeTodosLosCampos() {
        assertThatThrownBy(() -> servicio.registrarCorreoEntrante(" ", "a@b.co", "Asunto", "Texto", 1L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.registrarCorreoEntrante("Ana", null, "Asunto", "Texto", 1L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.registrarCorreoEntrante("Ana", "a@b.co", "", "Texto", 1L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.registrarCorreoEntrante("Ana", "a@b.co", "Asunto", " ", 1L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.registrarCorreoEntrante("Ana", "a@b.co", "Asunto", "Texto", null))
                .isInstanceOf(ReglaNegocioException.class);
        verify(pqrsRepository, never()).save(any());
    }

    @Test
    void fallaSiElTipoDeConsultaNoExiste() {
        when(tipoConsultaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrarCorreoEntrante("Ana", "a@b.co", "Asunto", "Texto", 99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(pqrsRepository, never()).save(any());
    }

    private void asignarIdAlGuardar() {
        when(pqrsRepository.save(any())).thenAnswer(invocacion -> {
            Pqrs p = invocacion.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 1005L);
            return p;
        });
    }

    private TipoConsulta tipo(Long id, Prioridad prioridad) {
        TipoConsulta tipo = new TipoConsulta();
        tipo.setId(id);
        tipo.setDescripcion("Tipo de prueba");
        tipo.setPrioridadPorDefecto(prioridad);
        return tipo;
    }
}
