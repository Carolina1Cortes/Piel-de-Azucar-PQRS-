package co.edu.unal.apolo.dominio;

/** Valores permitidos para Prioridad. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum Prioridad {

    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta"),
    CRITICA("Crítica");

    private final String etiqueta;

    Prioridad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
