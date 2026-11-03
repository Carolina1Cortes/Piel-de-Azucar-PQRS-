package co.edu.unal.apolo.dominio;

/** Valores permitidos para TipoAccion. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum TipoAccion {

    CASO_TOMADO("Caso tomado"),
    RESPUESTA_ENVIADA("Respuesta enviada"),
    ESCALADA("Escalada"),
    DELEGADA("Delegada"),
    PRIORIDAD_CAMBIADA("Prioridad cambiada"),
    OPERADOR_BLOQUEADO("Operador bloqueado"),
    CASO_RESUELTO("Caso resuelto");

    private final String etiqueta;

    TipoAccion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
