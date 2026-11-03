package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.persistencia.PqrsRepository;

@ExtendWith(MockitoExtension.class)
class PqrsServiceTest {

    @Mock
    private PqrsRepository pqrsRepository;

    @InjectMocks
    private PqrsService pqrsService;

    @Test
    void buscarPorIdDevuelveLaPqrsExistente() {
        Pqrs pqrs = new Pqrs();
        pqrs.setId(7L);
        when(pqrsRepository.findById(7L)).thenReturn(Optional.of(pqrs));

        assertThat(pqrsService.buscarPorId(7L)).isSameAs(pqrs);
    }

    @Test
    void buscarPorIdLanzaExcepcionSiNoExiste() {
        when(pqrsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pqrsService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listarConDetalleDelegaEnElRepositorio() {
        when(pqrsRepository.findAllConDetalle()).thenReturn(List.of(new Pqrs(), new Pqrs()));

        assertThat(pqrsService.listarConDetalle()).hasSize(2);
    }
}
