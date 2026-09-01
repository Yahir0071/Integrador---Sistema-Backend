package pe.edu.utp.Grupo06.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo06.model.Categoria;
import pe.edu.utp.Grupo06.repository.CategoriaRepository;
import pe.edu.utp.Grupo06.service.ICategoriaService;
import pe.edu.utp.Grupo06.util.Validador;

import java.util.List;

@Service
public class CategoriaServiceImpl implements ICategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private Validador validador;

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarActivas() {
        return categoriaRepository.findByEstadoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + id));
    }

    @Override
    @Transactional
    public Categoria registrar(Categoria categoria) {
        validador.validar(categoria);
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public Categoria actualizar(Long id, Categoria categoria) {
        Categoria existente = buscarPorId(id);
        existente.setNombre(categoria.getNombre());
        existente.setDescripcion(categoria.getDescripcion());
        existente.setEstado(categoria.getEstado());
        validador.validar(existente);
        return categoriaRepository.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        categoria.setEstado(false); // Eliminación lógica
        categoriaRepository.save(categoria);
    }
}
