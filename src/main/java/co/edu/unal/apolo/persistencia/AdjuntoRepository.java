package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.Adjunto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de Adjunto. Spring Data genera la implementación. */
public interface AdjuntoRepository extends JpaRepository<Adjunto, Long> {

    List<Adjunto> findByMensajeId(Long mensajeId);

    List<Adjunto> findByPlantillaId(Long plantillaId);
}
