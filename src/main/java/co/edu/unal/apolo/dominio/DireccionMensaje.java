package co.edu.unal.apolo.dominio;

/** Valores permitidos para DireccionMensaje. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum DireccionMensaje {

    ENTRANTE("Entrante"),
    SALIENTE("Saliente");

    private final String etiqueta;

    DireccionMensaje(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
