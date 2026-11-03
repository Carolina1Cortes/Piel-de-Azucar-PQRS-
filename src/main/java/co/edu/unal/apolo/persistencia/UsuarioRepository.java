package co.edu.unal.apolo.persistencia;

import co.edu.unal.apolo.dominio.Rol;
import co.edu.unal.apolo.dominio.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de Usuario. Spring Data genera la implementación. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByRol(Rol rol);
}
