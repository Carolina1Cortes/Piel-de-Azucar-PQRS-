package co.edu.unal.apolo.integracion;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.Mensaje;

/**
 * Canal de prueba: no sale a internet, solo deja el envío en el log.
 * Permite que la aplicación sea útil sin credenciales de Gmail.
 * La selección entre este canal y GmailCanal se hará por configuración en un sprint posterior.
 */
@Component
public class CanalSimulado implements CanalMensajeria {

    private static final Logger LOG = LoggerFactory.getLogger(CanalSimulado.class);

    @Override
    public void enviar(Mensaje mensaje, List<Adjunto> adjuntos) {
        LOG.info("[CANAL SIMULADO] Mensaje '{}' enviado a la persona cliente de la PQRS {} con {} adjunto(s) {}",
                mensaje.getAsunto(), mensaje.getPqrs().getId(), adjuntos.size(),
                adjuntos.stream().map(Adjunto::getNombreArchivo).toList());
    }
}
