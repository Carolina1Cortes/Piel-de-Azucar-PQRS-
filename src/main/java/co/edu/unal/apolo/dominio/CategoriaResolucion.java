package co.edu.unal.apolo.dominio;

/** Cómo terminó un caso. Se guarda como texto (VARCHAR con CHECK) en la columna categoria_resolucion de pqrs. */
public enum CategoriaResolucion {

    SOLUCIONADO("Solucionado"),
    INFORMACION_BRINDADA("Información brindada"),
    COMPENSACION_APLICADA("Compensación aplicada (cambio, reposición o reembolso)"),
    NO_PROCEDE("No procede"),
    DUPLICADO("Caso duplicado"),
    SIN_RESPUESTA("Sin respuesta de la persona cliente");

    private final String etiqueta;

    CategoriaResolucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
