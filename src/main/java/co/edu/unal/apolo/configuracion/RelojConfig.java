package co.edu.unal.apolo.configuracion;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj inyectable: los servicios lo usan para "hoy" y "ahora", y las pruebas lo reemplazan por uno fijo.
 * Usa siempre la hora de Bogotá, sin depender de la zona horaria del equipo (en un Codespace es UTC).
 */
@Configuration
public class RelojConfig {

    public static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_BOGOTA);
    }
}
