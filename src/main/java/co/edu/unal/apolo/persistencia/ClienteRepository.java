package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de Cliente. Spring Data genera la implementación. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCorreo(String correo);
}
