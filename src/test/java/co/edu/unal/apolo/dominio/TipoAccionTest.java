package co.edu.unal.apolo.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class TipoAccionTest {

    @Test
    void soloResolverEscalarYLaNotaInternaExigenNota() {
        assertThat(Arrays.stream(TipoAccion.values()).filter(TipoAccion::exigeNotaInterna))
                .containsExactlyInAnyOrder(TipoAccion.CASO_RESUELTO, TipoAccion.ESCALADA, TipoAccion.NOTA_INTERNA);
    }

    @Test
    void tomarResponderYCompletarPasosNoExigenNota() {
        assertThat(TipoAccion.CASO_TOMADO.exigeNotaInterna()).isFalse();
        assertThat(TipoAccion.RESPUESTA_ENVIADA.exigeNotaInterna()).isFalse();
        assertThat(TipoAccion.PASO_PROTOCOLO_COMPLETADO.exigeNotaInterna()).isFalse();
    }

    @Test
    void laNotaInternaTieneEtiquetaParaLasVistas() {
        assertThat(TipoAccion.NOTA_INTERNA.getEtiqueta()).isEqualTo("Nota interna");
    }
}
