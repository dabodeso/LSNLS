package com.lsnls.service;

import com.lsnls.entity.HistorialJornada;
import com.lsnls.entity.HistorialJornada.EstadoAsignacion;
import com.lsnls.entity.HistorialJornada.TipoAsignacion;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Combo;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.repository.HistorialJornadaRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.dto.HistorialJornadaDTO;
import com.lsnls.dto.MarcarNoUsadoDTO;
import com.lsnls.dto.ReaprovecharComboDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class HistorialJornadaService {
    
    @Autowired
    private HistorialJornadaRepository historialRepository;
    
    @Autowired
    private CuestionarioRepository cuestionarioRepository;
    
    @Autowired
    private ComboRepository comboRepository;
    
    @Autowired
    private JornadaRepository jornadaRepository;
    
    @Autowired
    private PreguntaRepository preguntaRepository;

    @Autowired
    private UndoService undoService;

    /**
     * Registrar asignación de cuestionario a jornada
     */
    public HistorialJornada registrarAsignacionCuestionario(Long jornadaId, Long cuestionarioId) {
        Optional<Jornada> jornadaOpt = jornadaRepository.findById(jornadaId);
        Optional<Cuestionario> cuestionarioOpt = cuestionarioRepository.findById(cuestionarioId);
        
        if (jornadaOpt.isEmpty() || cuestionarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Jornada o cuestionario no encontrado");
        }
        
        HistorialJornada historial = new HistorialJornada();
        historial.setJornada(jornadaOpt.get());
        historial.setCuestionario(cuestionarioOpt.get());
        historial.setTipoAsignacion(TipoAsignacion.CUESTIONARIO);
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        
        return historialRepository.save(historial);
    }

    /**
     * Registrar asignación de combo a jornada
     */
    public HistorialJornada registrarAsignacionCombo(Long jornadaId, Long comboId) {
        Optional<Jornada> jornadaOpt = jornadaRepository.findById(jornadaId);
        Optional<Combo> comboOpt = comboRepository.findById(comboId);
        
        if (jornadaOpt.isEmpty() || comboOpt.isEmpty()) {
            throw new IllegalArgumentException("Jornada o combo no encontrado");
        }
        
        HistorialJornada historial = new HistorialJornada();
        historial.setJornada(jornadaOpt.get());
        historial.setCombo(comboOpt.get());
        historial.setTipoAsignacion(TipoAsignacion.COMBO);
        historial.setEstadoAsignacion(EstadoAsignacion.asignado);
        
        return historialRepository.save(historial);
    }

    /**
     * Marcar elementos como no usados en una jornada
     */
    public void marcarNoUsados(MarcarNoUsadoDTO dto) {
        // Marcar cuestionarios como no usados
        if (dto.getCuestionarioIds() != null) {
            for (Long cuestionarioId : dto.getCuestionarioIds()) {
                List<HistorialJornada> historiales = historialRepository.findByCuestionarioId(cuestionarioId);
                for (HistorialJornada historial : historiales) {
                    if (historial.getJornada().getId().equals(dto.getJornadaId())) {
                        historial.setEstadoAsignacion(EstadoAsignacion.no_usado);
                        historial.setNotas(dto.getMotivo());
                        historialRepository.save(historial);
                        
                        // Cambiar estado del cuestionario a aprobado
                        Cuestionario cuestionario = historial.getCuestionario();
                        cuestionario.setEstado(Cuestionario.EstadoCuestionario.aprobado);
                        cuestionarioRepository.save(cuestionario);
                    }
                }
            }
        }
        
        // Marcar combos como no usados
        if (dto.getComboIds() != null) {
            for (Long comboId : dto.getComboIds()) {
                List<HistorialJornada> historiales = historialRepository.findByComboId(comboId);
                for (HistorialJornada historial : historiales) {
                    if (historial.getJornada().getId().equals(dto.getJornadaId())) {
                        historial.setEstadoAsignacion(EstadoAsignacion.no_usado);
                        historial.setNotas(dto.getMotivo());
                        historialRepository.save(historial);
                        
                        // Cambiar estado del combo a aprobado
                        Combo combo = historial.getCombo();
                        combo.setEstado(Combo.EstadoCombo.aprobado);
                        comboRepository.save(combo);
                    }
                }
            }
        }
    }

    /**
     * Reaprovechar un combo
     */
    public Combo reaprovecharCombo(ReaprovecharComboDTO dto) {
        Optional<Combo> comboOriginalOpt = comboRepository.findById(dto.getComboOriginalId());
        if (comboOriginalOpt.isEmpty()) {
            throw new IllegalArgumentException("Combo original no encontrado");
        }
        
        Combo comboOriginal = comboOriginalOpt.get();
        List<java.util.Map<String, Object>> accionesUndo = new java.util.ArrayList<>();

        java.util.Map<String, Object> camposCombo = new java.util.LinkedHashMap<>();
        camposCombo.put("estado", comboOriginal.getEstado() != null ? comboOriginal.getEstado().name() : null);
        accionesUndo.add(UndoService.accionActualizarCampos("combos", comboOriginal.getId(), camposCombo));
        
        // Marcar combo original como reaprovechado
        comboOriginal.setEstado(Combo.EstadoCombo.reaprovechado);
        comboRepository.save(comboOriginal);
        
        // Crear nuevo combo
        Combo nuevoCombo = new Combo();
        nuevoCombo.setCreacionUsuario(comboOriginal.getCreacionUsuario());
        nuevoCombo.setNivel(comboOriginal.getNivel());
        nuevoCombo.setTipo(comboOriginal.getTipo());
        nuevoCombo.setEstado(Combo.EstadoCombo.borrador);
        
        Combo comboGuardado = comboRepository.save(nuevoCombo);
        if (comboGuardado.getId() != null) {
            accionesUndo.add(UndoService.accionEliminarFilas("combos_preguntas", "combo_id", comboGuardado.getId()));
            accionesUndo.add(UndoService.accionEliminarFila("combos", comboGuardado.getId()));
        }
        
        // Registrar pregunta usada en el historial
        List<HistorialJornada> historiales = historialRepository.findByComboId(dto.getComboOriginalId());
        for (HistorialJornada historial : historiales) {
            if (historial.getEstadoAsignacion() == EstadoAsignacion.asignado) {
                if (historial.getId() != null) {
                    java.util.Map<String, Object> camposHistorial = new java.util.LinkedHashMap<>();
                    camposHistorial.put("estado_asignacion", historial.getEstadoAsignacion().name());
                    camposHistorial.put("pregunta_usada_id", historial.getPreguntaUsadaId());
                    camposHistorial.put("fecha_uso", historial.getFechaUso() != null ? historial.getFechaUso().toString() : null);
                    camposHistorial.put("notas", historial.getNotas());
                    accionesUndo.add(UndoService.accionActualizarCampos("historial_jornadas", historial.getId(), camposHistorial));
                }
                historial.setEstadoAsignacion(EstadoAsignacion.usado);
                historial.setPreguntaUsadaId(dto.getPreguntaUsadaId());
                historial.setFechaUso(LocalDateTime.now());
                historial.setNotas("Combo reaprovechado - Pregunta usada: " + dto.getPreguntaUsadaId());
                historialRepository.save(historial);
                break;
            }
        }
        
        // Liberar preguntas no usadas
        if (dto.getPreguntasNoUsadasIds() != null) {
            for (Long preguntaId : dto.getPreguntasNoUsadasIds()) {
                Optional<Pregunta> preguntaOpt = preguntaRepository.findById(preguntaId);
                if (preguntaOpt.isPresent()) {
                    Pregunta pregunta = preguntaOpt.get();
                    java.util.Map<String, Object> camposPregunta = new java.util.LinkedHashMap<>();
                    camposPregunta.put("estado_disponibilidad",
                            pregunta.getEstadoDisponibilidad() != null ? pregunta.getEstadoDisponibilidad().name() : null);
                    accionesUndo.add(UndoService.accionActualizarCampos("preguntas", preguntaId, camposPregunta));
                    pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
                    preguntaRepository.save(pregunta);
                }
            }
        }

        undoService.registrar("reaprovechar_combo",
                "Reaprovechar combo " + dto.getComboOriginalId(), accionesUndo);
        
        return comboGuardado;
    }

    /**
     * Obtener historial de un cuestionario
     */
    public List<HistorialJornadaDTO> obtenerHistorialCuestionario(Long cuestionarioId) {
        List<HistorialJornada> historiales = historialRepository.findByCuestionarioId(cuestionarioId);
        return historiales.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtener historial de un combo
     */
    public List<HistorialJornadaDTO> obtenerHistorialCombo(Long comboId) {
        List<Long> cadena = construirCadenaReciclaje(comboId);
        List<HistorialJornada> historiales = new ArrayList<>();
        for (Long id : cadena) {
            historiales.addAll(listaSegura(historialRepository.findByComboId(id)));
        }
        historiales.sort(Comparator.comparing(HistorialJornada::getFechaAsignacion,
            Comparator.nullsLast(Comparator.naturalOrder())));
        return consolidarReciclajesCombo(historiales, cadena);
    }

    List<Long> construirCadenaReciclaje(Long comboId) {
        if (comboId == null) {
            return Collections.emptyList();
        }
        Long raiz = comboId;
        Set<Long> antiLoop = new HashSet<>();
        while (raiz != null && antiLoop.add(raiz)) {
            Long padre = buscarPadreReciclaje(raiz);
            if (padre == null) {
                break;
            }
            raiz = padre;
        }
        List<Long> cadena = new ArrayList<>();
        Deque<Long> cola = new ArrayDeque<>();
        Set<Long> vistos = new HashSet<>();
        cola.add(raiz);
        while (!cola.isEmpty()) {
            Long actual = cola.removeFirst();
            if (actual == null || !vistos.add(actual)) {
                continue;
            }
            cadena.add(actual);
            for (HistorialJornada hijo : hijosDeComboPadre(actual)) {
                if (hijo.getCombo() != null && hijo.getCombo().getId() != null) {
                    cola.add(hijo.getCombo().getId());
                }
            }
        }
        if (!cadena.contains(comboId)) {
            cadena.add(comboId);
        }
        return cadena;
    }

    private Long buscarPadreReciclaje(Long comboId) {
        for (HistorialJornada historial : historialRepository.findByComboId(comboId)) {
            Long padre = extraerComboPadreDesdeNotas(historial.getNotas());
            if (padre != null) {
                return padre;
            }
        }
        return null;
    }

    private List<HistorialJornadaDTO> consolidarReciclajesCombo(List<HistorialJornada> historiales, List<Long> cadena) {
        List<HistorialJornada> padres = new ArrayList<>();
        List<HistorialJornada> hijos = new ArrayList<>();
        List<HistorialJornada> otros = new ArrayList<>();
        for (HistorialJornada historial : historiales) {
            String notas = historial.getNotas() == null ? "" : historial.getNotas();
            if (notas.contains("RECICLAJE_PARCIAL_COMBO_HIJO")) {
                hijos.add(historial);
            } else if (notas.contains("RECICLAJE_PARCIAL_COMBO_PADRE")
                    || historial.getEstadoAsignacion() == EstadoAsignacion.reaprovechado) {
                padres.add(historial);
            } else {
                otros.add(historial);
            }
        }
        Set<HistorialJornada> hijosUsados = new HashSet<>();
        List<HistorialJornadaDTO> resultado = new ArrayList<>();
        for (HistorialJornada padre : padres) {
            Long padreId = comboIdDe(padre);
            if (padreId == null) {
                padreId = extraerComboIdDesdeNotasPadre(padre.getNotas());
            }
            HistorialJornada hijo = emparejarHijoReciclaje(hijos, padre, padreId, hijosUsados);
            if (hijo != null) {
                hijosUsados.add(hijo);
            }
            resultado.add(dtoReciclajeUnico(padre, hijo, padreId, cadena));
        }
        for (HistorialJornada hijo : hijos) {
            if (hijosUsados.contains(hijo)) {
                continue;
            }
            resultado.add(dtoReciclajeUnico(null, hijo, extraerComboPadreDesdeNotas(hijo.getNotas()), cadena));
        }
        for (HistorialJornada otro : otros) {
            HistorialJornadaDTO dto = convertirADTO(otro);
            if (cadena != null && !cadena.isEmpty()) {
                dto.setCadenaReciclajeIds(new ArrayList<>(cadena));
            }
            resultado.add(dto);
        }
        resultado.sort(Comparator.comparing(HistorialJornadaDTO::getFechaAsignacion,
            Comparator.nullsLast(Comparator.naturalOrder())));
        return resultado;
    }

    private HistorialJornada emparejarHijoReciclaje(List<HistorialJornada> hijos, HistorialJornada padre,
            Long padreId, Set<HistorialJornada> usados) {
        HistorialJornada candidato = null;
        for (HistorialJornada hijo : hijos) {
            if (usados.contains(hijo)) {
                continue;
            }
            Long padreDelHijo = extraerComboPadreDesdeNotas(hijo.getNotas());
            if (padreId == null || !padreId.equals(padreDelHijo)) {
                continue;
            }
            if (mismaJornadaHistorial(padre, hijo)) {
                return hijo;
            }
            if (candidato == null) {
                candidato = hijo;
            }
        }
        return candidato;
    }

    private HistorialJornadaDTO dtoReciclajeUnico(HistorialJornada padre, HistorialJornada hijo,
            Long padreId, List<Long> cadena) {
        HistorialJornada base = padre != null ? padre : hijo;
        HistorialJornadaDTO dto = convertirADTO(base);
        dto.setComboPadreId(padreId);
        if (padre != null && padre.getCombo() != null) {
            dto.setComboId(padre.getCombo().getId());
        }
        if (padre != null && padre.getPreguntaUsadaId() != null) {
            dto.setPreguntaUsadaId(padre.getPreguntaUsadaId());
        }
        Long hijoId = comboIdDe(hijo);
        if (hijoId != null) {
            dto.setComboHijosIds(Collections.singletonList(hijoId));
        }
        dto.setEstadoAsignacion(EstadoAsignacion.reaprovechado.name());
        if (cadena != null && !cadena.isEmpty()) {
            dto.setCadenaReciclajeIds(new ArrayList<>(cadena));
        }
        return dto;
    }

    private static boolean mismaJornadaHistorial(HistorialJornada a, HistorialJornada b) {
        if (a == null || b == null || a.getJornada() == null || b.getJornada() == null) {
            return false;
        }
        return Objects.equals(a.getJornada().getId(), b.getJornada().getId());
    }

    private static Long comboIdDe(HistorialJornada historial) {
        return historial != null && historial.getCombo() != null ? historial.getCombo().getId() : null;
    }

    private List<HistorialJornada> hijosDeComboPadre(Long comboId) {
        return listaSegura(historialRepository.findHijosDeComboPadre(comboId));
    }

    private static List<HistorialJornada> listaSegura(List<HistorialJornada> lista) {
        return lista == null ? Collections.emptyList() : lista;
    }

    static Long extraerComboPadreDesdeNotas(String notas) {
        if (notas == null) {
            return null;
        }
        int idx = notas.indexOf("COMBO_HIJO;PADRE:");
        if (idx < 0) {
            return null;
        }
        String resto = notas.substring(idx + "COMBO_HIJO;PADRE:".length());
        StringBuilder numero = new StringBuilder();
        for (int i = 0; i < resto.length(); i++) {
            char c = resto.charAt(i);
            if (Character.isDigit(c)) {
                numero.append(c);
            } else {
                break;
            }
        }
        return parsearIdFinal(numero);
    }

    static Long extraerComboIdDesdeNotasPadre(String notas) {
        if (notas == null) {
            return null;
        }
        int idx = notas.indexOf("RECICLAJE_PARCIAL_COMBO_PADRE:");
        if (idx < 0) {
            return null;
        }
        String resto = notas.substring(idx + "RECICLAJE_PARCIAL_COMBO_PADRE:".length());
        StringBuilder numero = new StringBuilder();
        for (int i = 0; i < resto.length(); i++) {
            char c = resto.charAt(i);
            if (Character.isDigit(c)) {
                numero.append(c);
            } else {
                break;
            }
        }
        return parsearIdFinal(numero);
    }

    private static Long parsearIdFinal(StringBuilder numero) {
        if (numero.length() == 0) {
            return null;
        }
        try {
            return Long.parseLong(numero.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Obtener asignaciones no usadas de una jornada
     */
    public List<HistorialJornadaDTO> obtenerNoUsados(Long jornadaId) {
        List<HistorialJornada> historiales = historialRepository.findByJornadaIdAndEstado(jornadaId, EstadoAsignacion.no_usado);
        return historiales.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }

    /**
     * Convertir entidad a DTO
     */
    private HistorialJornadaDTO convertirADTO(HistorialJornada historial) {
        HistorialJornadaDTO dto = new HistorialJornadaDTO();
        dto.setId(historial.getId());
        dto.setJornadaId(historial.getJornada().getId());
        dto.setJornadaNombre(historial.getJornada().getNombre());
        dto.setTipoAsignacion(historial.getTipoAsignacion().name());
        dto.setEstadoAsignacion(historial.getEstadoAsignacion().name());
        dto.setFechaAsignacion(historial.getFechaAsignacion());
        dto.setFechaUso(historial.getFechaUso());
        dto.setPreguntaUsadaId(historial.getPreguntaUsadaId());
        dto.setNotas(historial.getNotas());
        
        if (historial.getCuestionario() != null) {
            dto.setCuestionarioId(historial.getCuestionario().getId());
        }
        if (historial.getCombo() != null) {
            dto.setComboId(historial.getCombo().getId());
        }
        
        return dto;
    }
}
