package co.edu.unal.apolo.dominio;

/** Valores permitidos para TipoAutomatizacion. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum TipoAutomatizacion {

    AUTOMATICA("Automática"),
    ASISTIDA("Asistida"),
    ATENCION_HUMANA("Atención humana"),
    REGISTRO("Registro y clasificación");

    private final String etiqueta;

    TipoAutomatizacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
