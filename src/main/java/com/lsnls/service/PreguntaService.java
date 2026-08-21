package com.lsnls.service;

import com.lsnls.entity.Pregunta;
import com.lsnls.entity.Pregunta.EstadoPregunta;
import com.lsnls.entity.Pregunta.EstadoDisponibilidad;
import com.lsnls.entity.Pregunta.NivelPregunta;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.repository.PreguntaComboRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.lsnls.dto.PreguntaDTO;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class PreguntaService {
    
    private static final Logger log = LoggerFactory.getLogger(PreguntaService.class);
    
    @Autowired
    private PreguntaRepository preguntaRepository;
    
    @Autowired
    private DataTransformationService dataTransformationService;
    
    @Autowired
    private PreguntaComboRepository preguntaComboRepository;

    @Autowired
    private UndoService undoService;

    @Autowired
    private EntityManager entityManager;
    
    @Autowired(required = false)
    private UsuarioService usuarioService; // no usado actualmente

    public Pregunta crear(Pregunta pregunta) {
        // Transformar datos automáticamente a mayúsculas y limpiar
        pregunta.setPregunta(dataTransformationService.normalizarPregunta(pregunta.getPregunta()));
        pregunta.setRespuesta(dataTransformationService.normalizarRespuesta(pregunta.getRespuesta()));
        pregunta.setTematica(dataTransformationService.normalizarTematica(pregunta.getTematica()));
        
        // Validar datos transformados
        DataTransformationService.ValidationResult validation = 
            dataTransformationService.validarPreguntaCompleta(
                pregunta.getPregunta(), 
                pregunta.getRespuesta(), 
                pregunta.getTematica()
            );
        
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Datos no válidos: " + validation.getErrorsAsString());
        }
        
        pregunta.setFechaCreacion(LocalDateTime.now());
        if (pregunta.getEstado() == null) {
            pregunta.setEstado(EstadoPregunta.borrador);
        }
        if (pregunta.getEstadoDisponibilidad() == null) {
            pregunta.setEstadoDisponibilidad(EstadoDisponibilidad.disponible);
        }
        return preguntaRepository.save(pregunta);
    }

    /**
     * Comprueba que la pregunta tiene los campos mínimos para pasar a "para verificar".
     */
    public void validarRequisitosParaVerificar(String tematica, String pregunta, String respuesta, String fuentes) {
        List<String> faltantes = new ArrayList<>();
        if (tematica == null || tematica.trim().isEmpty()) {
            faltantes.add("temática");
        }
        if (pregunta == null || pregunta.trim().isEmpty()) {
            faltantes.add("pregunta");
        }
        if (respuesta == null || respuesta.trim().isEmpty()) {
            faltantes.add("respuesta");
        }
        if (fuentes == null || fuentes.trim().isEmpty()) {
            faltantes.add("fuente");
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                "Para pasar a 'para verificar' son obligatorios: " + String.join(", ", faltantes));
        }
    }

    /**
     * Restaura una pregunta eliminada recreándola con el mismo ID del snapshot (undo).
     */
    public Pregunta restaurarDesdeSnapshot(PreguntaDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Snapshot de pregunta vacío");
        }
        if (dto.getId() == null) {
            throw new IllegalArgumentException("El ID es obligatorio para restaurar la pregunta");
        }
        if (preguntaRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("Ya existe una pregunta con ID " + dto.getId());
        }
        if (dto.getNivel() == null) {
            throw new IllegalArgumentException("El nivel es obligatorio para restaurar");
        }
        if (dto.getTematica() == null || dto.getTematica().trim().isEmpty()) {
            throw new IllegalArgumentException("La temática es obligatoria para restaurar");
        }
        if (dto.getPregunta() == null || dto.getPregunta().trim().isEmpty()) {
            throw new IllegalArgumentException("El texto de la pregunta es obligatorio para restaurar");
        }
        if (dto.getRespuesta() == null || dto.getRespuesta().trim().isEmpty()) {
            throw new IllegalArgumentException("La respuesta es obligatoria para restaurar");
        }

        Long id = dto.getId();
        String tematica = dataTransformationService.normalizarTematica(dto.getTematica());
        String pregunta = dataTransformationService.normalizarPregunta(dto.getPregunta());
        String respuesta = dataTransformationService.normalizarRespuesta(dto.getRespuesta());

        DataTransformationService.ValidationResult validation =
            dataTransformationService.validarPreguntaCompleta(pregunta, respuesta, tematica);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Datos no válidos: " + validation.getErrorsAsString());
        }

        EstadoPregunta estado = EstadoPregunta.borrador;
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            estado = EstadoPregunta.valueOf(dto.getEstado().trim());
        }
        EstadoDisponibilidad estadoDisponibilidad = estado == EstadoPregunta.usada
            ? EstadoDisponibilidad.usada
            : EstadoDisponibilidad.disponible;

        LocalDateTime fechaCreacion = parseSnapshotDateTime(dto.getFechaCreacion()).orElse(LocalDateTime.now());
        LocalDateTime fechaVerificacion = parseSnapshotDateTime(dto.getFechaVerificacion()).orElse(null);
        Long version = dto.getVersion() != null ? dto.getVersion() : 0L;

        entityManager.createNativeQuery(
            "INSERT INTO preguntas (" +
            "id, version, creacion_usuario_id, fecha_creacion, fecha_verificacion, " +
            "verificacion_usuario_id, respuesta, tematica, pregunta, subtema, " +
            "datos_extra, fuentes, autor, notas, notas_verificacion, notas_direccion, " +
            "verificacion, estado, estado_disponibilidad, factor, nivel" +
            ") VALUES (" +
            ":id, :version, :creacionUsuarioId, :fechaCreacion, :fechaVerificacion, " +
            ":verificacionUsuarioId, :respuesta, :tematica, :pregunta, :subtema, " +
            ":datosExtra, :fuentes, :autor, :notas, :notasVerificacion, :notasDireccion, " +
            ":verificacion, :estado, :estadoDisponibilidad, :factor, :nivel" +
            ")"
        )
        .setParameter("id", id)
        .setParameter("version", version)
        .setParameter("creacionUsuarioId", dto.getCreacionUsuarioId())
        .setParameter("fechaCreacion", Timestamp.valueOf(fechaCreacion))
        .setParameter("fechaVerificacion", fechaVerificacion != null ? Timestamp.valueOf(fechaVerificacion) : null)
        .setParameter("verificacionUsuarioId", null)
        .setParameter("respuesta", respuesta)
        .setParameter("tematica", tematica)
        .setParameter("pregunta", pregunta)
        .setParameter("subtema", dto.getSubtema())
        .setParameter("datosExtra", dto.getDatosExtra())
        .setParameter("fuentes", dto.getFuentes())
        .setParameter("autor", dto.getAutor())
        .setParameter("notas", dto.getNotas())
        .setParameter("notasVerificacion", dto.getNotasVerificacion())
        .setParameter("notasDireccion", dto.getNotasDireccion())
        .setParameter("verificacion", dto.getVerificacion())
        .setParameter("estado", estado.name())
        .setParameter("estadoDisponibilidad", estadoDisponibilidad.name())
        .setParameter("factor", dto.getFactor() != null ? dto.getFactor().name() : null)
        .setParameter("nivel", dto.getNivel().name())
        .executeUpdate();

        sincronizarAutoIncrementPreguntas();
        entityManager.flush();
        entityManager.clear();

        return preguntaRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("No se pudo cargar la pregunta restaurada con ID " + id));
    }

    private Optional<LocalDateTime> parseSnapshotDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(raw));
        } catch (DateTimeParseException e) {
            try {
                return Optional.of(LocalDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } catch (DateTimeParseException e2) {
                log.warn("[RESTAURAR] Fecha no parseable: '{}'", raw);
                return Optional.empty();
            }
        }
    }

    private void sincronizarAutoIncrementPreguntas() {
        Number maxId = (Number) entityManager.createNativeQuery(
            "SELECT COALESCE(MAX(id), 0) FROM preguntas"
        ).getSingleResult();
        long nextId = maxId.longValue() + 1;
        entityManager.createNativeQuery(
            "ALTER TABLE preguntas AUTO_INCREMENT = " + nextId
        ).executeUpdate();
    }

    private Optional<Usuario> obtenerUsuarioActual() {
        try {
            org.springframework.security.core.Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()) && usuarioService != null) {
                return usuarioService.obtenerPorNombre(auth.getName());
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public List<Pregunta> obtenerTodas() {
        return preguntaRepository.findAll();
    }

    public Page<Pregunta> obtenerPaginadas(Pageable pageable) {
        return preguntaRepository.findAll(pageable);
    }

    public Optional<Pregunta> obtenerPorId(Long id) {
        return preguntaRepository.findById(id);
    }

    public List<Pregunta> obtenerPorEstado(EstadoPregunta estado) {
        return preguntaRepository.findByEstado(estado);
    }

    public List<Pregunta> obtenerPorNivel(NivelPregunta nivel) {
        return preguntaRepository.findByNivel(nivel);
    }

    public List<Pregunta> obtenerDisponibles() {
        return preguntaRepository.findByEstadoAndEstadoDisponibilidad(
            EstadoPregunta.aprobada, EstadoDisponibilidad.disponible);
    }

    public List<Pregunta> buscarPorTematica(String tematica) {
        return preguntaRepository.findByTematicaContainingIgnoreCase(tematica);
    }

    public List<Pregunta> obtenerPorEstadoYNivel(EstadoPregunta estado, NivelPregunta nivel) {
        return preguntaRepository.findByEstadoAndNivel(estado, nivel);
    }

    public Pregunta actualizar(Long id, Pregunta pregunta) {
        System.out.println("🔍 [BACKEND] Iniciando actualización de pregunta ID: " + id);
        System.out.println("🔍 [BACKEND] Datos recibidos - tematica: " + pregunta.getTematica() + ", verificacion: " + pregunta.getVerificacion());
        System.out.println("🔍 [BACKEND] notasVerificacion recibidas: '" + pregunta.getNotasVerificacion() + "'");
        
        if (preguntaRepository.existsById(id)) {
            // Obtener pregunta existente PRIMERO
            Pregunta preguntaExistente = preguntaRepository.findById(id).orElse(null);
            if (preguntaExistente == null) {
                return null;
            }
            
            System.out.println("📥 [BACKEND] Pregunta existente - tematica: " + preguntaExistente.getTematica() + ", verificacion: " + preguntaExistente.getVerificacion());
            System.out.println("📥 [BACKEND] notasVerificacion existentes: '" + preguntaExistente.getNotasVerificacion() + "'");
            
            // IMPORTANTE: Solo transformar y actualizar los campos que REALMENTE se enviaron (no son null)
            // Copiar todos los valores existentes al objeto a guardar
            pregunta.setId(id);
            pregunta.setVersion(preguntaExistente.getVersion());
            pregunta.setCreacionUsuario(preguntaExistente.getCreacionUsuario());
            pregunta.setFechaCreacion(preguntaExistente.getFechaCreacion());
            pregunta.setVerificacionUsuario(preguntaExistente.getVerificacionUsuario());
            pregunta.setFechaVerificacion(preguntaExistente.getFechaVerificacion());
            pregunta.setEstadoDisponibilidad(preguntaExistente.getEstadoDisponibilidad());
            pregunta.setFactor(preguntaExistente.getFactor());
            pregunta.setNotas(preguntaExistente.getNotas());
            
            // Solo actualizar campos específicos si vienen en la petición
            if (pregunta.getPregunta() == null) {
                pregunta.setPregunta(preguntaExistente.getPregunta());
            } else {
                pregunta.setPregunta(dataTransformationService.normalizarPregunta(pregunta.getPregunta()));
            }
            
            if (pregunta.getRespuesta() == null) {
                pregunta.setRespuesta(preguntaExistente.getRespuesta());
            } else {
                pregunta.setRespuesta(dataTransformationService.normalizarRespuesta(pregunta.getRespuesta()));
            }
            
            if (pregunta.getTematica() == null) {
                pregunta.setTematica(preguntaExistente.getTematica());
            } else {
                pregunta.setTematica(dataTransformationService.normalizarTematica(pregunta.getTematica()));
            }
            
            if (pregunta.getSubtema() == null) {
                pregunta.setSubtema(preguntaExistente.getSubtema());
            }
            
            if (pregunta.getNivel() == null) {
                pregunta.setNivel(preguntaExistente.getNivel());
            }
            
            if (pregunta.getDatosExtra() == null) {
                pregunta.setDatosExtra(preguntaExistente.getDatosExtra());
            }
            
            if (pregunta.getFuentes() == null) {
                pregunta.setFuentes(preguntaExistente.getFuentes());
            }
            
            if (pregunta.getNotasDireccion() == null) {
                pregunta.setNotasDireccion(preguntaExistente.getNotasDireccion());
            }
            
            // Manejar campo notasVerificacion
            if (pregunta.getNotasVerificacion() == null) {
                pregunta.setNotasVerificacion(preguntaExistente.getNotasVerificacion());
            }
            
            // Manejar campo verificacion - SOLO actualizar si el estado cambia a verificada
            if (pregunta.getEstado() != null && preguntaExistente.getEstado() != null &&
                pregunta.getEstado() == Pregunta.EstadoPregunta.verificada && 
                preguntaExistente.getEstado() != Pregunta.EstadoPregunta.verificada) {
                
                System.out.println("🔄 [BACKEND] Estado cambiado a VERIFICADA, actualizando verificacion...");
                
                // Obtener el usuario actual del contexto de seguridad
                String nombreUsuario = null;
                try {
                    org.springframework.security.core.Authentication auth = 
                        SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                        nombreUsuario = auth.getName();
                    }
                } catch (Exception e) {
                    nombreUsuario = "Usuario";
                }
                
                if (nombreUsuario != null) {
                    String verificacionActual = preguntaExistente.getVerificacion();
                    
                    if (verificacionActual == null || verificacionActual.trim().isEmpty()) {
                        pregunta.setVerificacion(nombreUsuario);
                        System.out.println("🆕 [BACKEND] Nueva verificacion: " + nombreUsuario);
                    } else {
                        if (!verificacionActual.contains(nombreUsuario)) {
                            pregunta.setVerificacion(verificacionActual + ", " + nombreUsuario);
                            System.out.println("📝 [BACKEND] Verificacion actualizada: " + pregunta.getVerificacion());
                        } else {
                            pregunta.setVerificacion(verificacionActual);
                            System.out.println("🔄 [BACKEND] Usuario ya incluido, manteniendo: " + verificacionActual);
                        }
                    }
                }
            } else {
                // Si no cambia a verificada, mantener verificacion existente
                pregunta.setVerificacion(preguntaExistente.getVerificacion());
                System.out.println("🔒 [BACKEND] Estado no cambia a VERIFICADA, manteniendo verificacion: " + preguntaExistente.getVerificacion());
            }
            
            // Validar datos finales
            DataTransformationService.ValidationResult validation = 
                dataTransformationService.validarPreguntaCompleta(pregunta.getPregunta(), pregunta.getRespuesta(), pregunta.getTematica());
            
            if (!validation.isValid()) {
                throw new IllegalArgumentException("Datos no válidos: " + validation.getErrorsAsString());
            }
            
            Pregunta resultado = preguntaRepository.save(pregunta);
            System.out.println("💾 [BACKEND] Pregunta guardada - ID: " + resultado.getId() + ", tematica: " + resultado.getTematica() + ", verificacion: " + resultado.getVerificacion());
            return resultado;
        }
        return null;
    }

    public Pregunta cambiarEstado(Long id, EstadoPregunta nuevoEstado) {
        return cambiarEstado(id, nuevoEstado, null);
    }

    public Pregunta cambiarEstado(Long id, EstadoPregunta nuevoEstado, Usuario usuarioActual) {
        return preguntaRepository.findById(id).map(pregunta -> {
            pregunta.setEstado(nuevoEstado);
            if (nuevoEstado == EstadoPregunta.verificada) {
                pregunta.setFechaVerificacion(LocalDateTime.now());
                if (usuarioActual != null) {
                    pregunta.setVerificacionUsuario(usuarioActual);
                }
            }
            if (nuevoEstado == EstadoPregunta.aprobada) {
                pregunta.setEstadoDisponibilidad(EstadoDisponibilidad.disponible);
                System.out.println("✅ Pregunta ID " + id + " aprobada y marcada como DISPONIBLE");
            }
            return preguntaRepository.save(pregunta);
        }).orElse(null);
    }

    /**
     * Cambia el estado de una pregunta de forma atómica con validación de concurrencia
     * @param id ID de la pregunta
     * @param estadoActualEsperado Estado actual esperado
     * @param nuevoEstado Nuevo estado a asignar
     * @param usuarioActual Usuario que realiza el cambio (opcional)
     * @return true si el estado fue cambiado exitosamente
     * @throws IllegalStateException si otro usuario modificó la pregunta simultáneamente
     */
    @Transactional
    public boolean cambiarEstadoAtomico(Long id, EstadoPregunta estadoActualEsperado, 
                                       EstadoPregunta nuevoEstado, Usuario usuarioActual) {

        if (nuevoEstado == EstadoPregunta.para_verificar
            && estadoActualEsperado != EstadoPregunta.para_verificar) {
            Pregunta pregunta = preguntaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta con ID " + id + " no encontrada"));
            validarRequisitosParaVerificar(
                pregunta.getTematica(), pregunta.getPregunta(), pregunta.getRespuesta(), pregunta.getFuentes());
        }
        
        // Construir query base
        StringBuilder query = new StringBuilder("UPDATE preguntas SET estado = ?");
        List<Object> parametros = new ArrayList<>();
        parametros.add(nuevoEstado.name());
        
        int paramIndex = 2; // mantenido para claridad aunque no se usa después
        
        // Agregar campos adicionales según el nuevo estado
        if (nuevoEstado == EstadoPregunta.verificada) {
            query.append(", fecha_verificacion = ?");
            parametros.add(java.sql.Timestamp.valueOf(LocalDateTime.now()));
            paramIndex++;
            
            if (usuarioActual != null) {
                query.append(", verificacion_usuario_id = ?");
                parametros.add(usuarioActual.getId());
                paramIndex++;
                
                // Actualizar también el campo verificacion con el nombre del usuario
                query.append(", verificacion = ?");
                
                // Primero obtener el valor actual del campo verificacion
                String verificacionActual = null;
                try {
                    Object result = entityManager.createNativeQuery("SELECT verificacion FROM preguntas WHERE id = ?")
                        .setParameter(1, id)
                        .getSingleResult();
                    verificacionActual = result != null ? result.toString() : null;
                } catch (Exception e) {
                    // Si hay error, dejar verificacionActual como null
                    System.out.println("No se pudo obtener el valor actual de verificacion: " + e.getMessage());
                }
                
                // Construir el nuevo valor de verificacion
                String nuevoValorVerificacion;
                if (verificacionActual == null || verificacionActual.trim().isEmpty()) {
                    nuevoValorVerificacion = usuarioActual.getNombre();
                } else {
                    // Verificar si el usuario ya está incluido
                    if (!verificacionActual.contains(usuarioActual.getNombre())) {
                        nuevoValorVerificacion = verificacionActual + ", " + usuarioActual.getNombre();
                    } else {
                        nuevoValorVerificacion = verificacionActual;
                    }
                }
                
                parametros.add(nuevoValorVerificacion);
                paramIndex++;
                
                System.out.println("✅ Actualizando campo verificacion a: " + nuevoValorVerificacion);
            }
        }
        
        if (nuevoEstado == EstadoPregunta.aprobada) {
            query.append(", estado_disponibilidad = ?");
            parametros.add(EstadoDisponibilidad.disponible.name());
            paramIndex++;
        }
        
        // Agregar condiciones WHERE con verificación de estado
        query.append(" WHERE id = ? AND estado = ?");
        parametros.add(id);
        parametros.add(estadoActualEsperado.name());
        
        // Ejecutar query nativa atómica
        javax.persistence.Query nativeQuery = entityManager.createNativeQuery(query.toString());
        for (int i = 0; i < parametros.size(); i++) {
            nativeQuery.setParameter(i + 1, parametros.get(i));
        }
        
        int filasActualizadas = nativeQuery.executeUpdate();
        
        if (filasActualizadas == 0) {
            throw new IllegalStateException("No se pudo cambiar el estado de la pregunta " + id + 
                " porque otro usuario la modificó simultáneamente. Estado esperado: " + estadoActualEsperado);
        }
        
        if (nuevoEstado == EstadoPregunta.aprobada) {
            System.out.println("✅ Pregunta ID " + id + " aprobada atómicamente y marcada como DISPONIBLE");
        }
        
        return true;
    }

    public Pregunta verificar(Long id, EstadoPregunta nuevoEstado, String notas, Usuario verificador) {
        return preguntaRepository.findById(id).map(pregunta -> {
            pregunta.setEstado(nuevoEstado);
            pregunta.setVerificacionUsuario(verificador);
            pregunta.setFechaVerificacion(LocalDateTime.now());
            if (notas != null && !notas.trim().isEmpty()) {
                pregunta.setNotas(notas);
            }
            return preguntaRepository.save(pregunta);
        }).orElse(null);
    }

    public Pregunta rechazar(Long id, String motivo) {
        return preguntaRepository.findById(id).map(pregunta -> {
            pregunta.setEstado(EstadoPregunta.rechazada);
            if (motivo != null && !motivo.trim().isEmpty()) {
                pregunta.setNotas("RECHAZADA: " + motivo);
            }
            return preguntaRepository.save(pregunta);
        }).orElse(null);
    }

    /**
     * Rechaza una pregunta de forma atómica con validación de concurrencia
     * @param id ID de la pregunta
     * @param estadoActualEsperado Estado actual esperado
     * @param motivo Motivo del rechazo
     * @return true si la pregunta fue rechazada exitosamente
     * @throws IllegalStateException si otro usuario modificó la pregunta simultáneamente
     */
    @Transactional
    public boolean rechazarAtomico(Long id, EstadoPregunta estadoActualEsperado, String motivo) {
        // Construir query con estado y notas
        String query = "UPDATE preguntas SET estado = ?, notas = ? WHERE id = ? AND estado = ?";
        
        String notasRechazo = "RECHAZADA: " + (motivo != null && !motivo.trim().isEmpty() ? motivo : "Sin motivo especificado");
        
        // Ejecutar query nativa atómica
        int filasActualizadas = entityManager.createNativeQuery(query)
            .setParameter(1, EstadoPregunta.rechazada.name())
            .setParameter(2, notasRechazo)
            .setParameter(3, id)
            .setParameter(4, estadoActualEsperado.name())
            .executeUpdate();
        
        if (filasActualizadas == 0) {
            throw new IllegalStateException("No se pudo rechazar la pregunta " + id + 
                " porque otro usuario la modificó simultáneamente. Estado esperado: " + estadoActualEsperado);
        }
        
        return true;
    }

    public Pregunta marcarParaRevisar(Long id, String notas, Usuario usuario) {
        return preguntaRepository.findById(id).map(pregunta -> {
            pregunta.setEstado(EstadoPregunta.revisar);
            pregunta.setVerificacionUsuario(usuario);
            pregunta.setFechaVerificacion(LocalDateTime.now());
            if (notas != null && !notas.trim().isEmpty()) {
                pregunta.setNotasVerificacion("REVISAR: " + notas);
            }
            return preguntaRepository.save(pregunta);
        }).orElse(null);
    }

    public Pregunta marcarParaCorregir(Long id, String notas, Usuario usuario) {
        return preguntaRepository.findById(id).map(pregunta -> {
            pregunta.setEstado(EstadoPregunta.corregir);
            pregunta.setVerificacionUsuario(usuario);
            pregunta.setFechaVerificacion(LocalDateTime.now());
            if (notas != null && !notas.trim().isEmpty()) {
                pregunta.setNotasVerificacion("CORREGIR: " + notas);
            }
            return preguntaRepository.save(pregunta);
        }).orElse(null);
    }

    public Pregunta verificarPregunta(Long id, Long verificadorId, EstadoPregunta nuevoEstado) {
        return preguntaRepository.findById(id).map(pregunta -> {
            pregunta.setEstado(nuevoEstado);
            pregunta.setFechaVerificacion(LocalDateTime.now());
            return preguntaRepository.save(pregunta);
        }).orElse(null);
    }

    public void marcarComoUsada(Long id) {
        preguntaRepository.findById(id).ifPresent(pregunta -> {
            pregunta.setEstadoDisponibilidad(EstadoDisponibilidad.usada);
            // Cambiar el estado de la pregunta a 'usada' si estaba en 'aprobada'
            if (pregunta.getEstado() == EstadoPregunta.aprobada) {
                pregunta.setEstado(EstadoPregunta.usada);
                System.out.println("✅ Pregunta ID " + id + " marcada como USADA");
            }
            preguntaRepository.save(pregunta);
        });
    }

    public void liberarPregunta(Long id) {
        preguntaRepository.findById(id).ifPresent(pregunta -> {
            pregunta.setEstadoDisponibilidad(EstadoDisponibilidad.liberada);
            // Cambiar el estado de la pregunta a 'aprobada' si estaba en 'usada'
            if (pregunta.getEstado() == EstadoPregunta.usada) {
                pregunta.setEstado(EstadoPregunta.aprobada);
                System.out.println("✅ Pregunta ID " + id + " liberada y marcada como APROBADA");
            }
            preguntaRepository.save(pregunta);
        });
    }

    public void eliminar(Long id) {
        // Verificar que la pregunta existe
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(id);
        if (preguntaOpt.isEmpty()) {
            throw new IllegalArgumentException("Pregunta con ID " + id + " no encontrada");
        }

        Pregunta pregunta = preguntaOpt.get(); // usado para validaciones más abajo

        // Verificar si está siendo usada en cuestionarios
        Long cuestionariosCount = entityManager.createQuery(
            "SELECT COUNT(pc) FROM PreguntaCuestionario pc WHERE pc.pregunta.id = :preguntaId", Long.class)
            .setParameter("preguntaId", id)
            .getSingleResult();
        
        if (cuestionariosCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en " + 
                cuestionariosCount + " cuestionario(s). Quítala de los cuestionarios primero.");
        }

        // Verificar si está siendo usada en combos
        Long combosCount = entityManager.createQuery(
            "SELECT COUNT(pc) FROM PreguntaCombo pc WHERE pc.pregunta.id = :preguntaId", Long.class)
            .setParameter("preguntaId", id)
            .getSingleResult();
        
        if (combosCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en " + 
                combosCount + " combo(s). Quítala de los combos primero.");
        }

        // UNDO: capturar la fila completa antes de borrar (se reinsertará con el mismo id)
        Map<String, Object> filaPregunta = undoService.snapshotFila("preguntas", id);

        // Si llegamos aquí, es seguro eliminar
        preguntaRepository.deleteById(id);

        if (filaPregunta != null) {
            undoService.registrar("eliminar_pregunta", "Eliminar pregunta " + id,
                Collections.singletonList(UndoService.accionInsertarFila("preguntas", filaPregunta)));
        }
    }
    
    /**
     * Método específico para validar una pregunta sin guardarla
     */
    public DataTransformationService.ValidationResult validarPregunta(Pregunta pregunta) {
        String preguntaTexto = dataTransformationService.normalizarPregunta(pregunta.getPregunta());
        String respuestaTexto = dataTransformationService.normalizarRespuesta(pregunta.getRespuesta());
        String tematicaTexto = dataTransformationService.normalizarTematica(pregunta.getTematica());
        
        return dataTransformationService.validarPreguntaCompleta(preguntaTexto, respuestaTexto, tematicaTexto);
    }

    public Pregunta actualizarDesdeDTO(Long id, PreguntaDTO dto) {
        Pregunta pregunta = preguntaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada"));

        if (dto.getVersion() != null) {
            pregunta.setVersion(dto.getVersion());
        }
        
        System.out.println("✅ [ACTUALIZAR] Iniciando actualización de pregunta ID: " + id);
        System.out.println("✅ [ACTUALIZAR] Estado actual: " + pregunta.getEstado() + ", Estado solicitado: " + dto.getEstado());
        
        // Proteger el campo de autoría - no permitir modificaciones
        if (dto.getCreacionUsuarioId() != null && !dto.getCreacionUsuarioId().equals(pregunta.getCreacionUsuario().getId())) {
            throw new IllegalArgumentException("No se puede modificar el campo de autoría de una pregunta");
        }

        // Validar cambio de nivel si la pregunta está asignada a un combo
        if (dto.getNivel() != null && !dto.getNivel().equals(pregunta.getNivel())) {
            boolean estaAsignadaACombo = preguntaComboRepository.existsByPreguntaId(id);
            
            if (estaAsignadaACombo) {
                // Verificar si el nuevo nivel es de nivel 5
                boolean esNivel5 = dto.getNivel().name().startsWith("_5");
                
                if (!esNivel5) {
                    throw new IllegalArgumentException("La pregunta está asignada a un combo, solo puede tener nivel 5");
                }
            }
        }

        // Guardar el valor anterior del estado para comparar
        Pregunta.EstadoPregunta estadoAnterior = pregunta.getEstado();
        
        // IMPORTANTE: Manejar explícitamente el cambio de estado
        if (dto.getEstado() != null) {
            try {
                EstadoPregunta nuevoEstado = EstadoPregunta.valueOf(dto.getEstado());
                System.out.println("✅ [ACTUALIZAR] Cambiando estado de " + pregunta.getEstado() + " a " + nuevoEstado);
                pregunta.setEstado(nuevoEstado);
            } catch (IllegalArgumentException e) {
                System.err.println("❌ [ACTUALIZAR] Estado inválido: " + dto.getEstado());
                throw new IllegalArgumentException("Estado inválido: " + dto.getEstado());
            }
        }

        if (dto.getTematica() != null) pregunta.setTematica(dataTransformationService.normalizarTematica(dto.getTematica()));
        if (dto.getPregunta() != null) pregunta.setPregunta(dataTransformationService.normalizarPregunta(dto.getPregunta()));
        if (dto.getRespuesta() != null) pregunta.setRespuesta(dataTransformationService.normalizarRespuesta(dto.getRespuesta()));
        if (dto.getDatosExtra() != null) pregunta.setDatosExtra(dto.getDatosExtra());
        if (dto.getFuentes() != null) pregunta.setFuentes(dto.getFuentes());
        if (dto.getNivel() != null) pregunta.setNivel(dto.getNivel());
        if (dto.getNotas() != null) pregunta.setNotas(dto.getNotas());
        if (dto.getFactor() != null) pregunta.setFactor(dto.getFactor());
        if (dto.getNotasVerificacion() != null) pregunta.setNotasVerificacion(dto.getNotasVerificacion());
        if (dto.getNotasDireccion() != null) pregunta.setNotasDireccion(dto.getNotasDireccion());
        if (dto.getSubtema() != null) pregunta.setSubtema(dto.getSubtema());

        if (pregunta.getEstado() == EstadoPregunta.para_verificar
            && estadoAnterior != EstadoPregunta.para_verificar) {
            validarRequisitosParaVerificar(
                pregunta.getTematica(), pregunta.getPregunta(), pregunta.getRespuesta(), pregunta.getFuentes());
        }
        
        // Manejar actualización del campo verificacion SOLO cuando se cambia a estado verificada
        if (dto.getEstado() != null && 
            EstadoPregunta.verificada.name().equals(dto.getEstado()) && 
            estadoAnterior != EstadoPregunta.verificada) {
            
            System.out.println("✅ [ACTUALIZAR] Estado cambiado a VERIFICADA, actualizando verificacion");
            
            // Obtener el usuario actual del contexto de seguridad
            String nombreUsuario = null;
            try {
                org.springframework.security.core.Authentication auth = 
                    SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    nombreUsuario = auth.getName();
                }
            } catch (Exception e) {
                // Si no se puede obtener el usuario, usar un valor por defecto
                nombreUsuario = "Usuario";
            }
            
            if (nombreUsuario != null) {
                String verificacionActual = pregunta.getVerificacion();
                
                if (verificacionActual == null || verificacionActual.trim().isEmpty()) {
                    // Si no hay verificacion previa, usar solo el nombre del usuario actual
                    pregunta.setVerificacion(nombreUsuario);
                } else {
                    // Si ya hay verificacion previa, agregar el nuevo usuario si no está ya incluido
                    if (!verificacionActual.contains(nombreUsuario)) {
                        pregunta.setVerificacion(verificacionActual + ", " + nombreUsuario);
                    }
                    // Si ya está incluido, mantener la verificacion actual (no hacer nada)
                }
            }
        }
        
        // Validar y guardar
        DataTransformationService.ValidationResult validation = dataTransformationService.validarPreguntaCompleta(
            pregunta.getPregunta(), pregunta.getRespuesta(), pregunta.getTematica()
        );
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Datos no válidos: " + validation.getErrorsAsString());
        }
        
        Pregunta preguntaGuardada = preguntaRepository.save(pregunta);
        System.out.println("✅ [ACTUALIZAR] Pregunta guardada con éxito. Estado final: " + preguntaGuardada.getEstado());
        return preguntaGuardada;
    }

    public Page<Pregunta> buscarPreguntasPaginadas(String nivel, String factor, String id, String pregunta, String respuesta, String tematica, Pageable pageable) {
        Pregunta.NivelPregunta nivelEnum = null;
        Pregunta.FactorPregunta factorEnum = null;
        try {
            if (nivel != null && !nivel.isBlank()) nivelEnum = Pregunta.NivelPregunta.valueOf(nivel.startsWith("_") ? nivel : ("_"+nivel));
        } catch (Exception ignored) {}
        try {
            if (factor != null && !factor.isBlank()) factorEnum = Pregunta.FactorPregunta.valueOf(factor);
        } catch (Exception ignored) {}
        
        log.info("[BUSCAR] Parámetros de búsqueda - Nivel: {}, Factor: {}, ID: {}, Pregunta: {}, Respuesta: {}, Temática: {}", 
            nivel, factor, id, pregunta, respuesta, tematica);
        log.info("[BUSCAR] Enums convertidos - NivelEnum: {}, FactorEnum: {}", nivelEnum, factorEnum);
        
        // TEMPORAL: Usar el método sin filtro de disponibilidad para debug
        Page<Pregunta> resultado = preguntaRepository.buscarPreguntasSinFiltroDisponibilidad(
            nivelEnum,
            factorEnum,
            (id != null && !id.isBlank()) ? id : null,
            (pregunta != null && !pregunta.isBlank()) ? pregunta : null,
            (respuesta != null && !respuesta.isBlank()) ? respuesta : null,
            (tematica != null && !tematica.isBlank()) ? tematica : null,
            Pregunta.EstadoPregunta.aprobada,
            pageable
        );
        
        log.info("[BUSCAR] Resultado - Total elementos: {}, Total páginas: {}, Elementos en esta página: {}", 
            resultado.getTotalElements(), resultado.getTotalPages(), resultado.getContent().size());
        
        return resultado;
    }

    public void eliminarPorId(Long id) {
        // Verificar que la pregunta existe
        Optional<Pregunta> preguntaOpt = preguntaRepository.findById(id);
        if (preguntaOpt.isEmpty()) {
            throw new IllegalArgumentException("Pregunta con ID " + id + " no encontrada");
        }

        Pregunta pregunta = preguntaOpt.get();
        
        // Verificar si la pregunta está en estado "usada"
        if (pregunta.getEstado() == EstadoPregunta.usada || 
            pregunta.getEstadoDisponibilidad() == EstadoDisponibilidad.usada) {
            throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en un cuestionario o combo");
        }
        
        // Verificar si está siendo usada en cuestionarios
        Long cuestionariosCount = entityManager.createQuery(
            "SELECT COUNT(pc) FROM PreguntaCuestionario pc WHERE pc.pregunta.id = :preguntaId", Long.class)
            .setParameter("preguntaId", id)
            .getSingleResult();
        
        if (cuestionariosCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en " + 
                cuestionariosCount + " cuestionario(s). Quítala de los cuestionarios primero.");
        }

        // Verificar si está siendo usada en combos
        Long combosCount = entityManager.createQuery(
            "SELECT COUNT(pc) FROM PreguntaCombo pc WHERE pc.pregunta.id = :preguntaId", Long.class)
            .setParameter("preguntaId", id)
            .getSingleResult();
        
        if (combosCount > 0) {
            throw new IllegalArgumentException("No se puede eliminar la pregunta porque está siendo usada en " + 
                combosCount + " combo(s). Quítala de los combos primero.");
        }

        // UNDO: capturar la fila completa antes de borrar (se reinsertará con el mismo id)
        Map<String, Object> filaPregunta = undoService.snapshotFila("preguntas", id);

        // Si llegamos aquí, es seguro eliminar
        preguntaRepository.deleteById(id);

        if (filaPregunta != null) {
            undoService.registrar("eliminar_pregunta", "Eliminar pregunta " + id,
                Collections.singletonList(UndoService.accionInsertarFila("preguntas", filaPregunta)));
        }
    }

    public List<PreguntaDTO> filtrarPreguntasCompleto(String nivel, String factor, String estado, 
                                                     String tematica, String subtema, String pregunta, String respuesta, String autoria, String texto) {
        Specification<Pregunta> spec = buildFiltrarSpecification(
            parseNivel(nivel), parseFactor(factor), estado,
            tematica, subtema, pregunta, respuesta, autoria, texto);

        List<Pregunta> preguntas = preguntaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
        return preguntas.stream().map(this::mapPreguntaToDTO).collect(Collectors.toList());
    }

    public Page<PreguntaDTO> filtrarPreguntasCompletoPaginado(String nivel, String factor, String estado, 
                                                             String tematica, String subtema, String pregunta, String respuesta, 
                                                             String autoria, String texto, Pageable pageable) {
        log.info("[FILTRAR] Filtros recibidos - estado: {}, tematica: {}, subtema: {}, autoria: {}, nivel: {}, texto: {}",
            estado, tematica, subtema, autoria, nivel, texto);

        Specification<Pregunta> spec = buildFiltrarSpecification(
            parseNivel(nivel), parseFactor(factor), estado,
            tematica, subtema, pregunta, respuesta, autoria, texto);

        Page<Pregunta> page = preguntaRepository.findAll(spec, pageable);
        log.info("[FILTRAR] Resultado tras filtrar - total: {}, pagina: {}, tamPagina: {}",
            page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());

        return page.map(this::mapPreguntaToDTO);
    }

    private Pregunta.NivelPregunta parseNivel(String nivel) {
        if (nivel == null || nivel.isBlank()) {
            return null;
        }
        try {
            return Pregunta.NivelPregunta.valueOf(nivel.startsWith("_") ? nivel : ("_" + nivel));
        } catch (Exception e) {
            log.warn("[FILTRAR] Nivel no reconocido: '{}'", nivel);
            return null;
        }
    }

    private Pregunta.FactorPregunta parseFactor(String factor) {
        if (factor == null || factor.isBlank()) {
            return null;
        }
        try {
            return Pregunta.FactorPregunta.valueOf(factor);
        } catch (Exception e) {
            log.warn("[FILTRAR] Factor no reconocido: '{}'", factor);
            return null;
        }
    }

    private Specification<Pregunta> buildFiltrarSpecification(
            Pregunta.NivelPregunta nivel,
            Pregunta.FactorPregunta factor,
            String estadoRaw,
            String tematica,
            String subtema,
            String pregunta,
            String respuesta,
            String autoria,
            String texto) {

        Specification<Pregunta> spec = Specification.where(null);

        if (nivel != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("nivel"), nivel));
        }
        if (factor != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("factor"), factor));
        }

        if (estadoRaw != null && !estadoRaw.isBlank()) {
            if (estadoRaw.contains(",")) {
                List<Pregunta.EstadoPregunta> estados = new ArrayList<>();
                for (String s : estadoRaw.split(",")) {
                    String v = s.trim();
                    if (!v.isEmpty()) {
                        try {
                            estados.add(parseEstadoPregunta(v));
                        } catch (Exception e) {
                            log.warn("[FILTRAR] Estado CSV no reconocido: '{}'", v);
                        }
                    }
                }
                if (!estados.isEmpty()) {
                    spec = spec.and((root, cq, cb) -> root.get("estado").in(estados));
                }
            } else {
                try {
                    Pregunta.EstadoPregunta estado = parseEstadoPregunta(estadoRaw);
                    spec = spec.and((root, cq, cb) -> cb.equal(root.get("estado"), estado));
                } catch (Exception e) {
                    log.warn("[FILTRAR] Estado no reconocido: '{}'", estadoRaw);
                }
            }
        }

        if (tematica != null && !tematica.isBlank()) {
            final String like = "%" + tematica.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("tematica")), like));
        }
        if (subtema != null && !subtema.isBlank()) {
            final String like = "%" + subtema.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("subtema")), like));
        }
        if (pregunta != null && !pregunta.isBlank()) {
            final String like = "%" + pregunta.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("pregunta")), like));
        }
        if (respuesta != null && !respuesta.isBlank()) {
            final String like = "%" + respuesta.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("respuesta")), like));
        }
        if (autoria != null && !autoria.isBlank()) {
            final String like = "%" + autoria.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> {
                Join<Pregunta, Usuario> creacionUsuario = root.join("creacionUsuario", JoinType.LEFT);
                return cb.like(
                    cb.lower(cb.coalesce(root.get("autor"), cb.coalesce(creacionUsuario.get("nombre"), cb.literal("")))),
                    like
                );
            });
        }
        if (texto != null && !texto.isBlank()) {
            final String like = "%" + texto.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("pregunta")), like),
                cb.like(cb.lower(root.get("respuesta")), like)
            ));
        }

        return spec;
    }

    private Pregunta.EstadoPregunta parseEstadoPregunta(String raw) {
        String normalized = raw.trim().toLowerCase().replace(' ', '_');
        return Pregunta.EstadoPregunta.valueOf(normalized);
    }

    // --- MÉTODO DE MAPEADO DTO ---
    public PreguntaDTO mapPreguntaToDTO(Pregunta p) {
        PreguntaDTO dto = new PreguntaDTO();
        dto.setId(p.getId());
        dto.setTematica(p.getTematica());
        dto.setPregunta(p.getPregunta());
        dto.setRespuesta(p.getRespuesta());
        dto.setDatosExtra(p.getDatosExtra());
        dto.setFuentes(p.getFuentes());
        dto.setNivel(p.getNivel());
        dto.setCreacionUsuarioId(p.getCreacionUsuario() != null ? p.getCreacionUsuario().getId() : null);
        dto.setCreacionUsuarioNombre(p.getCreacionUsuario() != null ? p.getCreacionUsuario().getNombre() : null);
        dto.setSubtema(p.getSubtema());
        dto.setAutor(p.getAutor());
        dto.setNotas(p.getNotas());
        dto.setFactor(p.getFactor());
        dto.setNotasVerificacion(p.getNotasVerificacion());
        dto.setNotasDireccion(p.getNotasDireccion());
        dto.setVerificacion(p.getVerificacion());
        dto.setFechaCreacion(p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null);
        dto.setFechaVerificacion(p.getFechaVerificacion() != null ? p.getFechaVerificacion().toString() : null);
        dto.setEstado(p.getEstado() != null ? p.getEstado().name() : null);
        dto.setVersion(p.getVersion());
        
        return dto;
    }

    // Modificar obtenerTodas para devolver DTOs
    public List<PreguntaDTO> obtenerTodasDTO() {
        return obtenerTodas().stream().map(this::mapPreguntaToDTO).collect(java.util.stream.Collectors.toList());
    }

    // Método para obtener preguntas paginadas como DTOs
    public Page<PreguntaDTO> obtenerPaginadasDTO(Pageable pageable) {
        try {
            log.info("[PAGINADAS] Iniciando consulta paginada - Page: {}, Size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Pregunta> preguntasPage = preguntaRepository.findAll(pageable);
            
            log.info("[PAGINADAS] Consulta exitosa - Total elementos: {}, Total páginas: {}", 
                preguntasPage.getTotalElements(), preguntasPage.getTotalPages());
            
            return preguntasPage.map(this::mapPreguntaToDTO);
        } catch (Exception e) {
            log.error("[PAGINADAS] Error al obtener preguntas paginadas: {}", e.getMessage(), e);
            
            // Log adicional para identificar el problema específico
            if (e.getCause() != null && e.getCause().getMessage().contains("No enum constant")) {
                log.error("[PAGINADAS] Error de enum detectado: {}", e.getCause().getMessage());
                
                // Intentar identificar qué valores problemáticos hay
                try {
                    List<Pregunta> todas = preguntaRepository.findAll();
                    log.info("[PAGINADAS] Total preguntas en BD: {}", todas.size());
                    
                    // Verificar niveles únicos
                    Set<String> niveles = todas.stream()
                        .map(p -> p.getNivel() != null ? p.getNivel().name() : "NULL")
                        .collect(Collectors.toSet());
                    log.info("[PAGINADAS] Niveles únicos encontrados: {}", niveles);
                    
                    // Verificar estados únicos
                    Set<String> estados = todas.stream()
                        .map(p -> p.getEstado() != null ? p.getEstado().name() : "NULL")
                        .collect(Collectors.toSet());
                    log.info("[PAGINADAS] Estados únicos encontrados: {}", estados);
                    
                } catch (Exception ex) {
                    log.error("[PAGINADAS] Error al verificar datos: {}", ex.getMessage());
                }
            }
            
            throw e;
        }
    }

    // Modificar obtenerPorId para devolver DTO
    public Optional<PreguntaDTO> obtenerPorIdDTO(Long id) {
        return obtenerPorId(id).map(this::mapPreguntaToDTO);
    }
    
    /**
     * Busca apariciones de un texto en preguntas y respuestas
     * @param texto Texto a buscar
     * @return Lista de preguntas que contienen el texto en pregunta o respuesta
     */
    public List<PreguntaDTO> buscarApariciones(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String textoLimpio = texto.trim().toLowerCase();
        System.out.println("🔍 Buscando apariciones para: " + textoLimpio);
        
        // Buscar en todas las preguntas (no usar filtro de estado para ser exhaustivo)
        List<Pregunta> todasLasPreguntas = preguntaRepository.findAll();
        
        List<Pregunta> preguntasCoincidentes = todasLasPreguntas.stream()
            .filter(p -> {
                boolean coincidePregunta = p.getPregunta() != null && 
                                         p.getPregunta().toLowerCase().contains(textoLimpio);
                boolean coincideRespuesta = p.getRespuesta() != null && 
                                          p.getRespuesta().toLowerCase().contains(textoLimpio);
                return coincidePregunta || coincideRespuesta;
            })
            .collect(Collectors.toList());
        
        System.out.println("✅ Encontradas " + preguntasCoincidentes.size() + " coincidencias");
        
        // Convertir a DTOs para la respuesta
        return preguntasCoincidentes.stream()
            .map(this::mapPreguntaToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de niveles de preguntas para debug
     * @return Mapa con estadísticas por nivel
     */
    public Map<String, Object> obtenerEstadisticasNiveles() {
        Map<String, Object> estadisticas = new HashMap<>();
        
        List<Pregunta> todasLasPreguntas = preguntaRepository.findAll();
        
        // Agrupar por nivel
        Map<Pregunta.NivelPregunta, Long> porNivel = todasLasPreguntas.stream()
            .collect(Collectors.groupingBy(Pregunta::getNivel, Collectors.counting()));
        
        // Agrupar por estado
        Map<Pregunta.EstadoPregunta, Long> porEstado = todasLasPreguntas.stream()
            .collect(Collectors.groupingBy(Pregunta::getEstado, Collectors.counting()));
        
        // Agrupar por estado de disponibilidad
        Map<Pregunta.EstadoDisponibilidad, Long> porDisponibilidad = todasLasPreguntas.stream()
            .collect(Collectors.groupingBy(Pregunta::getEstadoDisponibilidad, Collectors.counting()));
        
        estadisticas.put("totalPreguntas", todasLasPreguntas.size());
        estadisticas.put("porNivel", porNivel);
        estadisticas.put("porEstado", porEstado);
        estadisticas.put("porDisponibilidad", porDisponibilidad);
        
        log.info("[DEBUG] Estadísticas de niveles: {}", estadisticas);
        
        return estadisticas;
    }
} 