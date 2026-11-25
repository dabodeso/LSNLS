package com.lsnls.service;

import com.lsnls.dto.ConcursanteDTO;
import com.lsnls.dto.ProgramaDTO;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Programa;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.ProgramaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;

@Service
public class ProgramaService {

    @Autowired
    private ProgramaRepository programaRepository;

    @Autowired
    private ConfiguracionGlobalService configuracionService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UndoService undoService;

    @Autowired
    private ConcursanteRepository concursanteRepository;

    @Autowired
    private ConcursanteService concursanteService;

    @Autowired
    private AuthorizationService authorizationService;

    /** Cubre el cálculo del código y el insert para que dos altas no reciban el mismo número. */
    private final Object asignacionCodigoLock = new Object();

    public List<Programa> findAll() {
        return programaRepository.findAll();
    }

    public List<ProgramaDTO> findAllDTO() {
        return programaRepository.findAll().stream()
                .sorted(COMPARADOR_FECHA_EMISION_DESC)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> findAllPaginated(Pageable pageable) {
        Pageable efectivo = pageableSeguro(pageable);
        Page<Programa> page;
        if (ordenaPorCodigo(efectivo)) {
            page = paginarPorCodigoNumerico(efectivo);
        } else if (ordenaPorFechaEmision(efectivo)) {
            // MySQL pone NULL al final en DESC; nullsFirst() del dialecto no es fiable.
            page = paginarPorFechaEmision(efectivo);
        } else {
            page = programaRepository.findAll(efectivo);
        }
        
        List<ProgramaDTO> programas = page.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("programas", programas);
        response.put("currentPage", page.getNumber());
        response.put("totalItems", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        
        return response;
    }

    private static final Set<String> CAMPOS_ORDENABLES = new HashSet<>(java.util.Arrays.asList(
            "id", "codigo", "temporada", "fechaEmision", "estado", "totalConcursantes", "totalPremios", "gap"));

    private static final Comparator<Programa> COMPARADOR_CODIGO_NUMERICO = (a, b) -> {
        int cmp = compararCodigoNumerico(a != null ? a.getCodigo() : null, b != null ? b.getCodigo() : null);
        if (cmp != 0) {
            return cmp;
        }
        long idA = a != null && a.getId() != null ? a.getId() : 0L;
        long idB = b != null && b.getId() != null ? b.getId() : 0L;
        return Long.compare(idA, idB);
    };

    /** Sin fecha primero; resto fechaEmision DESC; empate por id DESC. */
    private static final Comparator<Programa> COMPARADOR_FECHA_EMISION_DESC =
            comparadorFechaEmision(Sort.Direction.DESC);

    private static Comparator<Programa> comparadorFechaEmision(Sort.Direction direction) {
        Sort.Direction dir = direction != null ? direction : Sort.Direction.DESC;
        return (a, b) -> {
            java.time.LocalDate fechaA = a != null ? a.getFechaEmision() : null;
            java.time.LocalDate fechaB = b != null ? b.getFechaEmision() : null;
            if (fechaA == null && fechaB != null) {
                return -1;
            }
            if (fechaA != null && fechaB == null) {
                return 1;
            }
            if (fechaA != null && fechaB != null) {
                int cmp = dir == Sort.Direction.ASC
                        ? fechaA.compareTo(fechaB)
                        : fechaB.compareTo(fechaA);
                if (cmp != 0) {
                    return cmp;
                }
            }
            long idA = a != null && a.getId() != null ? a.getId() : 0L;
            long idB = b != null && b.getId() != null ? b.getId() : 0L;
            return Long.compare(idB, idA);
        };
    }

    private Pageable pageableSeguro(Pageable pageable) {
        Sort sortFechaNullsFirst = Sort.by(new Sort.Order(Sort.Direction.DESC, "fechaEmision").nullsFirst());
        if (pageable == null) {
            return PageRequest.of(0, 10, sortFechaNullsFirst);
        }
        Sort sort = pageable.getSort();
        if (sort == null || !sort.isSorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortFechaNullsFirst);
        }
        Sort.Order order = sort.iterator().next();
        String propiedad = order.getProperty();
        if (!CAMPOS_ORDENABLES.contains(propiedad)) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortFechaNullsFirst);
        }
        if ("fechaEmision".equals(propiedad)) {
            Sort.Order conNulls = new Sort.Order(order.getDirection(), "fechaEmision").nullsFirst();
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(conNulls));
        }
        return pageable;
    }

    private static boolean ordenaPorCodigo(Pageable pageable) {
        return pageable != null
                && pageable.getSort() != null
                && pageable.getSort().isSorted()
                && pageable.getSort().getOrderFor("codigo") != null;
    }

    private static boolean ordenaPorFechaEmision(Pageable pageable) {
        return pageable == null
                || pageable.getSort() == null
                || !pageable.getSort().isSorted()
                || pageable.getSort().getOrderFor("fechaEmision") != null;
    }

    private Page<Programa> rebanarListaOrdenada(List<Programa> ordenados, Pageable pageable) {
        int total = ordenados.size();
        int size = Math.max(1, pageable.getPageSize());
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
        int pageNum = Math.max(0, pageable.getPageNumber());
        if (totalPages > 0 && pageNum >= totalPages) {
            pageNum = totalPages - 1;
        }
        int from = Math.min(pageNum * size, total);
        int to = Math.min(from + size, total);
        List<Programa> slice = ordenados.subList(from, to);
        return new org.springframework.data.domain.PageImpl<>(slice,
                PageRequest.of(pageNum, size, pageable.getSort()), total);
    }

    private Page<Programa> paginarPorCodigoNumerico(Pageable pageable) {
        List<Programa> ordenados = new ArrayList<>(programaRepository.findAll());
        ordenados.sort(COMPARADOR_CODIGO_NUMERICO);
        Sort.Order order = pageable.getSort() != null ? pageable.getSort().getOrderFor("codigo") : null;
        if (order != null && order.getDirection() == Sort.Direction.DESC) {
            java.util.Collections.reverse(ordenados);
        }
        return rebanarListaOrdenada(ordenados, pageable);
    }

    private Page<Programa> paginarPorFechaEmision(Pageable pageable) {
        List<Programa> ordenados = new ArrayList<>(programaRepository.findAll());
        Sort.Order order = pageable.getSort() != null ? pageable.getSort().getOrderFor("fechaEmision") : null;
        Sort.Direction direction = order != null ? order.getDirection() : Sort.Direction.DESC;
        ordenados.sort(comparadorFechaEmision(direction));
        return rebanarListaOrdenada(ordenados, pageable);
    }

    /** Comparación numérica ascendente: 1, 2, 10…463. Textos no numéricos al final. El listado usa DESC por defecto. */
    static int compararCodigoNumerico(String codigoA, String codigoB) {
        Long numA = codigoEntero(codigoA);
        Long numB = codigoEntero(codigoB);
        if (numA != null && numB != null) {
            return Long.compare(numA, numB);
        }
        if (numA != null) {
            return -1;
        }
        if (numB != null) {
            return 1;
        }
        String a = codigoA == null ? "" : codigoA.trim();
        String b = codigoB == null ? "" : codigoB.trim();
        return a.compareToIgnoreCase(b);
    }

    public Optional<Programa> findById(Long id) {
        return programaRepository.findById(id);
    }

    public Optional<ProgramaDTO> findByIdDTO(Long id) {
        return programaRepository.findById(id)
                .map(this::convertToDTO);
    }

    public Programa create(Programa programa) {
        aplicarDuracionObjetivoPorDefecto(programa);
        programa.actualizarEstado();
        return guardarNuevoConCodigo(programa);
    }

    public ProgramaDTO createFromDTO(ProgramaDTO programaDTO) {
        Programa programa = convertToEntity(programaDTO);
        aplicarDuracionObjetivoPorDefecto(programa);
        programa.actualizarEstado();
        return convertToDTO(guardarNuevoConCodigo(programa));
    }

    /** Sugerencia para el formulario. El número definitivo se calcula otra vez al crear. */
    public String siguienteCodigoSugerido() {
        return String.valueOf(siguienteCodigoNumerico());
    }

    public Programa update(Long id, Programa programa) {
        programa.setId(id);
        programa.actualizarEstado();
        return programaRepository.save(programa);
    }

    public ProgramaDTO updateFromDTO(Long id, ProgramaDTO programaDTO) {
        Programa programa = convertToEntity(programaDTO);
        programa.setId(id);
        validarCodigo(programa.getCodigo(), id);
        programa.actualizarEstado();
        Programa saved = programaRepository.save(programa);
        return convertToDTO(saved);
    }

    @Transactional
    public ProgramaDTO updateCampo(Long id, Map<String, Object> campo) {
        Optional<Programa> optionalPrograma = programaRepository.findById(id);
        if (optionalPrograma.isPresent()) {
            Programa programa = optionalPrograma.get();
            Programa.EstadoPrograma estadoAnterior = programa.getEstado();
            boolean estadoActualizadoExplicitamente = false;
            boolean soloCambiaEstado = campo.keySet().stream().allMatch("estado"::equals);
            if (estadoAnterior == Programa.EstadoPrograma.emitido && !soloCambiaEstado && !esAdmin()) {
                throw new IllegalArgumentException(
                    "Un programa emitido no se puede editar. Cambia el estado para desbloquearlo.");
            }
            
            for (Map.Entry<String, Object> entry : campo.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                switch (key) {
                    case "codigo":
                        String codigo = normalizarCodigo(value != null ? value.toString() : null);
                        validarCodigo(codigo, id);
                        programa.setCodigo(codigo);
                        break;
                    case "temporada":
                        if (value != null) {
                            programa.setTemporada(Integer.parseInt(value.toString()));
                        }
                        break;
                    case "totalPremios":
                        programa.setTotalPremios(value != null ? new BigDecimal(value.toString()) : null);
                        break;
                    case "creditosEspeciales":
                        programa.setCreditosEspeciales((String) value);
                        break;
                    case "notas":
                        programa.setNotas((String) value);
                        break;
                    case "resultadoAcumulado":
                        programa.setResultadoAcumulado(value != null ? new BigDecimal(value.toString()) : null);
                        break;
                    case "duracionAcumulada":
                        if (value != null && !value.toString().isEmpty()) {
                            try {
                                programa.setDuracionAcumulada(java.time.LocalTime.parse(value.toString()));
                            } catch (Exception e) {
                                programa.setDuracionAcumulada(null);
                            }
                        } else {
                            programa.setDuracionAcumulada(null);
                        }
                        break;
                    case "fechaEmision":
                        if (value != null && !value.toString().isEmpty()) {
                            try {
                                programa.setFechaEmision(java.time.LocalDate.parse(value.toString()));
                            } catch (Exception e) {
                                programa.setFechaEmision(null);
                            }
                        } else {
                            programa.setFechaEmision(null);
                        }
                        break;
                    case "estado":
                        if (value != null && !value.toString().isEmpty()) {
                            Programa.EstadoPrograma nuevoEstado;
                            try {
                                nuevoEstado = Programa.EstadoPrograma.valueOf(value.toString());
                            } catch (Exception e) {
                                // Si el estado no es válido, no cambiar el estado actual
                                break;
                            }
                            if (nuevoEstado == Programa.EstadoPrograma.editado) {
                                exigirConcursantesEditados(id);
                            }
                            programa.setEstado(nuevoEstado);
                            estadoActualizadoExplicitamente = true;
                        }
                        break;
                }
            }
            
            // Solo recalcular automáticamente si NO se ha actualizado el estado explícitamente
            if (!estadoActualizadoExplicitamente) {
                programa.actualizarEstado();
            }
            
            Programa saved = programaRepository.save(programa);
            sincronizarEstadoConcursantesConPrograma(saved, estadoAnterior);
            return convertToDTO(saved);
        }
        throw new RuntimeException("Programa no encontrado con id: " + id);
    }

    private void exigirConcursantesEditados(Long programaId) {
        List<ConcursanteDTO> concursantes = concursanteService.findByProgramaId(programaId);
        if (concursantes == null || concursantes.isEmpty()) {
            return;
        }
        List<Integer> numeros = new ArrayList<>();
        int sinNumero = 0;
        for (ConcursanteDTO concursante : concursantes) {
            if (EstadoEditadoPrograma.esEditado(concursante.getEstado())) {
                continue;
            }
            if (concursante.getNumeroConcursante() == null) {
                sinNumero++;
            } else {
                numeros.add(concursante.getNumeroConcursante());
            }
        }
        EstadoEditadoPrograma.exigir(numeros, sinNumero);
    }

    private void sincronizarEstadoConcursantesConPrograma(Programa programa, Programa.EstadoPrograma estadoAnterior) {
        if (programa == null || programa.getId() == null || programa.getEstado() == estadoAnterior) {
            return;
        }
        List<Concursante> concursantes = concursanteRepository.findByNumeroPrograma(programa.getId().intValue());
        if (concursantes.isEmpty()) {
            return;
        }
        boolean cambio = false;
        if (programa.getEstado() == Programa.EstadoPrograma.emitido) {
            for (Concursante concursante : concursantes) {
                if (!"emitido".equalsIgnoreCase(concursante.getEstado())) {
                    concursante.setEstado("emitido");
                    cambio = true;
                }
            }
        } else if (estadoAnterior == Programa.EstadoPrograma.emitido) {
            for (Concursante concursante : concursantes) {
                if ("emitido".equalsIgnoreCase(concursante.getEstado())) {
                    concursante.setEstado("programado");
                    cambio = true;
                }
            }
        }
        if (cambio) {
            concursanteRepository.saveAll(concursantes);
        }
    }

    public void delete(Long id) {
        // Verificar que el programa existe
        Optional<Programa> programaOpt = programaRepository.findById(id);
        if (programaOpt.isEmpty()) {
            throw new IllegalArgumentException("Programa con ID " + id + " no encontrado");
        }

        Programa programa = programaOpt.get();

        // Verificar dependencias - no se puede eliminar si hay concursantes asignados
        Long concursantesCount = ((Number) entityManager.createNativeQuery(
            "SELECT COUNT(DISTINCT c.id) FROM concursantes c "
            + "LEFT JOIN programas_concursantes pc ON pc.concursante_id = c.id AND pc.programa_id = :programaId "
            + "WHERE c.numero_programa = :numeroPrograma OR pc.id IS NOT NULL")
            .setParameter("programaId", programa.getId())
            .setParameter("numeroPrograma", programa.getId() != null ? programa.getId().intValue() : null)
            .getSingleResult()).longValue();
        
        if (concursantesCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar el programa " + 
                programa.getId() + " porque tiene " + concursantesCount + 
                " concursante(s) asignado(s). Desasigna los concursantes primero.");
        }

        // Verificar estado del programa - no eliminar si está en grabación o finalizado (Admin sí puede)
        if (!esAdmin() && programa.getEstado() == Programa.EstadoPrograma.programado) {
            throw new IllegalArgumentException("No se puede eliminar un programa que ya está programado. " +
                "Cambia su estado a 'borrador' primero.");
        }
        
        if (!esAdmin() && programa.getEstado() == Programa.EstadoPrograma.emitido) {
            throw new IllegalArgumentException("No se puede eliminar un programa que ya ha sido emitido. " +
                "Los programas emitidos no pueden ser eliminados.");
        }

        // UNDO: capturar la fila completa antes de borrar (se reinsertará con el mismo id)
        Map<String, Object> filaPrograma = undoService.snapshotFila("programas", id);

        // Si llegamos aquí, es seguro eliminar
        programaRepository.deleteById(id);

        if (filaPrograma != null) {
            undoService.registrar("eliminar_programa",
                "Eliminar programa " + (programa.getCodigo() != null ? programa.getCodigo() : id),
                java.util.Collections.singletonList(UndoService.accionInsertarFila("programas", filaPrograma)));
        }
    }

    public String getDuracionObjetivo() {
        return configuracionService.getDuracionObjetivo();
    }

    public void updateDuracionObjetivo(Long id, String duracionObjetivo) {
        Optional<Programa> optionalPrograma = programaRepository.findById(id);
        if (optionalPrograma.isPresent()) {
            Programa programa = optionalPrograma.get();
            if (programa.getEstado() == Programa.EstadoPrograma.emitido && !esAdmin()) {
                throw new IllegalArgumentException(
                    "Un programa emitido no se puede editar. Cambia el estado para desbloquearlo.");
            }
            programa.setDuracionObjetivo(duracionObjetivo);
            programaRepository.save(programa);
        } else {
            throw new IllegalArgumentException("Programa con ID " + id + " no encontrado");
        }
    }

    private boolean esAdmin() {
        return authorizationService != null && authorizationService.isAdmin();
    }

    private void aplicarDuracionObjetivoPorDefecto(Programa programa) {
        if (programa.getDuracionObjetivo() == null || programa.getDuracionObjetivo().trim().isEmpty()) {
            programa.setDuracionObjetivo(configuracionService.getDuracionObjetivo());
        }
    }

    private Programa guardarNuevoConCodigo(Programa programa) {
        synchronized (asignacionCodigoLock) {
            asignarCodigoPorDefectoSiFalta(programa);
            validarCodigoNuevo(programa.getCodigo());
            return programaRepository.save(programa);
        }
    }

    private void asignarCodigoPorDefectoSiFalta(Programa programa) {
        if (programa.getCodigo() == null || programa.getCodigo().trim().isEmpty()) {
            programa.setCodigo(String.valueOf(siguienteCodigoNumerico()));
        }
    }

    /**
     * Último código de emisión que es un entero, más uno.
     * Ignora textos (APLAZADO, etc.). No consulta programas_repetidos.
     */
    private long siguienteCodigoNumerico() {
        List<String> codigos = programaRepository.findAllCodigos();
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
            throw new IllegalArgumentException("No hay un número de programa disponible.");
        }
        return maximo + 1L;
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

    private void validarCodigoNuevo(String codigo) {
        String normalizado = normalizarCodigo(codigo);
        if (normalizado == null) {
            return;
        }
        if (programaRepository.findByCodigo(normalizado).isPresent()) {
            throw new IllegalArgumentException("Ya existe un programa con el código " + normalizado + ".");
        }
    }

    private void validarCodigo(String codigo, Long id) {
        String normalizado = normalizarCodigo(codigo);
        if (normalizado == null) {
            throw new IllegalArgumentException("El código de programa es obligatorio.");
        }
        if (programaRepository.existsByCodigoAndIdNot(normalizado, id)) {
            throw new IllegalArgumentException("Ya existe un programa con el código " + normalizado + ".");
        }
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return null;
        }
        String normalizado = codigo.trim();
        if (normalizado.length() > 32) {
            throw new IllegalArgumentException("El código de programa no puede superar 32 caracteres.");
        }
        return normalizado;
    }

    private ProgramaDTO convertToDTO(Programa programa) {
        ProgramaDTO dto = new ProgramaDTO();
        dto.setId(programa.getId());
        dto.setCodigo(programa.getCodigo());
        dto.setTemporada(programa.getTemporada());
        dto.setDuracionAcumulada(programa.getDuracionAcumulada());
        dto.setDuracionObjetivo(programa.getDuracionObjetivo());
        dto.setResultadoAcumulado(programa.getResultadoAcumulado());
        dto.setFechaEmision(programa.getFechaEmision());
        dto.setDatoAudienciaShare(programa.getDatoAudienciaShare());
        dto.setDatoAudienciaTarget(programa.getDatoAudienciaTarget());
        dto.setEstado(programa.getEstado() != null ? programa.getEstado().toString() : null);
        dto.setTotalPremios(programa.getTotalPremios());
        dto.setGap(programa.getGap());
        dto.setTotalConcursantes(programa.getTotalConcursantes());
        dto.setCreditosEspeciales(programa.getCreditosEspeciales());
        dto.setNotas(programa.getNotas());
        dto.setVersion(programa.getVersion());
        return dto;
    }

    private Programa convertToEntity(ProgramaDTO dto) {
        Programa programa = new Programa();
        programa.setId(dto.getId());
        programa.setCodigo(normalizarCodigo(dto.getCodigo()));
        programa.setVersion(dto.getVersion());
        programa.setTemporada(dto.getTemporada());
        programa.setDuracionAcumulada(dto.getDuracionAcumulada());
        programa.setDuracionObjetivo(dto.getDuracionObjetivo());
        programa.setResultadoAcumulado(dto.getResultadoAcumulado());
        programa.setFechaEmision(dto.getFechaEmision());
        programa.setDatoAudienciaShare(dto.getDatoAudienciaShare());
        programa.setDatoAudienciaTarget(dto.getDatoAudienciaTarget());
        if (dto.getEstado() != null) {
            try {
                programa.setEstado(Programa.EstadoPrograma.valueOf(dto.getEstado()));
            } catch (IllegalArgumentException e) {
                programa.setEstado(Programa.EstadoPrograma.borrador);
            }
        }
        programa.setTotalPremios(dto.getTotalPremios());
        programa.setGap(dto.getGap());
        programa.setTotalConcursantes(dto.getTotalConcursantes());
        programa.setCreditosEspeciales(dto.getCreditosEspeciales());
        programa.setNotas(dto.getNotas());
        return programa;
    }
} 