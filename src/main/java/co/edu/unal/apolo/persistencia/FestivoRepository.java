package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.Festivo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de Festivo. Spring Data genera la implementación. */
public interface FestivoRepository extends JpaRepository<Festivo, LocalDate> {

    List<Festivo> findByFechaBetweenOrderByFecha(LocalDate desde, LocalDate hasta);
}
