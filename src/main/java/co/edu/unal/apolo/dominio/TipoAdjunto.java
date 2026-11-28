package co.edu.unal.apolo.dominio;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Tipos de archivo que se pueden adjuntar a una respuesta (CU-02-HU-02): imágenes JPG y PNG y documentos PDF.
 * Un archivo es de un tipo solo si coinciden la extensión del nombre y la firma de sus primeros bytes,
 * para que un archivo renombrado (por ejemplo, un .exe llamado guia.pdf) no pase como permitido.
 */
public enum TipoAdjunto {

    JPG("image/jpeg", List.of("jpg", "jpeg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", List.of("png"), new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    PDF("application/pdf", List.of("pdf"), new byte[] {'%', 'P', 'D', 'F', '-'});

    private final String tipoMime;
    private final List<String> extensiones;
    private final byte[] firma;

    TipoAdjunto(String tipoMime, List<String> extensiones, byte[] firma) {
        this.tipoMime = tipoMime;
        this.extensiones = extensiones;
        this.firma = firma;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    /** Extensión con la que se guarda el archivo en disco. */
    public String getExtension() {
        return extensiones.get(0);
    }

    /** Detecta el tipo de un archivo por su nombre y su contenido; vacío si no es JPG, PNG ni PDF. */
    public static Optional<TipoAdjunto> detectar(String nombreArchivo, byte[] contenido) {
        String extension = extensionDe(nombreArchivo);
        return Arrays.stream(values())
                .filter(tipo -> tipo.extensiones.contains(extension))
                .filter(tipo -> tipo.empiezaConFirma(contenido))
                .findFirst();
    }

    private boolean empiezaConFirma(byte[] contenido) {
        if (contenido == null || contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extensionDe(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "";
        }
        int punto = nombreArchivo.lastIndexOf('.');
        return punto < 0 ? "" : nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT);
    }
}
