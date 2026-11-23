package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.persistencia.MensajeRepository;
import co.edu.unal.apolo.persistencia.PqrsRepository;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;

/**
 * Comprueba la lista de palabras clave real de data.sql: cada correo de los datos de prueba debe quedar en el
 * tipo de consulta con que se cargó. Si el equipo cambia las palabras clave tras validarlas con la dueña del
 * negocio y esta prueba falla, hay que revisar si el cambio es intencional.
 */
@DataJpaTest
class ClasificacionDatosSemillaTest {

    @Autowired private TipoConsultaRepository tipoConsultaRepository;
    @Autowired private PqrsRepository pqrsRepository;
    @Autowired private MensajeRepository mensajeRepository;

    private ClasificacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ClasificacionService(tipoConsultaRepository);
    }

    @Test
    void cadaCorreoDeLosDatosDePruebaQuedaEnSuTipoDeConsulta() {
        List<Pqrs> casos = pqrsRepository.findAll();
        assertThat(casos).hasSizeGreaterThanOrEqualTo(8);

        for (Pqrs pqrs : casos) {
            if (pqrs.getId() > 999) {
                continue;
            }
            Mensaje entrante = mensajeRepository.findAll().stream()
                    .filter(m -> m.getPqrs().getId().equals(pqrs.getId()))
                    .filter(m -> m.getDireccion() == DireccionMensaje.ENTRANTE)
                    .findFirst().orElseThrow();

            TipoConsulta tipo = servicio.clasificar(entrante.getAsunto(), entrante.getCuerpo());

            assertThat(tipo.getId()).as("tipo del caso %s (%s)", pqrs.getId(), pqrs.getAsunto())
                    .isEqualTo(pqrs.getTipoConsulta().getId());
        }
    }

    @Test
    void unCorreoConEnrojecimientoQuedaComoCritico() {
        TipoConsulta tipo = servicio.clasificar("Ayuda", "Después de usar la crema tengo la cara enrojecida");

        assertThat(tipo.getCategoria()).isEqualTo(Categoria.REACCIONES);
        assertThat(tipo.getPrioridadPorDefecto()).isEqualTo(Prioridad.CRITICA);
    }

    @Test
    void unCorreoSinCoincidenciasQuedaParaClasificarAMano() {
        TipoConsulta tipo = servicio.clasificar("Hola", "Buenas tardes, quisiera hablar con alguien del equipo");

        assertThat(tipo.getCategoria()).isEqualTo(Categoria.POR_CLASIFICAR);
    }
}
