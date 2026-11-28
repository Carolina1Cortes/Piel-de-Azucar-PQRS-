package co.edu.unal.apolo.configuracion;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra {@link AdjuntosPropiedades} para que los servicios reciban los límites de adjuntos por canal. */
@Configuration
@EnableConfigurationProperties(AdjuntosPropiedades.class)
public class AdjuntosConfig {
}
