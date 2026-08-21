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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TemasCatalogService {

    private final TematicaPreguntaRepository tematicaPreguntaRepository;
    private final TematicaComboRepository tematicaComboRepository;
    private final SubtemaPreguntaRepository subtemaPreguntaRepository;
    private final AuthorizationService authorizationService;
    private final UndoService undoService;

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
        TematicaPregunta guardada = tematicaPreguntaRepository.save(t);
        if (guardada.getId() != null) {
            undoService.registrar("añadir_tematica_pregunta", "Añadir temática " + normalizado,
                    Collections.singletonList(UndoService.accionEliminarFila("tematicas_preguntas", guardada.getId())));
        }
        return normalizado;
    }

    public String añadirSubtemaPregunta(String nombre) {
        String normalizado = normalizar(nombre);
        if (subtemaPreguntaRepository.existsByNombre(normalizado)) return normalizado;
        SubtemaPregunta s = new SubtemaPregunta();
        s.setNombre(normalizado);
        s.setCreacionUsuario(authorizationService.getCurrentUser().orElse(null));
        SubtemaPregunta guardado = subtemaPreguntaRepository.save(s);
        if (guardado.getId() != null) {
            undoService.registrar("añadir_subtema_pregunta", "Añadir subtema " + normalizado,
                    Collections.singletonList(UndoService.accionEliminarFila("subtemas_preguntas", guardado.getId())));
        }
        return normalizado;
    }

    public void eliminarTematicaPregunta(String nombre) {
        tematicaPreguntaRepository.findByNombre(normalizar(nombre)).ifPresent(t -> {
            Map<String, Object> fila = t.getId() != null ? undoService.snapshotFila("tematicas_preguntas", t.getId()) : null;
            tematicaPreguntaRepository.delete(t);
            if (fila != null) {
                undoService.registrar("eliminar_tematica_pregunta", "Eliminar temática " + t.getNombre(),
                        Collections.singletonList(UndoService.accionInsertarFila("tematicas_preguntas", fila)));
            }
        });
    }

    public void eliminarSubtemaPregunta(String nombre) {
        subtemaPreguntaRepository.findByNombre(normalizar(nombre)).ifPresent(s -> {
            Map<String, Object> fila = s.getId() != null ? undoService.snapshotFila("subtemas_preguntas", s.getId()) : null;
            subtemaPreguntaRepository.delete(s);
            if (fila != null) {
                undoService.registrar("eliminar_subtema_pregunta", "Eliminar subtema " + s.getNombre(),
                        Collections.singletonList(UndoService.accionInsertarFila("subtemas_preguntas", fila)));
            }
        });
    }

    private String normalizar(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }
}


