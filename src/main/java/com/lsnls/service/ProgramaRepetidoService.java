package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.dto.ConcursanteEmitidoOpcionDTO;
import com.lsnls.dto.CrearProgramaRepetidoDTO;
import com.lsnls.dto.ProgramaEmitidoOpcionDTO;
import com.lsnls.dto.ProgramaRepetidoDTO;
import com.lsnls.dto.ResultadoBusquedaDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Programa;
import com.lsnls.entity.ProgramaConcursante;
import com.lsnls.entity.ProgramaRepetido;
import com.lsnls.entity.ProgramaRepetidoConcursante;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.ProgramaConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoConcursanteRepository;
import com.lsnls.repository.ProgramaRepetidoRepository;
import com.lsnls.repository.ProgramaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProgramaRepetidoService {

    static final List<String> ESCALA_VALORACION = List.of(
            "1", "1+", "2-", "2", "2+", "3-", "3", "3+");

    private static final Set<String> TIPOS = Set.of("REPE", "LB", "VB", "VERANO");
    private static final Set<String> CAMPOS_EDITABLES = Set.of(
            "codigo", "temporada", "fechaEmision", "estado", "creditosEspeciales", "notas");
    private static final Set<String> ESTADOS_ASIGNABLES = Set.of("grabado", "editado", "emitido");
    private static final int MAX_CONCURSANTES = 3;
    private static final int LIMITE_BUSQUEDA = 120;

    @Autowired
    private ProgramaRepetidoRepository programaRepetidoRepository;

    @Autowired
    private ProgramaRepetidoConcursanteRepository enlaceRepository;

    @Autowired
    private ConcursanteRepository concursanteRepository;

    @Autowired
    private ConcursanteService concursanteService;

    @Autowired
    private ProgramaRepository programaRepository;

    @Autowired
    private ProgramaConcursanteRepository programaConcursanteRepository;

    @Autowired
    private UndoService undoService;

    /** Cubre el cálculo del código y el insert para que dos altas no reciban el mismo número. */
    private final Object asignacionCodigoLock = new Object();

    public List<ProgramaRepetidoDTO> findAll() {
        return programaRepetidoRepository.findAllByOrderByFechaEmisionDescIdDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProgramaRepetidoDTO findById(Long id) {
        return toDto(exigirRepetido(id));
    }

    public List<ConcursanteDTO> findConcursantes(Long repetidoId) {
        exigirRepetido(repetidoId);
        List<ProgramaRepetidoConcursante> enlaces = enlaceRepository
                .findByProgramaRepetidoIdOrderByOrdenEscaletaAsc(repetidoId);
        if (enlaces.isEmpty()) {
            return List.of();
        }
        Map<Long, Concursante> porId = concursanteRepository.findAllById(
                enlaces.stream().map(ProgramaRepetidoConcursante::getConcursanteId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Concursante::getId, concursante -> concursante));
        List<ConcursanteDTO> resultado = new ArrayList<>();
        for (ProgramaRepetidoConcursante enlace : enlaces) {
            Concursante concursante = porId.get(enlace.getConcursanteId());
            if (concursante == null) {
                continue;
            }
            ConcursanteDTO dto = concursanteService.copiarDto(concursante);
            dto.setOrdenEscaleta(enlace.getOrdenEscaleta());
            resultado.add(dto);
        }
        resultado.sort(Comparator.comparing(dto -> dto.getOrdenEscaleta() == null ? Integer.MAX_VALUE : dto.getOrdenEscaleta()));
        return resultado;
    }

    public Page<ConcursanteDTO> findDisponibles(Long repetidoId, Pageable pageable, String busqueda, String estado) {
        exigirRepetido(repetidoId);
        return concursanteService.findDisponiblesParaRepetido(pageable, busqueda, estado, repetidoId);
    }

    public String siguienteCodigoSugerido(String tipo, Integer temporada) {
        String tipoNormalizado = normalizarTipo(tipo);
        if (temporada == null || temporada < 1) {
            throw new IllegalArgumentException("La temporada es obligatoria para sugerir el código.");
        }
        return String.valueOf(siguienteCodigoNumerico(tipoNormalizado, temporada));
    }

    public ResultadoBusquedaDTO<ProgramaEmitidoOpcionDTO> buscarProgramasEmitidos(
            String fechaEmision, String codigo, String valoracionMinima) {
        Integer minimo = indiceValoracionMinima(valoracionMinima);
        LocalDate fecha = parseFechaFiltro(fechaEmision);
        IndiceEmitidos indice = indiceEmitidos();
        List<ProgramaEmitidoOpcionDTO> coinciden = new ArrayList<>();
        for (Programa programa : indice.programas) {
            if (fecha != null && !fecha.equals(programa.getFechaEmision())) {
                continue;
            }
            if (!coincideCodigo(programa, codigo)) {
                continue;
            }
            List<ConcursanteOrden> concursantes = indice.porPrograma.getOrDefault(programa.getId(), List.of());
            if (!programaCumpleValoracion(concursantes, minimo)) {
                continue;
            }
            coinciden.add(opcionPrograma(programa, concursantes));
        }
        coinciden.sort(Comparator
                .comparing(ProgramaEmitidoOpcionDTO::getFechaEmision, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(dto -> codigoEntero(dto.getCodigo()) == null ? Long.MAX_VALUE : codigoEntero(dto.getCodigo()))
                .thenComparing(dto -> dto.getCodigo() == null ? "" : dto.getCodigo()));
        return recortar(coinciden);
    }

    public ResultadoBusquedaDTO<ConcursanteEmitidoOpcionDTO> buscarConcursantesEmitidos(
            String valoracionMinima, String xusoker, String numero, String ciudad, String nombre) {
        Integer minimo = indiceValoracionMinima(valoracionMinima);
        Integer numeroExacto = parseNumeroConcursante(numero);
        String xusokerFiltro = blancoANull(xusoker);
        String ciudadFiltro = blancoANull(ciudad);
        String nombreFiltro = blancoANull(nombre);
        IndiceEmitidos indice = indiceEmitidos();
        List<ConcursanteEmitidoOpcionDTO> coinciden = new ArrayList<>();
        for (Aparicion aparicion : indice.aparicionPrincipal.values()) {
            Concursante concursante = aparicion.concursante;
            if (!estadoAsignable(concursante.getEstado())) {
                continue;
            }
            if (minimo != null && rangoValoracion(concursante.getValoracionFinal()) < minimo) {
                continue;
            }
            if (xusokerFiltro != null && !xusokerFiltro.equals(concursante.getXusoker() == null ? "" : concursante.getXusoker().trim())) {
                continue;
            }
            if (numeroExacto != null && !numeroExacto.equals(concursante.getNumeroConcursante())) {
                continue;
            }
            if (ciudadFiltro != null && !contiene(concursante.getLugar(), ciudadFiltro)) {
                continue;
            }
            if (nombreFiltro != null && !contiene(concursante.getNombre(), nombreFiltro)) {
                continue;
            }
            coinciden.add(opcionConcursante(aparicion));
        }
        coinciden.sort(Comparator
                .comparing((ConcursanteEmitidoOpcionDTO dto) -> dto.getNumeroConcursante() == null
                        ? Integer.MAX_VALUE : dto.getNumeroConcursante())
                .thenComparing(dto -> dto.getNombre() == null ? "" : dto.getNombre(), String.CASE_INSENSITIVE_ORDER));
        return recortar(coinciden);
    }

    @Transactional
    public ProgramaRepetidoDTO crear(CrearProgramaRepetidoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Faltan los datos del programa repetido.");
        }
        if (dto.getTemporada() == null || dto.getTemporada() < 1) {
            throw new IllegalArgumentException("La temporada es obligatoria.");
        }
        boolean tienePrograma = dto.getProgramaOriginalId() != null;
        List<Long> idsPedidos = dto.getConcursanteIds() == null ? List.of() : dto.getConcursanteIds();
        boolean tieneConcursantes = !idsPedidos.isEmpty();
        if (tienePrograma && tieneConcursantes) {
            throw new IllegalArgumentException("Elige un programa emitido o concursantes sueltos, no las dos cosas a la vez.");
        }
        if (!tienePrograma && !tieneConcursantes) {
            throw new IllegalArgumentException("Selecciona un programa emitido o al menos un concursante.");
        }
        if (tieneConcursantes && idsPedidos.size() > MAX_CONCURSANTES) {
            throw new IllegalArgumentException("Un programa repetido admite como máximo 3 concursantes.");
        }
        if (tieneConcursantes && new HashSet<>(idsPedidos).size() != idsPedidos.size()) {
            throw new IllegalArgumentException("Hay concursantes repetidos en la selección.");
        }
        if (idsPedidos.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("La selección de concursantes no es válida.");
        }

        String tipo = normalizarTipo(dto.getTipo());
        String aviso = null;
        List<SlotConcursante> slots;
        String programaOriginalCodigo = null;
        LocalDate fechaOriginal = null;
        if (tienePrograma) {
            Programa original = programaRepository.findById(dto.getProgramaOriginalId())
                    .orElseThrow(() -> new IllegalArgumentException("El programa original no existe."));
            if (original.getEstado() != Programa.EstadoPrograma.emitido) {
                throw new IllegalArgumentException("Solo se puede repetir un programa ya emitido.");
            }
            programaOriginalCodigo = original.getCodigo() != null && !original.getCodigo().isBlank()
                    ? original.getCodigo().trim()
                    : String.valueOf(original.getId());
            fechaOriginal = original.getFechaEmision();
            List<ConcursanteDTO> delPrograma = concursanteService.findByProgramaId(original.getId());
            if (delPrograma.size() > MAX_CONCURSANTES) {
                aviso = "El programa original tiene más de 3 concursantes. Se han enlazado los 3 primeros de la escaleta.";
                delPrograma = delPrograma.subList(0, MAX_CONCURSANTES);
            }
            slots = slotsDesdePrograma(delPrograma);
        } else {
            slots = slotsDesdeSeleccion(idsPedidos);
        }

        synchronized (asignacionCodigoLock) {
            ProgramaRepetido repetido = new ProgramaRepetido();
            repetido.setTipo(tipo);
            repetido.setTemporada(dto.getTemporada());
            repetido.setFechaEmision(dto.getFechaEmision());
            repetido.setCodigo(codigoLibre(tipo, dto.getTemporada(), dto.getFechaEmision(), dto.getCodigo()));
            repetido.setProgramaOriginalCodigo(programaOriginalCodigo);
            repetido.setFechaOriginal(fechaOriginal);
            repetido.setEstado(dto.getFechaEmision() != null
                    ? Programa.EstadoPrograma.programado
                    : Programa.EstadoPrograma.borrador);
            repetido.setTotalConcursantes(0);
            repetido = programaRepetidoRepository.saveAndFlush(repetido);
            for (SlotConcursante slot : slots) {
                asignar(repetido.getId(), slot.concursanteId, slot.orden);
            }
            ProgramaRepetidoDTO respuesta = toDto(exigirRepetido(repetido.getId()));
            respuesta.setAviso(aviso);
            return respuesta;
        }
    }

    @Transactional
    public void delete(Long id) {
        ProgramaRepetido repetido = exigirRepetido(id);
        if (repetido.getEstado() == Programa.EstadoPrograma.emitido) {
            throw new IllegalArgumentException(
                    "No se puede eliminar un programa repetido que ya ha sido emitido.");
        }
        Map<String, Object> fila = undoService.snapshotFila("programas_repetidos", id);
        List<Map<String, Object>> enlaces = undoService.snapshotFilas(
                "programas_repetidos_concursantes", "programa_repetido_id", id);
        enlaceRepository.deleteByProgramaRepetidoId(id);
        programaRepetidoRepository.delete(repetido);

        List<Map<String, Object>> acciones = new ArrayList<>();
        if (fila != null) {
            acciones.add(UndoService.accionInsertarFila("programas_repetidos", fila));
        }
        for (Map<String, Object> enlace : enlaces) {
            acciones.add(UndoService.accionInsertarFila("programas_repetidos_concursantes", enlace));
        }
        String codigo = repetido.getCodigo() != null ? repetido.getCodigo() : String.valueOf(id);
        undoService.registrar("eliminar_programa_repetido", "Eliminar repetido " + codigo, acciones);
    }

    @Transactional
    public ProgramaRepetidoDTO updateCampo(Long id, Map<String, Object> campo) {
        if (campo == null || campo.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un campo para actualizar.");
        }
        for (String key : campo.keySet()) {
            if (!CAMPOS_EDITABLES.contains(key)) {
                throw new IllegalArgumentException("Ese campo no se puede editar en un programa repetido.");
            }
        }
        ProgramaRepetido repetido = exigirRepetido(id);
        if (repetido.getEstado() == Programa.EstadoPrograma.emitido) {
            throw new IllegalArgumentException("Un programa emitido no se puede editar.");
        }
        for (Map.Entry<String, Object> entry : campo.entrySet()) {
            aplicarCampo(repetido, entry.getKey(), entry.getValue());
        }
        exigirCombinacionLibre(repetido);
        return toDto(programaRepetidoRepository.save(repetido));
    }

    private void aplicarCampo(ProgramaRepetido repetido, String key, Object value) {
        switch (key) {
            case "codigo":
                String codigo = normalizarCodigo(value != null ? value.toString() : null);
                if (codigo == null) {
                    throw new IllegalArgumentException("El código de programa es obligatorio.");
                }
                repetido.setCodigo(codigo);
                break;
            case "temporada":
                repetido.setTemporada(parseTemporada(value));
                break;
            case "fechaEmision":
                repetido.setFechaEmision(parseFechaEditable(value));
                break;
            case "estado":
                repetido.setEstado(parseEstado(value));
                break;
            case "creditosEspeciales":
                repetido.setCreditosEspeciales(textoONull(value));
                break;
            case "notas":
                repetido.setNotas(textoONull(value));
                break;
            default:
                throw new IllegalArgumentException("Ese campo no se puede editar en un programa repetido.");
        }
    }

    private void exigirCombinacionLibre(ProgramaRepetido repetido) {
        Optional<ProgramaRepetido> existente = repetido.getFechaEmision() == null
                ? programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull(
                        repetido.getTipo(), repetido.getCodigo(), repetido.getTemporada())
                : programaRepetidoRepository.findByTipoAndCodigoAndTemporadaAndFechaEmision(
                        repetido.getTipo(), repetido.getCodigo(), repetido.getTemporada(), repetido.getFechaEmision());
        if (existente.isPresent() && !existente.get().getId().equals(repetido.getId())) {
            throw new IllegalArgumentException(mensajeCodigoDuplicado(
                    repetido.getTipo(), repetido.getCodigo(), repetido.getTemporada(), repetido.getFechaEmision()));
        }
    }

    private static Integer parseTemporada(Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("La temporada es obligatoria.");
        }
        try {
            int temporada = Integer.parseInt(value.toString().trim());
            if (temporada < 1) {
                throw new IllegalArgumentException("La temporada debe ser un número mayor o igual a 1.");
            }
            return temporada;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("La temporada debe ser un número mayor o igual a 1.");
        }
    }

    private static LocalDate parseFechaEditable(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.toString().trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de emisión no es válida.");
        }
    }

    private static Programa.EstadoPrograma parseEstado(Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio.");
        }
        String texto = value.toString().trim().toLowerCase(Locale.ROOT);
        try {
            return Programa.EstadoPrograma.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "El estado no es válido. Usa borrador, grabado, editado, programado o emitido.");
        }
    }

    private static String textoONull(Object value) {
        if (value == null) {
            return null;
        }
        String texto = value.toString();
        return texto.isBlank() ? null : texto;
    }

    @Transactional
    public ConcursanteDTO asignar(Long repetidoId, Long concursanteId, Integer posicion) {
        exigirRepetido(repetidoId);
        Concursante concursante = concursanteRepository.findById(concursanteId)
                .orElseThrow(() -> new IllegalArgumentException("Concursante no encontrado"));
        String estado = concursante.getEstado() == null ? "" : concursante.getEstado().trim().toLowerCase(Locale.ROOT);
        if (!ESTADOS_ASIGNABLES.contains(estado)) {
            throw new IllegalArgumentException("Solo se pueden añadir concursantes en estado grabado, editado o emitido.");
        }
        if (enlaceRepository.existsByProgramaRepetidoIdAndConcursanteId(repetidoId, concursanteId)) {
            throw new IllegalArgumentException("Ese concursante ya está en este repetido.");
        }
        int slot = posicion != null ? posicion : primerHuecoLibre(repetidoId);
        if (slot < 1 || slot > MAX_CONCURSANTES) {
            throw new IllegalArgumentException("La posición debe estar entre 1 y 3");
        }
        if (enlaceRepository.existsByProgramaRepetidoIdAndOrdenEscaleta(repetidoId, slot)) {
            throw new IllegalArgumentException("La posición " + slot + " ya está ocupada en este repetido");
        }
        ProgramaRepetidoConcursante enlace = new ProgramaRepetidoConcursante();
        enlace.setProgramaRepetidoId(repetidoId);
        enlace.setConcursanteId(concursanteId);
        enlace.setOrdenEscaleta(slot);
        enlaceRepository.save(enlace);
        actualizarTotal(repetidoId);
        ConcursanteDTO respuesta = concursanteService.copiarDto(concursante);
        respuesta.setOrdenEscaleta(slot);
        return respuesta;
    }

    @Transactional
    public void quitar(Long repetidoId, Long concursanteId) {
        exigirRepetido(repetidoId);
        if (!enlaceRepository.existsByProgramaRepetidoIdAndConcursanteId(repetidoId, concursanteId)) {
            throw new IllegalArgumentException("Ese concursante no está en este repetido.");
        }
        enlaceRepository.deleteByProgramaRepetidoIdAndConcursanteId(repetidoId, concursanteId);
        actualizarTotal(repetidoId);
    }

    private List<SlotConcursante> slotsDesdePrograma(List<ConcursanteDTO> delPrograma) {
        List<SlotConcursante> slots = new ArrayList<>();
        Set<Integer> usados = new HashSet<>();
        int siguiente = 1;
        for (ConcursanteDTO concursante : delPrograma) {
            if (concursante.getId() == null) {
                continue;
            }
            Integer slot = concursante.getOrdenEscaleta();
            if (slot == null || slot < 1 || slot > MAX_CONCURSANTES || usados.contains(slot)) {
                while (usados.contains(siguiente) && siguiente <= MAX_CONCURSANTES) {
                    siguiente++;
                }
                slot = siguiente;
            }
            if (slot > MAX_CONCURSANTES) {
                break;
            }
            usados.add(slot);
            slots.add(new SlotConcursante(concursante.getId(), slot));
        }
        return slots;
    }

    private List<SlotConcursante> slotsDesdeSeleccion(List<Long> ids) {
        List<SlotConcursante> slots = new ArrayList<>();
        int orden = 1;
        for (Long id : ids) {
            Concursante concursante = concursanteRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Concursante no encontrado"));
            if (!perteneceAProgramaEmitido(concursante)) {
                throw new IllegalArgumentException(
                        "El concursante " + etiquetaConcursante(concursante) + " no pertenece a un programa emitido.");
            }
            slots.add(new SlotConcursante(id, orden));
            orden++;
        }
        return slots;
    }

    private boolean perteneceAProgramaEmitido(Concursante concursante) {
        if (concursante.getNumeroPrograma() != null) {
            Programa porFicha = programaRepository.findById(concursante.getNumeroPrograma().longValue()).orElse(null);
            if (porFicha != null && porFicha.getEstado() == Programa.EstadoPrograma.emitido) {
                return true;
            }
        }
        for (ProgramaConcursante enlace : programaConcursanteRepository.findByConcursanteId(concursante.getId())) {
            Programa programa = programaRepository.findById(enlace.getProgramaId()).orElse(null);
            if (programa != null && programa.getEstado() == Programa.EstadoPrograma.emitido) {
                return true;
            }
        }
        return false;
    }

    private String codigoLibre(String tipo, Integer temporada, LocalDate fecha, String pedido) {
        String codigo = normalizarCodigo(pedido);
        if (codigo != null) {
            if (existeRepetido(tipo, codigo, temporada, fecha)) {
                throw new IllegalArgumentException(mensajeCodigoDuplicado(tipo, codigo, temporada, fecha));
            }
            return codigo;
        }
        long numero = siguienteCodigoNumerico(tipo, temporada);
        String candidato = String.valueOf(numero);
        int intentos = 0;
        while (existeRepetido(tipo, candidato, temporada, fecha)) {
            if (numero == Long.MAX_VALUE || ++intentos > 10000) {
                throw new IllegalArgumentException("No hay un código de repetido disponible.");
            }
            numero++;
            candidato = String.valueOf(numero);
        }
        return candidato;
    }

    private boolean existeRepetido(String tipo, String codigo, Integer temporada, LocalDate fecha) {
        if (fecha == null) {
            return programaRepetidoRepository
                    .existsByTipoAndCodigoAndTemporadaAndFechaEmisionIsNull(tipo, codigo, temporada);
        }
        return programaRepetidoRepository
                .existsByTipoAndCodigoAndTemporadaAndFechaEmision(tipo, codigo, temporada, fecha);
    }

    private long siguienteCodigoNumerico(String tipo, Integer temporada) {
        List<String> codigos = programaRepetidoRepository.findCodigosByTipoAndTemporada(tipo, temporada);
        long maximo = 0L;
        if (codigos != null) {
            for (String codigo : codigos) {
                Long valor = codigoEntero(codigo);
                if (valor != null && valor > maximo) {
                    maximo = valor;
                }
            }
        }
        if (maximo == Long.MAX_VALUE) {
            throw new IllegalArgumentException("No hay un código de repetido disponible.");
        }
        return maximo + 1L;
    }

    private static String mensajeCodigoDuplicado(String tipo, String codigo, Integer temporada, LocalDate fecha) {
        String cuando = fecha == null ? "sin fecha de emisión" : "con fecha " + fecha;
        return "Ya existe un repetido " + tipo + " con código " + codigo
                + " en la temporada " + temporada + " " + cuando + ".";
    }

    private IndiceEmitidos indiceEmitidos() {
        List<Programa> programas = programaRepository.findByEstado(Programa.EstadoPrograma.emitido);
        Map<Long, Programa> porId = new HashMap<>();
        for (Programa programa : programas) {
            porId.put(programa.getId(), programa);
        }
        Map<Long, LinkedHashMap<Long, ConcursanteOrden>> agrupados = new HashMap<>();
        if (!porId.isEmpty()) {
            List<ProgramaConcursante> enlaces = programaConcursanteRepository.findByProgramaIdIn(porId.keySet());
            Set<Long> concursanteIds = new HashSet<>();
            for (ProgramaConcursante enlace : enlaces) {
                concursanteIds.add(enlace.getConcursanteId());
            }
            List<Integer> numeros = porId.keySet().stream().map(Long::intValue).collect(Collectors.toList());
            List<Concursante> porFicha = numeros.isEmpty()
                    ? List.of()
                    : concursanteRepository.findByNumeroProgramaIn(numeros);
            for (Concursante concursante : porFicha) {
                if (concursante.getId() != null) {
                    concursanteIds.add(concursante.getId());
                }
            }
            Map<Long, Concursante> concursantes = concursanteIds.isEmpty()
                    ? Map.of()
                    : concursanteRepository.findAllById(concursanteIds).stream()
                    .collect(Collectors.toMap(Concursante::getId, concursante -> concursante));
            for (ProgramaConcursante enlace : enlaces) {
                Programa programa = porId.get(enlace.getProgramaId());
                Concursante concursante = concursantes.get(enlace.getConcursanteId());
                if (programa == null || concursante == null) {
                    continue;
                }
                agrupar(agrupados, programa.getId(), new ConcursanteOrden(concursante, enlace.getOrdenEscaleta()));
            }
            for (Concursante concursante : porFicha) {
                if (concursante.getNumeroPrograma() == null || concursante.getId() == null) {
                    continue;
                }
                Programa programa = porId.get(concursante.getNumeroPrograma().longValue());
                if (programa == null) {
                    continue;
                }
                agrupar(agrupados, programa.getId(), new ConcursanteOrden(concursante, concursante.getOrdenEscaleta()));
            }
        }

        Map<Long, List<ConcursanteOrden>> porPrograma = new HashMap<>();
        Map<Long, Aparicion> principal = new HashMap<>();
        for (Map.Entry<Long, LinkedHashMap<Long, ConcursanteOrden>> entrada : agrupados.entrySet()) {
            Programa programa = porId.get(entrada.getKey());
            if (programa == null) {
                continue;
            }
            List<ConcursanteOrden> lista = new ArrayList<>(entrada.getValue().values());
            lista.sort(Comparator.comparing(item -> item.orden == null ? Integer.MAX_VALUE : item.orden));
            porPrograma.put(programa.getId(), lista);
            for (ConcursanteOrden item : lista) {
                Aparicion actual = principal.get(item.concursante.getId());
                if (actual == null || esAparicionMasReciente(programa, actual.programa)) {
                    principal.put(item.concursante.getId(), new Aparicion(programa, item));
                }
            }
        }
        IndiceEmitidos indice = new IndiceEmitidos();
        indice.programas = programas;
        indice.porPrograma = porPrograma;
        indice.aparicionPrincipal = principal;
        return indice;
    }

    private static void agrupar(Map<Long, LinkedHashMap<Long, ConcursanteOrden>> agrupados,
                                Long programaId, ConcursanteOrden item) {
        agrupados.computeIfAbsent(programaId, id -> new LinkedHashMap<>())
                .putIfAbsent(item.concursante.getId(), item);
    }

    private static boolean esAparicionMasReciente(Programa candidato, Programa actual) {
        LocalDate fechaCandidato = candidato.getFechaEmision();
        LocalDate fechaActual = actual.getFechaEmision();
        if (fechaCandidato == null && fechaActual == null) {
            return candidato.getId() > actual.getId();
        }
        if (fechaCandidato == null) {
            return false;
        }
        if (fechaActual == null || fechaCandidato.isAfter(fechaActual)) {
            return true;
        }
        if (fechaCandidato.isEqual(fechaActual)) {
            return candidato.getId() > actual.getId();
        }
        return false;
    }

    private static boolean programaCumpleValoracion(List<ConcursanteOrden> concursantes, Integer minimo) {
        if (minimo == null) {
            return true;
        }
        if (concursantes.isEmpty()) {
            return false;
        }
        for (ConcursanteOrden item : concursantes) {
            if (rangoValoracion(item.concursante.getValoracionFinal()) < minimo) {
                return false;
            }
        }
        return true;
    }

    private static ProgramaEmitidoOpcionDTO opcionPrograma(Programa programa, List<ConcursanteOrden> concursantes) {
        ProgramaEmitidoOpcionDTO dto = new ProgramaEmitidoOpcionDTO();
        dto.setId(programa.getId());
        dto.setCodigo(programa.getCodigo());
        dto.setTemporada(programa.getTemporada());
        dto.setFechaEmision(programa.getFechaEmision());
        dto.setTotalConcursantes(concursantes.size());
        dto.setResumenConcursantes(concursantes.stream()
                .map(item -> {
                    String nombre = item.concursante.getNombre() == null ? "Sin nombre" : item.concursante.getNombre();
                    String valoracion = item.concursante.getValoracionFinal();
                    return (valoracion == null || valoracion.isBlank()) ? nombre : nombre + " (" + valoracion.trim() + ")";
                })
                .collect(Collectors.joining(" · ")));
        return dto;
    }

    private static ConcursanteEmitidoOpcionDTO opcionConcursante(Aparicion aparicion) {
        Concursante concursante = aparicion.concursante;
        ConcursanteEmitidoOpcionDTO dto = new ConcursanteEmitidoOpcionDTO();
        dto.setId(concursante.getId());
        dto.setNumeroConcursante(concursante.getNumeroConcursante());
        dto.setNombre(concursante.getNombre());
        dto.setLugar(concursante.getLugar());
        dto.setXusoker(concursante.getXusoker());
        dto.setValoracionFinal(concursante.getValoracionFinal());
        dto.setProgramaCodigo(aparicion.programa.getCodigo());
        dto.setFechaEmisionPrograma(aparicion.programa.getFechaEmision());
        dto.setOrdenEscaleta(aparicion.orden.orden);
        return dto;
    }

    private static <T> ResultadoBusquedaDTO<T> recortar(List<T> coinciden) {
        ResultadoBusquedaDTO<T> resultado = new ResultadoBusquedaDTO<>();
        resultado.setHayMas(coinciden.size() > LIMITE_BUSQUEDA);
        resultado.setResultados(coinciden.size() > LIMITE_BUSQUEDA
                ? new ArrayList<>(coinciden.subList(0, LIMITE_BUSQUEDA))
                : coinciden);
        return resultado;
    }

    private static Integer indiceValoracionMinima(String valoracion) {
        if (valoracion == null || valoracion.isBlank()) {
            return null;
        }
        int indice = ESCALA_VALORACION.indexOf(valoracion.trim());
        if (indice < 0) {
            throw new IllegalArgumentException("La valoración mínima no es válida.");
        }
        return indice;
    }

    static int rangoValoracion(String valoracion) {
        if (valoracion == null) {
            return -1;
        }
        return ESCALA_VALORACION.indexOf(valoracion.trim());
    }

    private static LocalDate parseFechaFiltro(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de emisión no es válida.");
        }
    }

    private static Integer parseNumeroConcursante(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(numero.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El número de concursante no es válido.");
        }
    }

    private static boolean coincideCodigo(Programa programa, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        String pedido = filtro.trim();
        if (pedido.regionMatches(true, 0, "PGM", 0, 3)) {
            pedido = pedido.substring(3).trim();
        }
        String codigo = programa.getCodigo() == null ? "" : programa.getCodigo().trim();
        if (codigo.equalsIgnoreCase(pedido)) {
            return true;
        }
        String sinPrefijo = codigo.regionMatches(true, 0, "PGM", 0, 3) ? codigo.substring(3).trim() : codigo;
        if (sinPrefijo.equalsIgnoreCase(pedido)) {
            return true;
        }
        return programa.getId() != null && pedido.equals(String.valueOf(programa.getId()));
    }

    private static boolean contiene(String valor, String filtro) {
        if (valor == null) {
            return false;
        }
        return valor.toLowerCase(Locale.ROOT).contains(filtro.trim().toLowerCase(Locale.ROOT));
    }

    private static String blancoANull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private static boolean estadoAsignable(String estado) {
        if (estado == null) {
            return false;
        }
        return ESTADOS_ASIGNABLES.contains(estado.trim().toLowerCase(Locale.ROOT));
    }

    private static String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "REPE";
        }
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(normalizado)) {
            throw new IllegalArgumentException("El tipo debe ser REPE, LB, VB o VERANO.");
        }
        return normalizado;
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        String normalizado = codigo.trim();
        if (normalizado.length() > 32) {
            throw new IllegalArgumentException("El código no puede superar 32 caracteres.");
        }
        return normalizado;
    }

    private static Long codigoEntero(String codigo) {
        if (codigo == null) {
            return null;
        }
        String normalizado = codigo.trim();
        if (!normalizado.matches("[0-9]+")) {
            return null;
        }
        try {
            return Long.parseLong(normalizado);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String etiquetaConcursante(Concursante concursante) {
        if (concursante.getNumeroConcursante() != null) {
            return String.valueOf(concursante.getNumeroConcursante());
        }
        if (concursante.getNombre() != null && !concursante.getNombre().isBlank()) {
            return concursante.getNombre().trim();
        }
        return String.valueOf(concursante.getId());
    }

    private int primerHuecoLibre(Long repetidoId) {
        Set<Integer> usadas = enlaceRepository.findByProgramaRepetidoIdOrderByOrdenEscaletaAsc(repetidoId).stream()
                .map(ProgramaRepetidoConcursante::getOrdenEscaleta)
                .collect(Collectors.toSet());
        for (int slot = 1; slot <= MAX_CONCURSANTES; slot++) {
            if (!usadas.contains(slot)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("El repetido ya tiene ocupadas las 3 posiciones de concursante");
    }

    private void actualizarTotal(Long repetidoId) {
        ProgramaRepetido repetido = exigirRepetido(repetidoId);
        repetido.setTotalConcursantes((int) enlaceRepository.countByProgramaRepetidoId(repetidoId));
        programaRepetidoRepository.save(repetido);
    }

    private ProgramaRepetido exigirRepetido(Long repetidoId) {
        return programaRepetidoRepository.findById(repetidoId)
                .orElseThrow(() -> new IllegalArgumentException("Programa repetido no encontrado"));
    }

    private ProgramaRepetidoDTO toDto(ProgramaRepetido repetido) {
        ProgramaRepetidoDTO dto = new ProgramaRepetidoDTO();
        dto.setId(repetido.getId());
        dto.setCodigo(repetido.getCodigo());
        dto.setVersion(repetido.getVersion());
        dto.setTemporada(repetido.getTemporada());
        dto.setFechaEmision(repetido.getFechaEmision());
        dto.setEstado(repetido.getEstado() != null ? repetido.getEstado().name() : null);
        dto.setTotalPremios(repetido.getTotalPremios());
        dto.setGap(repetido.getGap());
        dto.setTotalConcursantes(repetido.getTotalConcursantes());
        dto.setCreditosEspeciales(repetido.getCreditosEspeciales());
        dto.setNotas(repetido.getNotas());
        dto.setDuracionObjetivo("45m");
        dto.setTipo(repetido.getTipo());
        dto.setProgramaOriginalCodigo(repetido.getProgramaOriginalCodigo());
        dto.setFechaOriginal(repetido.getFechaOriginal());
        dto.setRepetido(true);
        return dto;
    }

    private static final class IndiceEmitidos {
        private List<Programa> programas = List.of();
        private Map<Long, List<ConcursanteOrden>> porPrograma = Map.of();
        private Map<Long, Aparicion> aparicionPrincipal = Map.of();
    }

    private static final class ConcursanteOrden {
        private final Concursante concursante;
        private final Integer orden;

        private ConcursanteOrden(Concursante concursante, Integer orden) {
            this.concursante = concursante;
            this.orden = orden;
        }
    }

    private static final class Aparicion {
        private final Programa programa;
        private final Concursante concursante;
        private final ConcursanteOrden orden;

        private Aparicion(Programa programa, ConcursanteOrden orden) {
            this.programa = programa;
            this.concursante = orden.concursante;
            this.orden = orden;
        }
    }

    private static final class SlotConcursante {
        private final Long concursanteId;
        private final int orden;

        private SlotConcursante(Long concursanteId, int orden) {
            this.concursanteId = concursanteId;
            this.orden = orden;
        }
    }
}
