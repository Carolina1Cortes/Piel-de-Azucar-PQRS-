package co.edu.unal.apolo.dominio;

/** Valores permitidos para EstadoPqrs. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum EstadoPqrs {

    PENDIENTE("Pendiente"),
    EN_ATENCION("En atención"),
    ESCALADA("Escalada"),
    DELEGADA("Delegada"),
    RESUELTA("Resuelta");

    private final String etiqueta;

    EstadoPqrs(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
