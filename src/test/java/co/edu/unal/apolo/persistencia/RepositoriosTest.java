package co.edu.unal.apolo.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.EstadoPqrs;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.Rol;
import co.edu.unal.apolo.dominio.TipoAutomatizacion;

/**
 * Pruebas de la capa de acceso a datos sobre el esquema real (schema.sql + data.sql en H2 en memoria).
 * No necesitan internet.
 */
@DataJpaTest
class RepositoriosTest {

    @Autowired private PqrsRepository pqrsRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoConsultaRepository tipoConsultaRepository;
    @Autowired private PlantillaRespuestaRepository plantillaRepository;
    @Autowired private MensajeRepository mensajeRepository;
    @Autowired private AdjuntoRepository adjuntoRepository;
    @Autowired private BitacoraRepository bitacoraRepository;
    @Autowired private FestivoRepository festivoRepository;

    // ---------- datos de prueba cargados

    @Test
    void losDatosDePruebaSeCargan() {
        assertThat(clienteRepository.count()).isGreaterThanOrEqualTo(5);
        assertThat(usuarioRepository.count()).isEqualTo(3);
        assertThat(tipoConsultaRepository.count()).isGreaterThanOrEqualTo(8);
        assertThat(pqrsRepository.count()).isGreaterThanOrEqualTo(8);
    }

    @Test
    void hayUnAdminYDosOperadores() {
        assertThat(usuarioRepository.findByRol(Rol.ADMIN)).hasSize(1);
        assertThat(usuarioRepository.findByRol(Rol.OPERADOR)).hasSize(2);
        assertThat(usuarioRepository.findByCorreo("paula@pieldeazucar.co")).isPresent();
    }

    @Test
    void unOperadorDePruebaEstaBloqueadoYElOtroNo() {
        assertThat(usuarioRepository.findByCorreo("operador1@pieldeazucar.co").orElseThrow().isBloqueado()).isFalse();
        assertThat(usuarioRepository.findByCorreo("operador2@pieldeazucar.co").orElseThrow().isBloqueado()).isTrue();
    }

    @Test
    void elCatalogoCubreLasOchoCategorias() {
        for (Categoria categoria : Categoria.values()) {
            assertThat(tipoConsultaRepository.findByCategoria(categoria))
                    .as("tipos de consulta de %s", categoria).isNotEmpty();
        }
    }

    @Test
    void lasReaccionesSonCriticasYDeAtencionHumana() {
        tipoConsultaRepository.findByCategoria(Categoria.REACCIONES).forEach(tipo -> {
            assertThat(tipo.getPrioridadPorDefecto()).isEqualTo(Prioridad.CRITICA);
            assertThat(tipo.getAutomatizacion()).isEqualTo(TipoAutomatizacion.ATENCION_HUMANA);
        });
    }

    // ---------- consultas de PQRS

    @Test
    void buscaPqrsPorIdExterno() {
        assertThat(pqrsRepository.findByIdExterno("gmail-msg-002")).isPresent();
        assertThat(pqrsRepository.existsByIdExterno("gmail-msg-002")).isTrue();
        assertThat(pqrsRepository.existsByIdExterno("no-existe")).isFalse();
    }

    @Test
    void filtraPorEstadoYPrioridad() {
        assertThat(pqrsRepository.findByEstado(EstadoPqrs.RESUELTA)).hasSize(2);
        assertThat(pqrsRepository.findByPrioridad(Prioridad.CRITICA))
                .extracting(Pqrs::getIdExterno).containsExactly("gmail-msg-002");
    }

    @Test
    void encuentraLasPqrsVencidasSinContarLasResueltas() {
        List<Pqrs> vencidas = pqrsRepository
                .findByEstadoNotAndFechaLimiteBefore(EstadoPqrs.RESUELTA, LocalDate.of(2026, 10, 26));

        assertThat(vencidas).extracting(Pqrs::getIdExterno).containsExactly("gmail-msg-007");
    }

    @Test
    void listaConDetalleOrdenadaPorFechaLimite() {
        List<Pqrs> todas = pqrsRepository.findAllConDetalle();

        assertThat(todas).hasSizeGreaterThanOrEqualTo(8);
        assertThat(todas.get(0).getIdExterno()).isEqualTo("gmail-msg-007");
        assertThat(todas).extracting(Pqrs::getFechaLimite).isSorted();
        assertThat(todas.get(0).getCliente().getNombre()).isNotBlank();
    }

    @Test
    void cargaUnaPqrsConSuDetalleParaLaVista() {
        Pqrs critica = pqrsRepository.findByIdConDetalle(2L).orElseThrow();
        assertThat(critica.getCliente().getNombre()).isEqualTo("Valentina Ortiz");
        assertThat(critica.getTipoConsulta().getCategoria()).isEqualTo(Categoria.REACCIONES);
        assertThat(critica.getResponsable()).isNull();
        assertThat(pqrsRepository.findByIdConDetalle(999L)).isEmpty();
    }

    @Test
    void listaSoloLasCriticasConDetalle() {
        List<Pqrs> criticas = pqrsRepository.findByPrioridadConDetalle(Prioridad.CRITICA);
        assertThat(criticas).extracting(Pqrs::getIdExterno).containsExactly("gmail-msg-002");
        assertThat(criticas.get(0).getCliente().getNombre()).isNotBlank();
    }

    @Test
    void filtraPorResponsable() {
        Long operadorId = usuarioRepository.findByCorreo("operador1@pieldeazucar.co").orElseThrow().getId();

        assertThat(pqrsRepository.findByResponsableId(operadorId)).hasSize(4);
    }

    // ---------- mensajes, adjuntos, bitácora, festivos, plantillas

    @Test
    void recuperaLaConversacionEnOrden() {
        Long pqrsId = pqrsRepository.findByIdExterno("gmail-msg-003").orElseThrow().getId();

        assertThat(mensajeRepository.findByPqrsIdOrderByFechaAsc(pqrsId))
                .extracting(m -> m.getDireccion().name()).containsExactly("ENTRANTE", "SALIENTE");
    }

    @Test
    void recuperaLaBitacoraDeUnCaso() {
        Long pqrsId = pqrsRepository.findByIdExterno("gmail-msg-005").orElseThrow().getId();

        assertThat(bitacoraRepository.findByPqrsIdOrderByFechaAsc(pqrsId))
                .extracting(r -> r.getAccion().name()).containsExactly("CASO_TOMADO", "ESCALADA");
    }

    @Test
    void recuperaAdjuntosDeMensajesYDePlantillas() {
        Long pqrsId = pqrsRepository.findByIdExterno("gmail-msg-002").orElseThrow().getId();
        Long mensajeId = mensajeRepository.findByPqrsIdOrderByFechaAsc(pqrsId).get(0).getId();

        assertThat(adjuntoRepository.findByMensajeId(mensajeId)).hasSize(1);
        assertThat(adjuntoRepository.findByPlantillaId(2L)).hasSize(1);
    }

    @Test
    void conoceLosFestivosDeColombia() {
        assertThat(festivoRepository.existsById(LocalDate.of(2026, 12, 25))).isTrue();
        assertThat(festivoRepository.findByFechaBetweenOrderByFecha(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30)))
                .extracting(f -> f.getFecha().toString())
                .containsExactly("2026-10-12", "2026-11-02", "2026-11-16");
    }

    @Test
    void listaSoloPlantillasActivas() {
        assertThat(plantillaRepository.findByActivaTrue()).hasSize(4);
        assertThat(plantillaRepository.findByTipoConsultaId(5L)).hasSize(1);
    }

    // ---------- restricciones del esquema

    @Test
    void noPermiteDosPqrsConElMismoIdExterno() {
        pqrsRepository.saveAndFlush(nuevaPqrs("correo-unico-1"));

        assertThatThrownBy(() -> pqrsRepository.saveAndFlush(nuevaPqrs("correo-unico-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unAdjuntoDebeTenerUnDuenio() {
        Adjunto huerfano = new Adjunto();
        huerfano.setNombreArchivo("huerfano.txt");
        huerfano.setRuta("adjuntos/huerfano.txt");

        assertThatThrownBy(() -> adjuntoRepository.saveAndFlush(huerfano))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void losIdentificadoresNuevosNoChocanConLosDatosDePrueba() {
        Pqrs guardada = pqrsRepository.saveAndFlush(nuevaPqrs("correo-nuevo"));

        assertThat(guardada.getId()).isGreaterThanOrEqualTo(1000L);
    }

    private Pqrs nuevaPqrs(String idExterno) {
        Pqrs pqrs = new Pqrs();
        pqrs.setIdExterno(idExterno);
        pqrs.setCanal(Canal.GMAIL);
        pqrs.setFechaLlegada(LocalDateTime.of(2026, 10, 21, 9, 0));
        pqrs.setAsunto("Prueba");
        pqrs.setEstado(EstadoPqrs.PENDIENTE);
        pqrs.setPrioridad(Prioridad.BAJA);
        pqrs.setFechaLimite(LocalDate.of(2026, 11, 11));
        pqrs.setCliente(clienteRepository.findAll().get(0));
        pqrs.setTipoConsulta(tipoConsultaRepository.findAll().get(0));
        return pqrs;
    }
}
