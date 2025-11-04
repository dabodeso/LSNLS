package com.lsnls.service;

import com.lsnls.entity.SubtemaPregunta;
import com.lsnls.entity.TematicaCombo;
import com.lsnls.entity.TematicaPregunta;
import com.lsnls.repository.SubtemaPreguntaRepository;
import com.lsnls.repository.TematicaComboRepository;
import com.lsnls.repository.TematicaPreguntaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TemasCatalogService {

    private final TematicaPreguntaRepository tematicaPreguntaRepository;
    private final TematicaComboRepository tematicaComboRepository;
    private final SubtemaPreguntaRepository subtemaPreguntaRepository;
    private final AuthorizationService authorizationService;

    public List<String> obtenerTematicasPreguntas() {
        return tematicaPreguntaRepository.findAll().stream()
                .map(TematicaPregunta::getNombre)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> obtenerTematicasCombos() {
        return tematicaComboRepository.findAll().stream()
                .map(TematicaCombo::getNombre)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> obtenerSubtemasPreguntas() {
        return subtemaPreguntaRepository.findAll().stream()
                .map(SubtemaPregunta::getNombre)
                .sorted()
                .collect(Collectors.toList());
    }

    public String añadirTematicaPregunta(String nombre) {
        String normalizado = normalizar(nombre);
        if (tematicaPreguntaRepository.existsByNombre(normalizado)) return normalizado;
        TematicaPregunta t = new TematicaPregunta();
        t.setNombre(normalizado);
        t.setCreacionUsuario(authorizationService.getCurrentUser().orElse(null));
        tematicaPreguntaRepository.save(t);
        return normalizado;
    }

    public String añadirSubtemaPregunta(String nombre) {
        String normalizado = normalizar(nombre);
        if (subtemaPreguntaRepository.existsByNombre(normalizado)) return normalizado;
        SubtemaPregunta s = new SubtemaPregunta();
        s.setNombre(normalizado);
        s.setCreacionUsuario(authorizationService.getCurrentUser().orElse(null));
        subtemaPreguntaRepository.save(s);
        return normalizado;
    }

    public void eliminarTematicaPregunta(String nombre) {
        tematicaPreguntaRepository.findByNombre(normalizar(nombre))
                .ifPresent(tematicaPreguntaRepository::delete);
    }

    public void eliminarSubtemaPregunta(String nombre) {
        subtemaPreguntaRepository.findByNombre(normalizar(nombre))
                .ifPresent(subtemaPreguntaRepository::delete);
    }

    private String normalizar(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }
}


