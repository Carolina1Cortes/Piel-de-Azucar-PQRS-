package co.edu.unal.apolo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de APOLO: aplicación web monolítica de gestión de PQRS para Piel de Azúcar. */
@SpringBootApplication
public class ApoloApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApoloApplication.class, args);
    }
}
