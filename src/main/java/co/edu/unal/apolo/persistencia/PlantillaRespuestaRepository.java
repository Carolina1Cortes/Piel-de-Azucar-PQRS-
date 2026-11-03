package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.PlantillaRespuesta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de PlantillaRespuesta. Spring Data genera la implementación. */
public interface PlantillaRespuestaRepository extends JpaRepository<PlantillaRespuesta, Long> {

    List<PlantillaRespuesta> findByActivaTrue();

    List<PlantillaRespuesta> findByTipoConsultaId(Long tipoConsultaId);
}
