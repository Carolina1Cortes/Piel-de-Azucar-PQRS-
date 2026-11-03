package co.edu.unal.apolo.integracion;

import co.edu.unal.apolo.dominio.Mensaje;

/**
 * Puerto de salida hacia el canal por el que se contesta a la clienta.
 * Implementaciones: GmailCanal (real, por SMTP) y CanalSimulado (sin internet).
 */
public interface CanalMensajeria {

    /** Envía el mensaje saliente a la clienta de la PQRS asociada. */
    void enviar(Mensaje mensaje);
}
