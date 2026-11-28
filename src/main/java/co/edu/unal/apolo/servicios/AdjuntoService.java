package co.edu.unal.apolo.servicios;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.configuracion.AdjuntosPropiedades;
import co.edu.unal.apolo.dominio.Adjunto;
import co.edu.unal.apolo.dominio.Canal;
import co.edu.unal.apolo.dominio.Mensaje;
import co.edu.unal.apolo.dominio.TipoAdjunto;
import co.edu.unal.apolo.integracion.AlmacenArchivos;
import co.edu.unal.apolo.persistencia.AdjuntoRepository;

/**
 * Adjuntos de las respuestas (CU-02-HU-02): valida que sean JPG, PNG o PDF y que no superen el tamaño máximo
 * configurado para el canal, los guarda y los liga al mensaje saliente.
 */
@Service
public class AdjuntoService {

    /** Cantidad máxima de archivos en un solo mensaje. */
    static final int MAXIMO_ADJUNTOS = 5;
    /** Igual al largo de {@code adjunto.nombre_archivo}. */
    private static final int LARGO_MAXIMO_NOMBRE = 255;
    static final String CARPETA_MENSAJES = "adjuntos/mensajes";
    private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");

    private final AdjuntoRepository adjuntoRepository;
    private final AlmacenArchivos almacen;
    private final AdjuntosPropiedades propiedades;

    public AdjuntoService(AdjuntoRepository adjuntoRepository, AlmacenArchivos almacen,
                          AdjuntosPropiedades propiedades) {
        this.adjuntoRepository = adjuntoRepository;
        this.almacen = almacen;
        this.propiedades = propiedades;
    }

    /**
     * Comprueba los adjuntos antes de enviar: como máximo {@value #MAXIMO_ADJUNTOS} archivos, ninguno vacío,
     * solo JPG, PNG o PDF y que juntos no superen el tamaño máximo del canal.
     * Lanza {@link ReglaNegocioException} con el primer problema que encuentre. Sin adjuntos no hace nada.
     */
    public void validar(List<ArchivoAdjunto> archivos, Canal canal) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }
        if (archivos.size() > MAXIMO_ADJUNTOS) {
            throw new ReglaNegocioException("Puedes adjuntar como máximo " + MAXIMO_ADJUNTOS + " archivos por mensaje");
        }
        long maximo = propiedades.tamanoMaximoPara(canal).toBytes();
        String nombreCanal = canal == null ? "este canal" : canal.getEtiqueta();
        long total = 0;
        for (ArchivoAdjunto archivo : archivos) {
            String nombre = nombreLimpio(archivo.nombre());
            if (archivo.tamano() == 0) {
                throw new ReglaNegocioException("El archivo " + nombre + " está vacío");
            }
            if (TipoAdjunto.detectar(nombre, archivo.contenido()).isEmpty()) {
                throw new ReglaNegocioException("Solo se pueden adjuntar archivos JPG, PNG o PDF: " + nombre
                        + " no es válido");
            }
            if (archivo.tamano() > maximo) {
                throw new ReglaNegocioException("El archivo " + nombre + " pesa " + legible(archivo.tamano())
                        + " y el máximo para " + nombreCanal + " es " + legible(maximo));
            }
            total += archivo.tamano();
        }
        if (total > maximo) {
            throw new ReglaNegocioException("Los adjuntos suman " + legible(total) + " y el máximo para "
                    + nombreCanal + " es " + legible(maximo));
        }
    }

    /**
     * Guarda cada archivo con un nombre único (para que no se pisen ni se use el nombre del usuario en disco)
     * y crea su registro en {@code adjunto} ligado al mensaje. Llama antes a {@link #validar}.
     */
    @Transactional
    public List<Adjunto> guardar(Mensaje mensaje, List<ArchivoAdjunto> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return List.of();
        }
        List<Adjunto> guardados = new ArrayList<>();
        for (ArchivoAdjunto archivo : archivos) {
            String nombre = nombreLimpio(archivo.nombre());
            TipoAdjunto tipo = TipoAdjunto.detectar(nombre, archivo.contenido())
                    .orElseThrow(() -> new ReglaNegocioException("Solo se pueden adjuntar archivos JPG, PNG o PDF"));
            String nombreEnDisco = UUID.randomUUID() + "." + tipo.getExtension();
            String ruta;
            try {
                ruta = almacen.guardar(CARPETA_MENSAJES, nombreEnDisco, archivo.contenido());
            } catch (UncheckedIOException e) {
                throw new ReglaNegocioException("No se pudo guardar el archivo " + nombre + ". Intenta de nuevo.");
            }
            Adjunto adjunto = new Adjunto();
            adjunto.setNombreArchivo(nombre);
            adjunto.setRuta(ruta);
            adjunto.setTipoMime(tipo.getTipoMime());
            adjunto.setMensaje(mensaje);
            guardados.add(adjuntoRepository.save(adjunto));
        }
        return guardados;
    }

    /** Nombres de los adjuntos de cada mensaje de un caso, agrupados por id del mensaje. */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> nombresPorMensaje(Long pqrsId) {
        return adjuntoRepository.findByMensajePqrsId(pqrsId).stream()
                .collect(Collectors.groupingBy(adjunto -> adjunto.getMensaje().getId(),
                        Collectors.mapping(Adjunto::getNombreArchivo, Collectors.toList())));
    }

    /** Tamaño máximo de los adjuntos del canal, listo para mostrar (por ejemplo, "10 MB"). */
    public String tamanoMaximoLegible(Canal canal) {
        return legible(propiedades.tamanoMaximoPara(canal).toBytes());
    }

    /** Quita la ruta que algunos navegadores envían con el nombre (C:\...\guia.pdf) y lo recorta al largo de la columna. */
    static String nombreLimpio(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "sin_nombre";
        }
        String soloNombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1).strip();
        if (soloNombre.isEmpty()) {
            return "sin_nombre";
        }
        return soloNombre.length() <= LARGO_MAXIMO_NOMBRE
                ? soloNombre
                : soloNombre.substring(soloNombre.length() - LARGO_MAXIMO_NOMBRE);
    }

    static String legible(long bytes) {
        long megabyte = 1024L * 1024L;
        if (bytes >= megabyte) {
            if (bytes % megabyte == 0) {
                return (bytes / megabyte) + " MB";
            }
            return String.format(ESPANOL, "%.1f MB", bytes / (double) megabyte);
        }
        return Math.max(1, (bytes + 1023) / 1024) + " KB";
    }
}
