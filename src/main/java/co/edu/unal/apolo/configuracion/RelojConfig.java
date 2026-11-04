package co.edu.unal.apolo.configuracion;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj inyectable: los servicios lo usan para "hoy" y "ahora", y las pruebas lo reemplazan por uno fijo. */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
