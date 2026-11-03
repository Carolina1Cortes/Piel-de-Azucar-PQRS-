package co.edu.unal.apolo.dominio;

/** Valores permitidos para Rol. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum Rol {

    OPERADOR("Operador"),
    ADMIN("Admin");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
