package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.TipoConsulta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de TipoConsulta. Spring Data genera la implementación. */
public interface TipoConsultaRepository extends JpaRepository<TipoConsulta, Long> {

    List<TipoConsulta> findByCategoria(Categoria categoria);
}
