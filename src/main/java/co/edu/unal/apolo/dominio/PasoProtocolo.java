package co.edu.unal.apolo.dominio;

/** Pasos del protocolo de seguridad de un caso crítico, en el orden en que se muestran. */
public enum PasoProtocolo {

    SUSPENDER_USO("Pedir a la persona cliente que suspenda el uso del producto"),
    SOLICITAR_FOTOS("Solicitar fotos del producto y de la reacción"),
    SOLICITAR_LOTE("Solicitar el número de lote del producto"),
    RECOPILAR_SINTOMAS("Recopilar los síntomas y desde cuándo aparecieron");

    private final String etiqueta;

    PasoProtocolo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
