package co.edu.unal.apolo.servicios;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;

/**
 * Clasificación automática por palabras clave (E5-07).
 *
 * <p>Compara el asunto y el cuerpo de un correo con las palabras clave de cada tipo de consulta
 * ({@code tipo_consulta.palabras_clave}, separadas por coma). Reglas:
 * <ul>
 *   <li>Se ignoran mayúsculas, tildes y signos de puntuación.</li>
 *   <li>Una palabra clave coincide si aparece al inicio de una palabra del texto: {@code enroj} coincide con
 *       "enrojecimiento" y "enrojecida"; una frase como {@code mi pedido} debe aparecer completa.</li>
 *   <li>Si coincide algún tipo de prioridad crítica, se elige entre los críticos: ante la duda, el caso se trata
 *       como crítico.</li>
 *   <li>Si no, gana el tipo con más palabras clave distintas; el empate lo resuelve la mayor prioridad y después
 *       el id menor.</li>
 *   <li>Si nada coincide, el caso queda en el tipo de la categoría {@link Categoria#POR_CLASIFICAR} para que una
 *       persona lo clasifique a mano.</li>
 * </ul>
 */
@Service
public class ClasificacionService {

    private final TipoConsultaRepository tipoConsultaRepository;

    public ClasificacionService(TipoConsultaRepository tipoConsultaRepository) {
        this.tipoConsultaRepository = tipoConsultaRepository;
    }

    /** Tipo de consulta del correo; si ninguna palabra clave coincide, el tipo "Por clasificar". */
    @Transactional(readOnly = true)
    public TipoConsulta clasificar(String asunto, String cuerpo) {
        return buscarPorPalabrasClave(asunto, cuerpo)
                .orElseGet(() -> tipoConsultaRepository.findByCategoria(Categoria.POR_CLASIFICAR).stream()
                        .findFirst()
                        .orElseThrow(() -> new ReglaNegocioException(
                                "No existe el tipo de consulta para casos por clasificar")));
    }

    /** Tipo de consulta que mejor coincide con el correo, o vacío si ninguna palabra clave coincide. */
    @Transactional(readOnly = true)
    public Optional<TipoConsulta> buscarPorPalabrasClave(String asunto, String cuerpo) {
        String texto = " " + normalizar((asunto == null ? "" : asunto) + " " + (cuerpo == null ? "" : cuerpo)) + " ";
        List<Coincidencia> coincidencias = tipoConsultaRepository.findAll().stream()
                .filter(tipo -> tipo.getCategoria() != Categoria.POR_CLASIFICAR)
                .map(tipo -> new Coincidencia(tipo, contarCoincidencias(tipo.getPalabrasClave(), texto)))
                .filter(c -> c.palabras() > 0)
                .toList();

        List<Coincidencia> criticas = coincidencias.stream()
                .filter(c -> c.tipo().getPrioridadPorDefecto() == Prioridad.CRITICA)
                .toList();

        return (criticas.isEmpty() ? coincidencias : criticas).stream()
                .max(Comparator.comparingInt(Coincidencia::palabras)
                        .thenComparing(c -> c.tipo().getPrioridadPorDefecto())
                        .thenComparing(c -> c.tipo().getId(), Comparator.reverseOrder()))
                .map(Coincidencia::tipo);
    }

    /** Cuántas palabras clave distintas aparecen en el texto ya normalizado y rodeado de espacios. */
    private int contarCoincidencias(String palabrasClave, String texto) {
        if (palabrasClave == null || palabrasClave.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(palabrasClave.split(","))
                .map(ClasificacionService::normalizar)
                .filter(palabra -> !palabra.isEmpty())
                .distinct()
                .filter(palabra -> texto.contains(" " + palabra))
                .count();
    }

    /** Minúsculas, sin tildes y con cualquier signo reemplazado por un solo espacio ("¿Cómo?" → "como"). */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinTildes.replaceAll("[^a-z0-9]+", " ").strip();
    }

    private record Coincidencia(TipoConsulta tipo, int palabras) {
    }
}
