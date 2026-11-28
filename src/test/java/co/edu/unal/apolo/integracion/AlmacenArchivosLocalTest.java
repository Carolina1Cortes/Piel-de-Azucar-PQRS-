package co.edu.unal.apolo.integracion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlmacenArchivosLocalTest {

    @TempDir
    Path carpetaTemporal;

    @Test
    void guardaElArchivoEnLaCarpetaYDevuelveLaRutaRelativa() throws IOException {
        AlmacenArchivosLocal almacen = new AlmacenArchivosLocal(carpetaTemporal.toString());
        byte[] contenido = {'%', 'P', 'D', 'F', '-', '1'};

        String ruta = almacen.guardar("adjuntos/mensajes", "abc.pdf", contenido);

        assertThat(ruta).isEqualTo("adjuntos/mensajes/abc.pdf");
        assertThat(Files.readAllBytes(carpetaTemporal.resolve(ruta))).isEqualTo(contenido);
    }

    @Test
    void noPermiteEscribirFueraDeLaCarpetaBase() {
        AlmacenArchivosLocal almacen = new AlmacenArchivosLocal(carpetaTemporal.toString());

        assertThatThrownBy(() -> almacen.guardar("adjuntos", "../../fuera.pdf", new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
