package co.edu.unal.apolo.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unal.apolo.dominio.Categoria;
import co.edu.unal.apolo.dominio.Prioridad;
import co.edu.unal.apolo.dominio.TipoAutomatizacion;
import co.edu.unal.apolo.dominio.TipoConsulta;
import co.edu.unal.apolo.persistencia.TipoConsultaRepository;

@ExtendWith(MockitoExtension.class)
class ClasificacionServiceTest {

    @Mock private TipoConsultaRepository tipoConsultaRepository;

    private ClasificacionService servicio;
    private TipoConsulta estadoPedido;
    private TipoConsulta seguimiento;
    private TipoConsulta pedidoIncompleto;
    private TipoConsulta reaccion;
    private TipoConsulta cambio;
    private TipoConsulta sinTipo;
    private TipoConsulta porClasificar;

    @BeforeEach
    void preparar() {
        servicio = new ClasificacionService(tipoConsultaRepository);
        estadoPedido = tipo(1L, Categoria.PEDIDOS, Prioridad.MEDIA, "estado de mi pedido,mi pedido");
        seguimiento = tipo(3L, Categoria.ENVIOS_ENTREGAS, Prioridad.MEDIA, "cuando llega,no llega,envio");
        pedidoIncompleto = tipo(4L, Categoria.ENVIOS_ENTREGAS, Prioridad.ALTA, "incompleto,danado");
        reaccion = tipo(7L, Categoria.REACCIONES, Prioridad.CRITICA, "enroj,irrita,alerg,ardor");
        cambio = tipo(9L, Categoria.CAMBIOS_DEVOLUCIONES, Prioridad.ALTA, "cambio,devol");
        sinTipo = tipo(14L, Categoria.ATENCION_QUEJAS, Prioridad.BAJA, null);
        porClasificar = tipo(17L, Categoria.POR_CLASIFICAR, Prioridad.MEDIA, "pedido");
        lenient().when(tipoConsultaRepository.findAll()).thenReturn(
                List.of(estadoPedido, seguimiento, pedidoIncompleto, reaccion, cambio, sinTipo, porClasificar));
    }

    @Test
    void clasificaComoCriticoUnCorreoConEnrojecimiento() {
        TipoConsulta tipo = servicio.clasificar("Me salió enrojecimiento con el sérum",
                "Hola, desde que lo uso me salió enrojecimiento y ardor en las mejillas.");

        assertThat(tipo).isSameAs(reaccion);
        assertThat(tipo.getPrioridadPorDefecto()).isEqualTo(Prioridad.CRITICA);
    }

    @Test
    void ignoraMayusculasTildesYSignosDePuntuacion() {
        assertThat(servicio.buscarPorPalabrasClave("¿CUÁNDO LLEGA?", "Gracias.")).contains(seguimiento);
        assertThat(servicio.buscarPorPalabrasClave("Envío", "")).contains(seguimiento);
    }

    @Test
    void laPalabraClaveCoincideConElInicioDeLaPalabraDelTexto() {
        assertThat(servicio.buscarPorPalabrasClave("Mi piel está enrojecida", "")).contains(reaccion);
    }

    @Test
    void laPalabraClaveNoCoincideEnMitadDeOtraPalabra() {
        // "cambio" no debe encontrarse dentro de "intercambio"; "ardor" no dentro de "recardor".
        assertThat(servicio.buscarPorPalabrasClave("intercambio de recardor", "")).isEmpty();
    }

    @Test
    void unaFraseClaveDebeAparecerCompleta() {
        assertThat(servicio.buscarPorPalabrasClave("Mi pedido", "")).contains(estadoPedido);
        assertThat(servicio.buscarPorPalabrasClave("Mi buen pedido", "")).isEmpty();
    }

    @Test
    void gananLosTiposConMasPalabrasClaveCoincidentes() {
        TipoConsulta tipo = servicio.clasificar("¿Cuándo llega mi pedido?", "Todavía no llega.");

        assertThat(tipo).isSameAs(seguimiento);
    }

    @Test
    void elEmpateSeResuelvePorMayorPrioridad() {
        TipoConsulta tipo = servicio.clasificar("Mi pedido llegó incompleto", "");

        assertThat(tipo).isSameAs(pedidoIncompleto);
    }

    @Test
    void elEmpateDePrioridadSeResuelvePorElIdMenor() {
        TipoConsulta otro = tipo(2L, Categoria.PEDIDOS, Prioridad.MEDIA, "mi pedido");
        when(tipoConsultaRepository.findAll()).thenReturn(List.of(seguimiento, otro, estadoPedido));

        assertThat(servicio.buscarPorPalabrasClave("Mi pedido", "")).contains(estadoPedido);
    }

    @Test
    void unTipoCriticoGanaAunqueOtroTipoTengaMasCoincidencias() {
        TipoConsulta tipo = servicio.clasificar("Quiero un cambio",
                "Pedí un cambio y una devolución porque me salió una alergia.");

        assertThat(tipo).isSameAs(reaccion);
    }

    @Test
    void usaElAsuntoAunqueElCuerpoNoCoincida() {
        assertThat(servicio.buscarPorPalabrasClave("Alergia", "Hola")).contains(reaccion);
    }

    @Test
    void elCasoQuedaPorClasificarSiNingunaPalabraClaveCoincide() {
        when(tipoConsultaRepository.findByCategoria(Categoria.POR_CLASIFICAR)).thenReturn(List.of(porClasificar));

        assertThat(servicio.clasificar("Hola", "Buenas tardes, quisiera hablar con alguien")).isSameAs(porClasificar);
    }

    @Test
    void noClasificaPorElTipoPorClasificarNiPorTiposSinPalabrasClave() {
        assertThat(servicio.buscarPorPalabrasClave("pedido", "")).isEmpty();
    }

    @Test
    void aceptaAsuntoOCuerpoNulos() {
        assertThat(servicio.buscarPorPalabrasClave(null, null)).isEmpty();
        assertThat(servicio.buscarPorPalabrasClave(null, "alergia")).contains(reaccion);
    }

    @Test
    void fallaSiNoExisteElTipoPorClasificar() {
        when(tipoConsultaRepository.findByCategoria(Categoria.POR_CLASIFICAR)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.clasificar("Hola", "Hola"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("por clasificar");
    }

    @Test
    void normalizaTextoConTildesEnyesYSignos() {
        assertThat(ClasificacionService.normalizar("¿Cómo está el DAÑO, señora?")).isEqualTo("como esta el dano senora");
    }

    private TipoConsulta tipo(Long id, Categoria categoria, Prioridad prioridad, String palabrasClave) {
        TipoConsulta tipo = new TipoConsulta();
        tipo.setId(id);
        tipo.setDescripcion("Tipo " + id);
        tipo.setCategoria(categoria);
        tipo.setPrioridadPorDefecto(prioridad);
        tipo.setAutomatizacion(TipoAutomatizacion.ATENCION_HUMANA);
        tipo.setPalabrasClave(palabrasClave);
        return tipo;
    }
}
