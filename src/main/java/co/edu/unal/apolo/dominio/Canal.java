package co.edu.unal.apolo.dominio;

/** Valores permitidos para Canal. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum Canal {

    GMAIL("Gmail");

    private final String etiqueta;

    Canal(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
