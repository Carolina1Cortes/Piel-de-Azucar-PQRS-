package co.edu.unal.apolo.dominio;

/** Valores permitidos para TipoAccion. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum TipoAccion {

    CASO_TOMADO("Caso tomado"),
    RESPUESTA_ENVIADA("Respuesta enviada"),
    ESCALADA("Escalada"),
    DELEGADA("Delegada"),
    PRIORIDAD_CAMBIADA("Prioridad cambiada"),
    OPERADOR_BLOQUEADO("Operador bloqueado"),
    CASO_RESUELTO("Caso resuelto"),
    PASO_PROTOCOLO_COMPLETADO("Paso del protocolo completado"),
    NOTA_INTERNA("Nota interna");

    private final String etiqueta;

    TipoAccion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Regla de negocio de las notas internas (E5-11): estas acciones no se pueden registrar sin una nota.
     * Es el único lugar donde el equipo define cuáles son; hoy: resolver (propuesta de la planeación), escalar
     * y la propia nota interna.
     */
    public boolean exigeNotaInterna() {
        return this == CASO_RESUELTO || this == ESCALADA || this == NOTA_INTERNA;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
