package co.edu.unal.apolo.integracion;

/**
 * Puerto de salida hacia el lugar donde se guardan los archivos adjuntos.
 * Implementación: AlmacenArchivosLocal (carpeta del equipo, dentro de data/).
 */
public interface AlmacenArchivos {

    /**
     * Guarda el contenido en {@code carpeta/nombreArchivo} y devuelve esa ruta relativa, que es la que se guarda
     * en {@code adjunto.ruta}. Lanza {@link java.io.UncheckedIOException} si no se puede escribir.
     */
    String guardar(String carpeta, String nombreArchivo, byte[] contenido);
}
