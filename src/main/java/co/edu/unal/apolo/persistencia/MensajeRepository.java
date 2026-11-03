package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.Mensaje;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de Mensaje. Spring Data genera la implementación. */
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByPqrsIdOrderByFechaAsc(Long pqrsId);
}
