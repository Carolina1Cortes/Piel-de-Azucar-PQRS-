package co.edu.unal.apolo.servicios;

/** Se lanza cuando se busca un registro por identificador y no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Object id) {
        super(recurso + " con identificador " + id + " no existe");
    }
}
