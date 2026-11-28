package co.edu.unal.apolo.integracion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Guarda los adjuntos en una carpeta local. Por defecto es {@code data/}, que ya está en el .gitignore,
 * así que los archivos nunca se suben al repositorio.
 */
@Component
public class AlmacenArchivosLocal implements AlmacenArchivos {

    private final Path base;

    public AlmacenArchivosLocal(@Value("${apolo.adjuntos.directorio:data}") String directorio) {
        this.base = Paths.get(directorio).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String nombreArchivo, byte[] contenido) {
        Path destino = base.resolve(carpeta).resolve(nombreArchivo).normalize();
        if (!destino.startsWith(base)) {
            throw new IllegalArgumentException("Ruta de adjunto no permitida: " + carpeta + "/" + nombreArchivo);
        }
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el adjunto " + nombreArchivo, e);
        }
        return base.relativize(destino).toString().replace('\\', '/');
    }
}
