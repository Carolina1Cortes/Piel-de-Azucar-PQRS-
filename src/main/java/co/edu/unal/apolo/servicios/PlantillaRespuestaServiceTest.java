package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.PlantillaRespuesta;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.persistencia.PlantillaRespuestaRepository;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlantillaRespuestaServiceTest {

    @Mock
    private PlantillaRespuestaRepository plantillas;

    @Mock
    private TipoConsultaRepository tiposConsulta;

    private PlantillaRespuestaService servicio;

    @BeforeEach
    void configurar() {
        servicio = new PlantillaRespuestaService(plantillas, tiposConsulta);
    }

    @Test
    void listaTodasLasPlantillasOrdenadasPorNombre() {
        PlantillaRespuesta p1 = crearPlantillaEjemplo(1L, "Bienvenida");
        PlantillaRespuesta p2 = crearPlantillaEjemplo(2L, "Guia de envio");
        when(plantillas.findAllByOrderByNombreAsc()).thenReturn(List.of(p1, p2));

        List<PlantillaRespuesta> resultado = servicio.listarTodas();

        assertThat(resultado).containsExactly(p1, p2);
    }

    @Test
    void listaSoloLasPlantillasActivasYPorTipoConsulta() {
        PlantillaRespuesta activa = crearPlantillaEjemplo(1L, "Seguimiento envio");
        when(plantillas.findByActivaTrue()).thenReturn(List.of(activa));
        when(plantillas.findByTipoConsultaId(10L)).thenReturn(List.of(activa));

        assertThat(servicio.listarActivas()).containsExactly(activa);
        assertThat(servicio.listarPorTipoConsulta(10L)).containsExactly(activa);
    }

    @Test
    void buscaPlantillaPorIdYLanzaExcepcionSiNoExiste() {
        PlantillaRespuesta existente = crearPlantillaEjemplo(5L, "Devolucion aprobada");
        when(plantillas.findById(5L)).thenReturn(Optional.of(existente));
        when(plantillas.findById(99L)).thenReturn(Optional.empty());

        assertThat(servicio.buscarPorId(5L)).isEqualTo(existente);
        assertThatThrownBy(() -> servicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void creaPlantillaActivaCuandoLosDatosSonValidos() {
        TipoConsulta tipo = crearTipoConsulta(10L);
        when(plantillas.existsByNombreIgnoreCase("Confirmacion de envio")).thenReturn(false);
        when(tiposConsulta.findById(10L)).thenReturn(Optional.of(tipo));
        when(plantillas.save(any(PlantillaRespuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        PlantillaRespuesta creada = servicio.crear(
                "  Confirmacion de envio  ",
                "  Tu pedido {{radicado}} va en camino  ",
                "  Hola {{nombreCliente}}, tu guia es {{numeroGuia}}.  ",
                "   ",
                10L);

        assertThat(creada.getNombre()).isEqualTo("Confirmacion de envio");
        assertThat(creada.getAsunto()).isEqualTo("Tu pedido {{radicado}} va en camino");
        assertThat(creada.getCuerpo()).isEqualTo("Hola {{nombreCliente}}, tu guia es {{numeroGuia}}.");
        assertThat(creada.getPalabrasClave()).isNull();
        assertThat(creada.getTipoConsulta()).isEqualTo(tipo);
        assertThat(creada.isActiva()).isTrue();
    }

    @Test
    void rechazaCreacionSiTituloEstaDuplicado() {
        when(plantillas.existsByNombreIgnoreCase("Confirmacion de envio")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(
                "Confirmacion de envio",
                "Asunto",
                "Cuerpo",
                "envio",
                10L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe una plantilla");

        verify(plantillas, never()).save(any());
    }

    @Test
    void rechazaCreacionCuandoFaltanCamposObligatoriosOSuperanLimite() {
        String textoLargo = "a".repeat(4001);

        assertThatThrownBy(() -> servicio.crear(" ", "Asunto", "Cuerpo", null, 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("a".repeat(151), "Asunto", "Cuerpo", null, 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("Titulo", "", "Cuerpo", null, 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("Titulo", "a".repeat(201), "Cuerpo", null, 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("Titulo", "Asunto", null, null, 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("Titulo", "Asunto", textoLargo, null, 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("Titulo", "Asunto", "Cuerpo", "a".repeat(501), 10L))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> servicio.crear("Titulo", "Asunto", "Cuerpo", null, null))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rechazaCreacionSiLaCategoriaNoExiste() {
        when(plantillas.existsByNombreIgnoreCase("Nueva plantilla")).thenReturn(false);
        when(tiposConsulta.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear("Nueva plantilla", "Asunto", "Cuerpo", "clave", 77L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizaPlantillaExistenteYReflejaCambiosDeInmediato() {
        PlantillaRespuesta existente = crearPlantillaEjemplo(3L, "Titulo anterior");
        TipoConsulta nuevoTipo = crearTipoConsulta(20L);
        when(plantillas.findById(3L)).thenReturn(Optional.of(existente));
        when(plantillas.existsByNombreIgnoreCaseAndIdNot("Titulo actualizado", 3L)).thenReturn(false);
        when(tiposConsulta.findById(20L)).thenReturn(Optional.of(nuevoTipo));
        when(plantillas.save(any(PlantillaRespuesta.class))).thenAnswer(inv -> inv.getArgument(0));

        PlantillaRespuesta actualizada = servicio.actualizar(
                3L,
                "Titulo actualizado",
                "Nuevo asunto",
                "Nuevo cuerpo {{cliente}}",
                "devolucion, cambio",
                20L,
                false);

        assertThat(actualizada.getNombre()).isEqualTo("Titulo actualizado");
        assertThat(actualizada.getAsunto()).isEqualTo("Nuevo asunto");
        assertThat(actualizada.getCuerpo()).isEqualTo("Nuevo cuerpo {{cliente}}");
        assertThat(actualizada.getPalabrasClave()).isEqualTo("devolucion, cambio");
        assertThat(actualizada.getTipoConsulta()).isEqualTo(nuevoTipo);
        assertThat(actualizada.isActiva()).isFalse();
    }

    @Test
    void rechazaActualizacionSiOtraPlantillaTieneElMismoTitulo() {
        PlantillaRespuesta existente = crearPlantillaEjemplo(3L, "Titulo anterior");
        when(plantillas.findById(3L)).thenReturn(Optional.of(existente));
        when(plantillas.existsByNombreIgnoreCaseAndIdNot("Duplicado", 3L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(3L, "Duplicado", "Asunto", "Cuerpo", null, 10L, true))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void eliminaPlantillaExistenteYLanzaExcepcionSiNoExiste() {
        PlantillaRespuesta existente = crearPlantillaEjemplo(4L, "Para borrar");
        when(plantillas.findById(4L)).thenReturn(Optional.of(existente));
        when(plantillas.findById(404L)).thenReturn(Optional.empty());

        servicio.eliminar(4L);
        verify(plantillas).delete(existente);

        assertThatThrownBy(() -> servicio.eliminar(404L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void extraeYRenderizaVariablesDinamicasEnElTexto() {
        String texto = "Hola {{nombreCliente}}, tu caso {{ radicado }} con {{nombreCliente}} fue enviado.";

        List<String> variables = servicio.extraerVariablesDinamicas(texto);
        assertThat(variables).containsExactly("nombreCliente", "radicado");
        assertThat(servicio.extraerVariablesDinamicas("   ")).isEmpty();

        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("nombreCliente", "Camila");
        valores.put("radicado", null);

        String renderizado = servicio.renderizarTexto(texto, valores);
        assertThat(renderizado).isEqualTo("Hola Camila, tu caso  con Camila fue enviado.");
        assertThat(servicio.renderizarTexto("Sin cambios", Map.of())).isEqualTo("Sin cambios");
    }

    private TipoConsulta crearTipoConsulta(Long id) {
        TipoConsulta tipo = new TipoConsulta();
        tipo.setId(id);
        tipo.setDescripcion("Seguimiento de envio");
        tipo.setCategoria(Categoria.ENVIOS_ENTREGAS);
        return tipo;
    }

    private PlantillaRespuesta crearPlantillaEjemplo(Long id, String nombre) {
        PlantillaRespuesta plantilla = new PlantillaRespuesta();
        plantilla.setId(id);
        plantilla.setNombre(nombre);
        plantilla.setAsunto("Asunto inicial");
        plantilla.setCuerpo("Cuerpo inicial");
        plantilla.setActiva(true);
        plantilla.setTipoConsulta(crearTipoConsulta(10L));
        return plantilla;
    }
}
