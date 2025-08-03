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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.lsnls.dto.PreguntaDTO;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import com.lsnls.entity.AuditLog;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@Transactional
public class PreguntaService {
    
    @Autowired
    private PreguntaRepository preguntaRepository;
    
    @Autowired
    private DataTransformationService dataTransformationService;
    
    @Autowired
    private PreguntaComboRepository preguntaComboRepository;

    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private UsuarioService usuarioService;

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
        pregunta.setEstado(EstadoPregunta.borrador);
        pregunta.setEstadoDisponibilidad(EstadoDisponibilidad.disponible);
        return preguntaRepository.save(pregunta);
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
            pregunta.setEstado(preguntaExistente.getEstado());
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
            
            // Manejar verificacion SOLO si se modificó notasVerificacion
            if (pregunta.getNotasVerificacion() != null && 
                !pregunta.getNotasVerificacion().equals(preguntaExistente.getNotasVerificacion())) {
                
                System.out.println("🔄 [BACKEND] notasVerificacion ha cambiado, evaluando si actualizar verificacion...");
                
                String nuevasNotas = pregunta.getNotasVerificacion().trim();
                String notasExistentes = preguntaExistente.getNotasVerificacion() != null ? 
                    preguntaExistente.getNotasVerificacion().trim() : "";
                
                System.out.println("🔍 [BACKEND] Nuevas notas (trim): '" + nuevasNotas + "'");
                System.out.println("🔍 [BACKEND] Notas existentes (trim): '" + notasExistentes + "'");
                
                // Solo proceder si las nuevas notas no están vacías Y son diferentes de las existentes
                if (!nuevasNotas.isEmpty() && !nuevasNotas.equals(notasExistentes)) {
                    System.out.println("✅ [BACKEND] Condiciones cumplidas, actualizando verificacion...");
                    
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
                    pregunta.setVerificacion(preguntaExistente.getVerificacion());
                    System.out.println("⏸️ [BACKEND] No se cumplieron condiciones, manteniendo verificacion: " + preguntaExistente.getVerificacion());
                }
            } else {
                // Si notasVerificacion no se está modificando O viene null, mantener verificacion existente
                if (pregunta.getNotasVerificacion() == null) {
                    pregunta.setNotasVerificacion(preguntaExistente.getNotasVerificacion());
                }
                pregunta.setVerificacion(preguntaExistente.getVerificacion());
                System.out.println("🔒 [BACKEND] notasVerificacion no cambió, manteniendo verificacion: " + preguntaExistente.getVerificacion());
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
        
        // Construir query base
        StringBuilder query = new StringBuilder("UPDATE preguntas SET estado = ?");
        List<Object> parametros = new ArrayList<>();
        parametros.add(nuevoEstado.name());
        
        int paramIndex = 2;
        
        // Agregar campos adicionales según el nuevo estado
        if (nuevoEstado == EstadoPregunta.verificada) {
            query.append(", fecha_verificacion = ?");
            parametros.add(java.sql.Timestamp.valueOf(LocalDateTime.now()));
            paramIndex++;
            
            if (usuarioActual != null) {
                query.append(", verificacion_usuario_id = ?");
                parametros.add(usuarioActual.getId());
                paramIndex++;
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

        Pregunta pregunta = preguntaOpt.get();

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

        // Si llegamos aquí, es seguro eliminar
        preguntaRepository.deleteById(id);
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

        // Guardar el valor anterior de notasVerificacion para comparar
        String notasVerificacionAnterior = pregunta.getNotasVerificacion();
        
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
        
        // Manejar actualización del campo verificacion cuando se modifica notasVerificacion
        if (dto.getNotasVerificacion() != null && 
            !dto.getNotasVerificacion().equals(notasVerificacionAnterior)) {
            
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
        
        // CAMBIADO: Buscar solo preguntas disponibles o liberadas para crear nuevos cuestionarios
        // No mostrar preguntas usadas para evitar confusión
        return preguntaRepository.buscarPreguntasDisponibles(
            nivelEnum,
            factorEnum,
            (id != null && !id.isBlank()) ? id : null,
            (pregunta != null && !pregunta.isBlank()) ? pregunta : null,
            (respuesta != null && !respuesta.isBlank()) ? respuesta : null,
            (tematica != null && !tematica.isBlank()) ? tematica : null,
            Pregunta.EstadoPregunta.aprobada,
            pageable
        );
    }

    public void eliminarPorId(Long id) {
        preguntaRepository.deleteById(id);
    }

    public List<PreguntaDTO> filtrarPreguntasCompleto(String nivel, String factor, String estado, 
                                                     String tematica, String subtema, String pregunta, String respuesta) {
        // Convertir strings a enums
        Pregunta.NivelPregunta nivelEnum = null;
        Pregunta.FactorPregunta factorEnum = null;
        Pregunta.EstadoPregunta estadoEnum = null;
        
        try {
            if (nivel != null && !nivel.isBlank()) {
                nivelEnum = Pregunta.NivelPregunta.valueOf(nivel.startsWith("_") ? nivel : ("_"+nivel));
            }
        } catch (Exception ignored) {}
        
        try {
            if (factor != null && !factor.isBlank()) {
                factorEnum = Pregunta.FactorPregunta.valueOf(factor);
            }
        } catch (Exception ignored) {}
        
        try {
            if (estado != null && !estado.isBlank()) {
                estadoEnum = Pregunta.EstadoPregunta.valueOf(estado);
            }
        } catch (Exception ignored) {}
        
        // Usar el nuevo método del repository
        List<Pregunta> preguntas = preguntaRepository.filtrarTodas(
            nivelEnum,
            factorEnum,
            estadoEnum,
            (tematica != null && !tematica.isBlank()) ? tematica : null,
            (subtema != null && !subtema.isBlank()) ? subtema : null,
            (pregunta != null && !pregunta.isBlank()) ? pregunta : null,
            (respuesta != null && !respuesta.isBlank()) ? respuesta : null
        );
        
        return preguntas.stream().map(this::mapPreguntaToDTO).collect(java.util.stream.Collectors.toList());
    }

    // --- MÉTODO DE MAPEADO DTO ---
    private PreguntaDTO mapPreguntaToDTO(Pregunta p) {
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
        dto.setNotas(p.getNotas());
        dto.setFactor(p.getFactor());
        dto.setNotasVerificacion(p.getNotasVerificacion());
        dto.setNotasDireccion(p.getNotasDireccion());
        dto.setVerificacion(p.getVerificacion());
        dto.setFechaCreacion(p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null);
        dto.setFechaVerificacion(p.getFechaVerificacion() != null ? p.getFechaVerificacion().toString() : null);
        dto.setEstado(p.getEstado() != null ? p.getEstado().name() : null);
        return dto;
    }

    // Modificar obtenerTodas para devolver DTOs
    public List<PreguntaDTO> obtenerTodasDTO() {
        return obtenerTodas().stream().map(this::mapPreguntaToDTO).collect(java.util.stream.Collectors.toList());
    }

    // Modificar obtenerPorId para devolver DTO
    public Optional<PreguntaDTO> obtenerPorIdDTO(Long id) {
        return obtenerPorId(id).map(this::mapPreguntaToDTO);
    }
} 