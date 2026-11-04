package co.edu.unal.apolo.servicios;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.dominio.Festivo;
import co.edu.unal.apolo.persistencia.FestivoRepository;

/**
 * Regla del plazo legal: los días hábiles excluyen sábados, domingos y los festivos de la tabla {@code festivo}.
 * El plazo de respuesta (15 días hábiles) se configura con {@code apolo.plazo.dias-habiles}.
 */
@Service
public class DiasHabilesService {

    private final FestivoRepository festivoRepository;
    private final int plazoDiasHabiles;

    public DiasHabilesService(FestivoRepository festivoRepository,
                              @Value("${apolo.plazo.dias-habiles:15}") int plazoDiasHabiles) {
        this.festivoRepository = festivoRepository;
        this.plazoDiasHabiles = plazoDiasHabiles;
    }

    /** Fecha límite de respuesta para una PQRS que llega en la fecha dada: el plazo configurado en días hábiles. */
    @Transactional(readOnly = true)
    public LocalDate calcularFechaLimite(LocalDate fechaLlegada) {
        return sumarDiasHabiles(fechaLlegada, plazoDiasHabiles);
    }

    /** Suma días hábiles a una fecha. El día de partida no cuenta. */
    @Transactional(readOnly = true)
    public LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("Los días a sumar no pueden ser negativos");
        }
        Set<LocalDate> festivos = festivosEntre(desde, desde.plusDays(dias * 2L + 30));
        LocalDate fecha = desde;
        int sumados = 0;
        while (sumados < dias) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha, festivos)) {
                sumados++;
            }
        }
        return fecha;
    }

    /**
     * Días hábiles que faltan entre hoy y la fecha límite (sin contar hoy, contando la fecha límite).
     * Es 0 si es el mismo día y negativo si la fecha límite ya pasó.
     */
    @Transactional(readOnly = true)
    public int diasHabilesRestantes(LocalDate hoy, LocalDate fechaLimite) {
        if (fechaLimite.equals(hoy)) {
            return 0;
        }
        boolean vencida = fechaLimite.isBefore(hoy);
        LocalDate inicio = vencida ? fechaLimite : hoy;
        LocalDate fin = vencida ? hoy : fechaLimite;
        Set<LocalDate> festivos = festivosEntre(inicio, fin);
        int cuenta = 0;
        for (LocalDate dia = inicio.plusDays(1); !dia.isAfter(fin); dia = dia.plusDays(1)) {
            if (esHabil(dia, festivos)) {
                cuenta++;
            }
        }
        return vencida ? -cuenta : cuenta;
    }

    private boolean esHabil(LocalDate fecha, Set<LocalDate> festivos) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY && !festivos.contains(fecha);
    }

    private Set<LocalDate> festivosEntre(LocalDate desde, LocalDate hasta) {
        return festivoRepository.findByFechaBetweenOrderByFecha(desde, hasta).stream()
                .map(Festivo::getFecha)
                .collect(Collectors.toSet());
    }
}
