package com.lsnls.service;

import com.lsnls.entity.Tematica;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.TematicaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@Transactional
public class TematicaService {

    @Autowired
    private TematicaRepository tematicaRepository;

    /**
     * Obtiene todas las temáticas ordenadas por nombre
     */
    public List<Tematica> obtenerTodas() {
        return tematicaRepository.findAllByOrderByNombreAsc();
    }

    /**
     * Busca una temática por nombre (case insensitive)
     */
    public Optional<Tematica> buscarPorNombre(String nombre) {
        return tematicaRepository.findByNombreIgnoreCase(nombre);
    }

    /**
     * Crea una nueva temática si no existe
     */
    public Tematica crearTematica(String nombre, Usuario usuario) {
        // Verificar si ya existe
        if (tematicaRepository.existsByNombreIgnoreCase(nombre)) {
            return tematicaRepository.findByNombreIgnoreCase(nombre).orElse(null);
        }
        
        // Crear nueva temática
        Tematica tematica = new Tematica(nombre, usuario);
        return tematicaRepository.save(tematica);
    }

    /**
     * Busca temáticas que contengan un texto específico
     */
    public List<Tematica> buscarPorTexto(String texto) {
        return tematicaRepository.findByNombreContainingIgnoreCase(texto);
    }

    /**
     * Obtiene una temática por ID
     */
    public Optional<Tematica> obtenerPorId(Long id) {
        return tematicaRepository.findById(id);
    }

    /**
     * Actualiza el nombre de una temática
     */
    public Tematica actualizarTematica(Long id, String nuevoNombre) {
        Optional<Tematica> tematicaOpt = tematicaRepository.findById(id);
        if (tematicaOpt.isPresent()) {
            Tematica tematica = tematicaOpt.get();
            
            // Verificar si el nuevo nombre ya existe (excluyendo la temática actual)
            if (!nuevoNombre.equalsIgnoreCase(tematica.getNombre()) && 
                tematicaRepository.existsByNombreIgnoreCase(nuevoNombre)) {
                throw new IllegalArgumentException("Ya existe una temática con el nombre: " + nuevoNombre);
            }
            
            tematica.setNombre(nuevoNombre);
            return tematicaRepository.save(tematica);
        }
        return null;
    }

    /**
     * Elimina una temática por ID
     */
    public boolean eliminarTematica(Long id) {
        if (tematicaRepository.existsById(id)) {
            tematicaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Elimina una temática por nombre
     */
    public boolean eliminarTematica(String nombre) {
        Optional<Tematica> tematicaOpt = tematicaRepository.findByNombreIgnoreCase(nombre);
        if (tematicaOpt.isPresent()) {
            tematicaRepository.delete(tematicaOpt.get());
            return true;
        }
        return false;
    }

    /**
     * Obtiene solo los nombres de las temáticas como lista de strings
     */
    public List<String> obtenerNombresTematicas() {
        return tematicaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(tematica -> tematica.getNombre())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Añade una nueva temática (alias para crearTematica)
     */
    public Tematica añadirTematica(String nombre, Usuario usuario) {
        return crearTematica(nombre, usuario);
    }

    /**
     * Obtiene estadísticas de las temáticas
     */
    public Map<String, Object> obtenerEstadisticas() {
        List<Tematica> tematicas = tematicaRepository.findAllByOrderByNombreAsc();
        Map<String, Object> estadisticas = new java.util.HashMap<>();
        estadisticas.put("totalTematicas", tematicas.size());
        estadisticas.put("tematicas", tematicas);
        return estadisticas;
    }
}
