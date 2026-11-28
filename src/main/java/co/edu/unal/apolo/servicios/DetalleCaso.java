package co.edu.unal.apolo.servicios;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.unal.apolo.dominio.CategoriaResolucion;
import co.edu.unal.apolo.dominio.DireccionMensaje;
import co.edu.unal.apolo.dominio.PasoProtocolo;
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
 * @param protocolo                pasos del protocolo crítico (vacío si el caso no es crítico)
 * @param puedeCompletarPasos      el operador actual puede marcar pasos del protocolo
 * @param puedeResolver            el operador actual puede resolver el caso
 * @param motivoResolverBloqueado  por qué no puede resolverlo (nulo si puede)
 * @param resolucion               cómo se resolvió el caso (nulo mientras no esté resuelto)
 * @param motivoNotaBloqueada      por qué no puede agregar una nota interna (nulo si puede)
 * @param tamanoMaximoAdjuntos     tamaño máximo de los adjuntos en el canal del caso, listo para mostrar (nulo si no aplica)
 */
public record DetalleCaso(
        Pqrs pqrs,
        List<MensajeVista> mensajes,
        List<RegistroVista> bitacora,
        int diasHabilesRestantes,
        boolean criticoSinAsignar,
        boolean puedeTomar,
        boolean puedeResponder,
        String motivoRespuestaBloqueada,
        List<PasoVista> protocolo,
        boolean puedeCompletarPasos,
        boolean puedeResolver,
        String motivoResolverBloqueado,
        ResolucionVista resolucion,
        String motivoNotaBloqueada,
        String tamanoMaximoAdjuntos) {

    /** Sin información de adjuntos (se conserva para no romper a quien ya construía el detalle así). */
    public DetalleCaso(Pqrs pqrs, List<MensajeVista> mensajes, List<RegistroVista> bitacora, int diasHabilesRestantes,
                       boolean criticoSinAsignar, boolean puedeTomar, boolean puedeResponder,
                       String motivoRespuestaBloqueada, List<PasoVista> protocolo, boolean puedeCompletarPasos,
                       boolean puedeResolver, String motivoResolverBloqueado, ResolucionVista resolucion,
                       String motivoNotaBloqueada) {
        this(pqrs, mensajes, bitacora, diasHabilesRestantes, criticoSinAsignar, puedeTomar, puedeResponder,
                motivoRespuestaBloqueada, protocolo, puedeCompletarPasos, puedeResolver, motivoResolverBloqueado,
                resolucion, motivoNotaBloqueada, null);
    }

    /** El operador actual puede agregar una nota interna al caso. */
    public boolean puedeAgregarNota() {
        return motivoNotaBloqueada == null;
    }

    /**
     * Un mensaje de la conversación; {@code autor} es nulo para los correos de la persona cliente y las respuestas automáticas.
     * {@code adjuntos} son los nombres de los archivos adjuntos (vacío si no tiene).
     */
    public record MensajeVista(LocalDateTime fecha, DireccionMensaje direccion, String asunto, String cuerpo,
                               boolean automatico, String autor, List<String> adjuntos) {

        public MensajeVista {
            adjuntos = adjuntos == null ? List.of() : List.copyOf(adjuntos);
        }

        /** Mensaje sin adjuntos. */
        public MensajeVista(LocalDateTime fecha, DireccionMensaje direccion, String asunto, String cuerpo,
                            boolean automatico, String autor) {
            this(fecha, direccion, asunto, cuerpo, automatico, autor, List.of());
        }
    }

    /** Una línea de la bitácora; {@code usuario} es nulo cuando la acción la hizo el sistema. */
    public record RegistroVista(LocalDateTime fecha, TipoAccion accion, String usuario, String motivo, String notas) {
    }

    /**
     * La resolución de un caso resuelto; {@code categoria} es nula en los casos resueltos antes de existir las categorías
     * y {@code resueltaPor} es nulo si lo resolvió el sistema.
     */
    public record ResolucionVista(CategoriaResolucion categoria, String nota, LocalDateTime fecha, String resueltaPor) {
    }

    /** Un paso del protocolo crítico; {@code fecha} y {@code usuario} son nulos mientras no se complete. */
    public record PasoVista(PasoProtocolo paso, boolean completado, LocalDateTime fecha, String usuario) {
    }
}
