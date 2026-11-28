package co.edu.unal.apolo.servicios;

import co.edu.unal.apolo.dominio.PlantillaRespuesta;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.persistencia.PlantillaRespuestaRepository;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas de negocio y operaciones CRUD para las plantillas de respuesta predeterminadas
 * (CU-05-HU-01). Los cambios persistidos quedan disponibles de inmediato para los agentes.
 */
@Service
public class PlantillaRespuestaService {

    private static final int LARGO_MAXIMO_NOMBRE = 150;
    private static final int LARGO_MAXIMO_ASUNTO = 200;
    private static final int LARGO_MAXIMO_CUERPO = 4000;
    private static final int LARGO_MAXIMO_PALABRAS_CLAVE = 500;
    private static final Pattern PATRON_VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final PlantillaRespuestaRepository plantillas;
    private final TipoConsultaRepository tiposConsulta;

    public PlantillaRespuestaService(
            PlantillaRespuestaRepository plantillas,
            TipoConsultaRepository tiposConsulta) {
        this.plantillas = plantillas;
        this.tiposConsulta = tiposConsulta;
    }

    @Transactional(readOnly = true)
    public List<PlantillaRespuesta> listarTodas() {
        return plantillas.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<PlantillaRespuesta> listarActivas() {
        return plantillas.findByActivaTrue();
    }

    @Transactional(readOnly = true)
    public List<PlantillaRespuesta> listarPorTipoConsulta(Long tipoConsultaId) {
        return plantillas.findByTipoConsultaId(tipoConsultaId);
    }

    @Transactional(readOnly = true)
    public PlantillaRespuesta buscarPorId(Long id) {
        return plantillas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plantilla de respuesta", id));
    }

    @Transactional
    public PlantillaRespuesta crear(
            String nombre,
            String asunto,
            String cuerpo,
            String palabrasClave,
            Long tipoConsultaId) {
        validarDatos(nombre, asunto, cuerpo, palabrasClave, tipoConsultaId);
        String nombreLimpio = nombre.trim();
        if (plantillas.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new ReglaNegocioException("Ya existe una plantilla con el titulo '" + nombreLimpio + "'.");
        }
        TipoConsulta tipoConsulta = buscarTipoConsulta(tipoConsultaId);

        PlantillaRespuesta plantilla = new PlantillaRespuesta();
        plantilla.setNombre(nombreLimpio);
        plantilla.setAsunto(asunto.trim());
        plantilla.setCuerpo(cuerpo.trim());
        plantilla.setPalabrasClave(limpiarOpcional(palabrasClave));
        plantilla.setTipoConsulta(tipoConsulta);
        plantilla.setActiva(true);
        return plantillas.save(plantilla);
    }

    @Transactional
    public PlantillaRespuesta actualizar(
            Long id,
            String nombre,
            String asunto,
            String cuerpo,
            String palabrasClave,
            Long tipoConsultaId,
            boolean activa) {
        PlantillaRespuesta plantilla = buscarPorId(id);
        validarDatos(nombre, asunto, cuerpo, palabrasClave, tipoConsultaId);
        String nombreLimpio = nombre.trim();
        if (plantillas.existsByNombreIgnoreCaseAndIdNot(nombreLimpio, id)) {
            throw new ReglaNegocioException("Ya existe otra plantilla con el titulo '" + nombreLimpio + "'.");
        }
        TipoConsulta tipoConsulta = buscarTipoConsulta(tipoConsultaId);

        plantilla.setNombre(nombreLimpio);
        plantilla.setAsunto(asunto.trim());
        plantilla.setCuerpo(cuerpo.trim());
        plantilla.setPalabrasClave(limpiarOpcional(palabrasClave));
        plantilla.setTipoConsulta(tipoConsulta);
        plantilla.setActiva(activa);
        return plantillas.save(plantilla);
    }

    @Transactional
    public void eliminar(Long id) {
        PlantillaRespuesta plantilla = buscarPorId(id);
        plantillas.delete(plantilla);
    }

    /** Extrae en orden de aparicion los nombres de variables dinamicas con formato {{variable}}. */
    public List<String> extraerVariablesDinamicas(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = PATRON_VARIABLE.matcher(texto);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return new ArrayList<>(variables);
    }

    /** Reemplaza las variables dinamicas {{variable}} usando el mapa de valores suministrado. */
    public String renderizarTexto(String texto, Map<String, String> valores) {
        if (texto == null || valores == null || valores.isEmpty()) {
            return texto;
        }
        String resultado = texto;
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            String valor = entrada.getValue() != null ? entrada.getValue() : "";
            resultado = resultado.replaceAll(
                    "\\{\\{\\s*" + Pattern.quote(entrada.getKey()) + "\\s*}}",
                    Matcher.quoteReplacement(valor));
        }
        return resultado;
    }

    private TipoConsulta buscarTipoConsulta(Long tipoConsultaId) {
        return tiposConsulta.findById(tipoConsultaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de consulta", tipoConsultaId));
    }

    private void validarDatos(
            String nombre,
            String asunto,
            String cuerpo,
            String palabrasClave,
            Long tipoConsultaId) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El titulo de la plantilla es obligatorio.");
        }
        if (nombre.trim().length() > LARGO_MAXIMO_NOMBRE) {
            throw new ReglaNegocioException("El titulo de la plantilla no puede superar 150 caracteres.");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new ReglaNegocioException("El asunto de la plantilla es obligatorio.");
        }
        if (asunto.trim().length() > LARGO_MAXIMO_ASUNTO) {
            throw new ReglaNegocioException("El asunto de la plantilla no puede superar 200 caracteres.");
        }
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new ReglaNegocioException("El cuerpo de la plantilla es obligatorio.");
        }
        if (cuerpo.trim().length() > LARGO_MAXIMO_CUERPO) {
            throw new ReglaNegocioException("El cuerpo de la plantilla no puede superar 4000 caracteres.");
        }
        if (palabrasClave != null && palabrasClave.trim().length() > LARGO_MAXIMO_PALABRAS_CLAVE) {
            throw new ReglaNegocioException("Las palabras clave no pueden superar 500 caracteres.");
        }
        if (tipoConsultaId == null) {
            throw new ReglaNegocioException("La categoria asociada a la plantilla es obligatoria.");
        }
    }

    private String limpiarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
