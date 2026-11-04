package co.edu.unal.apolo.seguridad;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.edu.unal.apolo.dominio.Usuario;
import co.edu.unal.apolo.persistencia.UsuarioRepository;

/** Carga el usuario por correo para el inicio de sesión. Un operador bloqueado queda con la cuenta bloqueada. */
@Service
public class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con ese correo"));
        return User.withUsername(usuario.getCorreo())
                .password(usuario.getContrasenaHash())
                .roles(usuario.getRol().name())
                .accountLocked(usuario.isBloqueado())
                .build();
    }
}
