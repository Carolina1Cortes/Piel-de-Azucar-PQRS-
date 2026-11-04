package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unal.apolo.dominio.Festivo;
import co.edu.unal.apolo.persistencia.FestivoRepository;

@ExtendWith(MockitoExtension.class)
class DiasHabilesServiceTest {

    @Mock
    private FestivoRepository festivoRepository;

    private DiasHabilesService servicio;

    @BeforeEach
    void preparar() {
        servicio = new DiasHabilesService(festivoRepository, 15);
    }

    @Test
    void laFechaLimiteSaltaFinesDeSemanaYFestivos() {
        // Llega el lunes 19/10/2026; el lunes 2/11 es festivo (Todos los Santos).
        when(festivoRepository.findByFechaBetweenOrderByFecha(any(), any()))
                .thenReturn(List.of(festivo(LocalDate.of(2026, 11, 2))));

        assertThat(servicio.calcularFechaLimite(LocalDate.of(2026, 10, 19))).isEqualTo(LocalDate.of(2026, 11, 10));
    }

    @Test
    void sinFestivosLosQuinceDiasHabilesSonTresSemanasExactas() {
        assertThat(servicio.calcularFechaLimite(LocalDate.of(2026, 10, 19))).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void siLlegaUnSabadoElPrimerDiaHabilEsElLunes() {
        assertThat(servicio.sumarDiasHabiles(LocalDate.of(2026, 10, 24), 1)).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void sumarCeroDiasDevuelveLaMismaFecha() {
        assertThat(servicio.sumarDiasHabiles(LocalDate.of(2026, 10, 19), 0)).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @Test
    void noSumaDiasNegativos() {
        assertThatThrownBy(() -> servicio.sumarDiasHabiles(LocalDate.of(2026, 10, 19), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cuentaLosDiasHabilesQueFaltan() {
        when(festivoRepository.findByFechaBetweenOrderByFecha(any(), any())).thenReturn(List.of());

        // Del miércoles 4/11 al martes 10/11: jueves 5, viernes 6, lunes 9 y martes 10.
        assertThat(servicio.diasHabilesRestantes(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 10))).isEqualTo(4);
    }

    @Test
    void elMismoDiaQuedanCeroDiasHabiles() {
        assertThat(servicio.diasHabilesRestantes(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4))).isZero();
    }

    @Test
    void unCasoVencidoDaDiasHabilesNegativos() {
        when(festivoRepository.findByFechaBetweenOrderByFecha(any(), any()))
                .thenReturn(List.of(festivo(LocalDate.of(2026, 11, 2))));

        // Venció el 20/10; desde entonces pasaron 10 días hábiles hasta el 4/11 (el 2/11 fue festivo).
        assertThat(servicio.diasHabilesRestantes(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 10, 20))).isEqualTo(-10);
    }

    private Festivo festivo(LocalDate fecha) {
        Festivo festivo = new Festivo();
        festivo.setFecha(fecha);
        festivo.setNombre("Festivo de prueba");
        return festivo;
    }
}
