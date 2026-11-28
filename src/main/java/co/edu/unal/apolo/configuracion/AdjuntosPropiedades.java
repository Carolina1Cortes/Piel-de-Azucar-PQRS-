package co.edu.unal.apolo.configuracion;

import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import co.edu.unal.apolo.dominio.Canal;

/**
 * Configuración de los adjuntos (CU-02-HU-02), leída de {@code application.properties}:
 * <pre>apolo.adjuntos.tamano-maximo.gmail=10MB</pre>
 * El tamaño máximo es la suma de todos los adjuntos de un mensaje y se define por canal (la clave es el nombre del
 * canal en minúsculas). Si un canal no tiene valor, se usa {@link #TAMANO_POR_DEFECTO}.
 */
@ConfigurationProperties(prefix = "apolo.adjuntos")
public record AdjuntosPropiedades(Map<String, DataSize> tamanoMaximo) {

    public static final DataSize TAMANO_POR_DEFECTO = DataSize.ofMegabytes(5);

    public AdjuntosPropiedades {
        tamanoMaximo = tamanoMaximo == null ? Map.of() : Map.copyOf(tamanoMaximo);
    }

    /** Tamaño máximo de los adjuntos de un mensaje que sale por el canal indicado. */
    public DataSize tamanoMaximoPara(Canal canal) {
        if (canal == null) {
            return TAMANO_POR_DEFECTO;
        }
        return tamanoMaximo.getOrDefault(canal.name().toLowerCase(Locale.ROOT), TAMANO_POR_DEFECTO);
    }
}
