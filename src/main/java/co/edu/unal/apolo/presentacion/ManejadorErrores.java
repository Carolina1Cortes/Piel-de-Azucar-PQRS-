package co.edu.unal.apolo.presentacion;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import co.edu.unal.apolo.servicios.RecursoNoEncontradoException;

/** Convierte un recurso inexistente en una página 404 amable en lugar de un error del servidor. */
@ControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RecursoNoEncontradoException excepcion, Model model) {
        model.addAttribute("mensaje", excepcion.getMessage());
        return "no-encontrado";
    }
}
