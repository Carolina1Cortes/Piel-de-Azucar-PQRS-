package co.edu.unal.apolo.servicios;

/** Se lanza cuando una operación incumple una regla de negocio (por ejemplo, responder un caso crítico sin tomarlo). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
