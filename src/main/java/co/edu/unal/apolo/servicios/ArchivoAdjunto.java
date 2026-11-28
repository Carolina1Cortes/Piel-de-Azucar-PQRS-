package co.edu.unal.apolo.servicios;

/**
 * Un archivo que el operador quiere adjuntar a una respuesta, tal como llegó del formulario.
 * Desacopla el servicio de las clases web (MultipartFile) para respetar las capas.
 *
 * @param nombre    nombre original del archivo
 * @param contenido bytes del archivo
 */
public record ArchivoAdjunto(String nombre, byte[] contenido) {

    public long tamano() {
        return contenido == null ? 0 : contenido.length;
    }
}
