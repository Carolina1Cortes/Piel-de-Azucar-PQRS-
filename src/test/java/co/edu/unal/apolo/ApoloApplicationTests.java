package co.edu.unal.apolo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifica que el contexto completo arranca: esquema, datos de prueba, capas y vistas. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:apolo-test;DB_CLOSE_DELAY=-1")
class ApoloApplicationTests {

    @Test
    void elContextoArranca() {
    }
}
