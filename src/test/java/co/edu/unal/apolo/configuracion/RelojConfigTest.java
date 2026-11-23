package co.edu.unal.apolo.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class RelojConfigTest {

    @Test
    void elRelojDeLaAplicacionUsaLaHoraDeBogota() {
        Clock reloj = new RelojConfig().reloj();

        assertThat(reloj.getZone()).isEqualTo(ZoneId.of("America/Bogota"));
    }
}
