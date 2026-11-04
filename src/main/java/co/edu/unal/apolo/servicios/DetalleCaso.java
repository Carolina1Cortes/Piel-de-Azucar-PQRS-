package co.edu.unal.apolo.servicios;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.dominio.TipoAccion;

/**
 * Todo lo que necesita la pantalla de un caso, ya calculado dentro de la transacción
 * (la vista no toca relaciones perezosas ni decide reglas de negocio).
 *
 * @param criticoSinAsignar        caso crítico que nadie ha tomado: se muestra "Automatización pausada"
 * @param puedeTomar               el operador actual puede autoasignarse el caso
 * @param puedeResponder           el operador actual puede enviar una respuesta
 * @param motivoRespuestaBloqueada por qué no puede responder (nulo si puede)
 */
public record DetalleCaso(
        Pqrs pqrs,
        List<MensajeVista> mensajes,
        List<RegistroVista> bitacora,
        int diasHabilesRestantes,
        boolean criticoSinAsignar,
        boolean puedeTomar,
        boolean puedeResponder,
        String motivoRespuestaBloqueada) {

    /** Un mensaje de la conversación; {@code autor} es nulo para los correos de la clienta y las respuestas automáticas. */
    public record MensajeVista(LocalDateTime fecha, DireccionMensaje direccion, String asunto, String cuerpo,
                               boolean automatico, String autor) {
    }

    /** Una línea de la bitácora; {@code usuario} es nulo cuando la acción la hizo el sistema. */
    public record RegistroVista(LocalDateTime fecha, TipoAccion accion, String usuario, String motivo, String notas) {
    }
}
