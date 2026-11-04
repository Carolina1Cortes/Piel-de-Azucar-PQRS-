package co.edu.unal.apolo.seguridad;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad mínima del Sprint 1: sin sesión no se entra a ninguna pantalla (salvo el login y los estilos).
 * Se usan {@code antMatcher} porque la consola H2 registra otro servlet y Spring Security no puede adivinar
 * si un patrón de texto es de Spring MVC.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(antMatcher("/login"), antMatcher("/css/**"), antMatcher("/error")).permitAll()
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(salida -> salida
                        .logoutSuccessUrl("/login?salio")
                        .permitAll())
                // La consola H2 (solo para la sustentación) manda formularios sin token y vive en un marco.
                .csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher("/h2-console/**")))
                .headers(cabeceras -> cabeceras.frameOptions(marco -> marco.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }
}
