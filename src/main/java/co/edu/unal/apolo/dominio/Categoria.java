package co.edu.unal.apolo.dominio;

/** Valores permitidos para Categoria. Se guarda como texto (VARCHAR con CHECK) en la base de datos. */
public enum Categoria {

    PEDIDOS("Pedidos"),
    ENVIOS_ENTREGAS("Envíos y entregas"),
    PRODUCTOS_USO("Productos y modo de uso"),
    REACCIONES("Reacciones o molestias con productos"),
    CAMBIOS_DEVOLUCIONES("Cambios, devoluciones y garantías"),
    PAGOS_FACTURACION("Pagos y facturación"),
    ATENCION_QUEJAS("Atención al cliente y quejas"),
    SUGERENCIAS_FELICITACIONES("Sugerencias y felicitaciones");

    private final String etiqueta;

    Categoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /** Texto legible para mostrar en las vistas. */
    public String getEtiqueta() {
        return etiqueta;
    }
}
