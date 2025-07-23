package com.lsnls.service;

import com.lsnls.entity.Tematica;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.TematicaRepository;
import com.lsnls.repository.CuestionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class TematicaService {
    
    @Autowired
    private TematicaRepository tematicaRepository;
    
    @Autowired
    private CuestionarioRepository cuestionarioRepository;
    
    public List<Tematica> obtenerTematicasDisponibles() {
        return tematicaRepository.findAllByOrderByNombreAsc();
    }
    
    public List<String> obtenerNombresTematicas() {
        return tematicaRepository.findAllByOrderByNombreAsc()
                .stream()
                .map(Tematica::getNombre)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Tematica añadirTematica(String nombre, Usuario usuario) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la temática no puede estar vacío");
        }
        
        String nombreLimpio = nombre.trim();
        
        if (tematicaRepository.existsByNombre(nombreLimpio)) {
            throw new RuntimeException("La temática '" + nombreLimpio + "' ya existe");
        }
        
        Tematica tematica = new Tematica(nombreLimpio, usuario);
        return tematicaRepository.save(tematica);
    }
    
    @Transactional
    public void eliminarTematica(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la temática no puede estar vacío");
        }
        
        String nombreLimpio = nombre.trim();
        
        // Verificar si hay cuestionarios usando esta temática
        long count = cuestionarioRepository.countByTematica(nombreLimpio);
        if (count > 0) {
            throw new RuntimeException("No se puede eliminar la temática '" + nombreLimpio + "' porque hay " + count + " cuestionario(s) que la utilizan");
        }
        
        Tematica tematica = tematicaRepository.findByNombre(nombreLimpio)
                .orElseThrow(() -> new RuntimeException("La temática '" + nombreLimpio + "' no existe"));
        
        tematicaRepository.delete(tematica);
    }
    
    public Map<String, Object> obtenerEstadisticas() {
        List<Tematica> tematicas = obtenerTematicasDisponibles();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTematicas", tematicas.size());
        return stats;
    }
    
    public boolean existeTematica(String nombre) {
        return tematicaRepository.existsByNombre(nombre);
    }
} 