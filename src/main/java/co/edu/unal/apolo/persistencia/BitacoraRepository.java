package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.RegistroBitacora;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de RegistroBitacora. Spring Data genera la implementación. */
public interface BitacoraRepository extends JpaRepository<RegistroBitacora, Long> {

    List<RegistroBitacora> findByPqrsIdOrderByFechaAsc(Long pqrsId);

    List<RegistroBitacora> findByUsuarioId(Long usuarioId);
}
