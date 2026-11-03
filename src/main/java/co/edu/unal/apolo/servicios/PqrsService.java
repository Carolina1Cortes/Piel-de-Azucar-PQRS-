package co.edu.unal.apolo.servicios;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unal.apolo.dominio.Pqrs;
import co.edu.unal.apolo.persistencia.PqrsRepository;

/**
 * Casos de uso sobre PQRS. En este primer sprint solo ofrece consultas;
 * tomarCaso, resolver y cambiarEstado llegan con los diagramas de secuencia (CU-02 y CU-03).
 */
@Service
public class PqrsService {

    private final PqrsRepository pqrsRepository;

    public PqrsService(PqrsRepository pqrsRepository) {
        this.pqrsRepository = pqrsRepository;
    }

    @Transactional(readOnly = true)
    public List<Pqrs> listarConDetalle() {
        return pqrsRepository.findAllConDetalle();
    }

    @Transactional(readOnly = true)
    public Pqrs buscarPorId(Long id) {
        return pqrsRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("PQRS", id));
    }
}
