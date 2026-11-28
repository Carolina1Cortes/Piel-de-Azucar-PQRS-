package co.edu.unal.apolo.integracion;

import java.util.List;

import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.Mensaje;

/**
 * Puerto de salida hacia el canal por el que se contesta a la persona cliente.
 * Implementaciones: GmailCanal (real, por SMTP) y CanalSimulado (sin internet).
 */
public interface CanalMensajeria {

    /** Envía el mensaje saliente, con sus adjuntos (JPG, PNG o PDF), a la persona cliente de la PQRS asociada. */
    void enviar(Mensaje mensaje, List<Adjunto> adjuntos);

    /** Envía el mensaje saliente sin adjuntos. */
    default void enviar(Mensaje mensaje) {
        enviar(mensaje, List.of());
    }
}
