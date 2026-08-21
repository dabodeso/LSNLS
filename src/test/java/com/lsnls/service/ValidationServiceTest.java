package com.lsnls.service;

import com.lsnls.entity.Combo;
import com.lsnls.entity.Concursante;
import com.lsnls.entity.Cuestionario;
import com.lsnls.entity.Jornada;
import com.lsnls.entity.Pregunta;
import com.lsnls.entity.PreguntaCombo;
import com.lsnls.entity.PreguntaCuestionario;
import com.lsnls.entity.Programa;
import com.lsnls.entity.Usuario;
import com.lsnls.repository.ComboRepository;
import com.lsnls.repository.ConcursanteRepository;
import com.lsnls.repository.CuestionarioRepository;
import com.lsnls.repository.JornadaRepository;
import com.lsnls.repository.PreguntaRepository;
import com.lsnls.repository.ProgramaRepository;
import com.lsnls.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private PreguntaRepository preguntaRepository;

    @Mock
    private CuestionarioRepository cuestionarioRepository;

    @Mock
    private ComboRepository comboRepository;

    @Mock
    private ConcursanteRepository concursanteRepository;

    @Mock
    private JornadaRepository jornadaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProgramaRepository programaRepository;

    @InjectMocks
    private ValidationService validationService;

    private TypedQuery<Long> query;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        query = mock(TypedQuery.class);
    }

    private void stubEntityManager(Long resultado) {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        org.mockito.Mockito.lenient().when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(resultado);
    }

    @Test
    void validarIntegridadPreguntaErroresBasicos() {
        Pregunta pregunta = new Pregunta();
        pregunta.setPregunta("corta");
        pregunta.setRespuesta("no");
        pregunta.setTematica("ab");

        ValidationService.ValidationResult result = validationService.validarIntegridadPregunta(pregunta);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("10 caracteres"));
        assertTrue(result.getErrorsAsString().contains("3 caracteres"));
        assertTrue(result.getErrorsAsString().contains("temática"));
    }

    @Test
    void validarIntegridadPreguntaExcesosYFechasYEstado() {
        Pregunta pregunta = new Pregunta();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2001; i++) {
            sb.append("a");
        }
        pregunta.setPregunta(sb.toString());
        StringBuilder resp = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            resp.append("b");
        }
        pregunta.setRespuesta(resp.toString());
        pregunta.setTematica("historia");
        pregunta.setFechaCreacion(LocalDateTime.now());
        pregunta.setFechaVerificacion(LocalDateTime.now().minusDays(1));
        pregunta.setEstado(Pregunta.EstadoPregunta.aprobada);
        pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.descartada);

        ValidationService.ValidationResult result = validationService.validarIntegridadPregunta(pregunta);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("2000"));
        assertTrue(result.getErrorsAsString().contains("500"));
        assertTrue(result.getErrorsAsString().contains("verificación"));
        assertTrue(result.getErrorsAsString().contains("aprobada"));
    }

    @Test
    void validarIntegridadPreguntaValidaConWarningDeUsuario() {
        Pregunta pregunta = preguntaValida();
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        pregunta.setCreacionUsuario(usuario);
        pregunta.setEstado(Pregunta.EstadoPregunta.aprobada);
        pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
        when(preguntaRepository.countByCreacionUsuario(usuario)).thenReturn(1001L);

        ValidationService.ValidationResult result = validationService.validarIntegridadPregunta(pregunta);

        assertTrue(result.isValid());
        assertTrue(result.getWarningsAsString().contains("1000 preguntas"));
    }

    @Test
    void validarIntegridadCuestionarioSinPreguntas() {
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setPreguntas(new HashSet<PreguntaCuestionario>());

        ValidationService.ValidationResult result = validationService.validarIntegridadCuestionario(cuestionario);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("al menos 1 pregunta"));
    }

    @Test
    void validarIntegridadCuestionarioNivelInvalidoYAdjudicadoSinJornada() {
        stubEntityManager(0L);
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setId(1L);
        cuestionario.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        Pregunta pregunta = preguntaValida();
        pregunta.setNivel(Pregunta.NivelPregunta._5LS);
        PreguntaCuestionario pc = new PreguntaCuestionario();
        pc.setPregunta(pregunta);
        Set<PreguntaCuestionario> set = new HashSet<PreguntaCuestionario>();
        set.add(pc);
        cuestionario.setPreguntas(set);

        ValidationService.ValidationResult result = validationService.validarIntegridadCuestionario(cuestionario);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("nivel inválido"));
        assertTrue(result.getErrorsAsString().contains("adjudicado"));
    }

    @Test
    void validarIntegridadCuestionarioValido() {
        stubEntityManager(1L);
        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setId(1L);
        cuestionario.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        Pregunta pregunta = preguntaValida();
        pregunta.setNivel(Pregunta.NivelPregunta._1LS);
        PreguntaCuestionario pc = new PreguntaCuestionario();
        pc.setPregunta(pregunta);
        Set<PreguntaCuestionario> set = new HashSet<PreguntaCuestionario>();
        set.add(pc);
        cuestionario.setPreguntas(set);

        ValidationService.ValidationResult result = validationService.validarIntegridadCuestionario(cuestionario);

        assertTrue(result.isValid());
    }

    @Test
    void validarIntegridadComboVacioYNivelYDuplicados() {
        Combo combo = new Combo();
        combo.setPreguntas(new HashSet<PreguntaCombo>());
        ValidationService.ValidationResult vacio = validationService.validarIntegridadCombo(combo);
        assertFalse(vacio.isValid());
        assertTrue(vacio.getErrorsAsString().contains("al menos una pregunta"));

        Combo combo2 = new Combo();
        Set<PreguntaCombo> set = new HashSet<PreguntaCombo>();
        set.add(preguntaCombo(Pregunta.NivelPregunta._1LS, "X2"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5LS, "X2"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5LS, "X3"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5NLS, "3"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5LS, "X"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5LS, "0"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5LS, "libre"));
        combo2.setPreguntas(set);

        ValidationService.ValidationResult result = validationService.validarIntegridadCombo(combo2);
        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("nivel"));
        assertTrue(result.getErrorsAsString().contains("X2 duplicado"));
        assertTrue(result.getErrorsAsString().contains("X3 duplicado"));
        assertTrue(result.getErrorsAsString().contains("X duplicado"));
    }

    @Test
    void validarIntegridadComboValido() {
        Combo combo = new Combo();
        Set<PreguntaCombo> set = new HashSet<PreguntaCombo>();
        set.add(preguntaCombo(Pregunta.NivelPregunta._5LS, "2"));
        set.add(preguntaCombo(Pregunta.NivelPregunta._5NLS, "X3"));
        combo.setPreguntas(set);

        ValidationService.ValidationResult result = validationService.validarIntegridadCombo(combo);

        assertTrue(result.isValid());
    }

    @Test
    void validarIntegridadConcursanteErrores() {
        Concursante concursante = new Concursante();
        concursante.setNombre("A");
        concursante.setNumeroConcursante(7);
        when(concursanteRepository.countByNumeroConcursante(7)).thenReturn(1L);
        Cuestionario cuest = new Cuestionario();
        cuest.setEstado(Cuestionario.EstadoCuestionario.borrador);
        concursante.setCuestionario(cuest);
        Combo combo = new Combo();
        combo.setEstado(Combo.EstadoCombo.borrador);
        concursante.setCombo(combo);

        ValidationService.ValidationResult result = validationService.validarIntegridadConcursante(concursante);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("nombre"));
        assertTrue(result.getErrorsAsString().contains("número"));
        assertTrue(result.getErrorsAsString().contains("cuestionario"));
        assertTrue(result.getErrorsAsString().contains("combo"));
    }

    @Test
    void validarIntegridadConcursanteUpdateDuplicado() {
        stubEntityManager(1L);
        Concursante concursante = new Concursante();
        concursante.setId(2L);
        concursante.setNombre("Ana Pérez");
        concursante.setNumeroConcursante(7);
        Cuestionario cuest = new Cuestionario();
        cuest.setEstado(Cuestionario.EstadoCuestionario.aprobado);
        concursante.setCuestionario(cuest);
        Combo combo = new Combo();
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        concursante.setCombo(combo);

        ValidationService.ValidationResult result = validationService.validarIntegridadConcursante(concursante);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("otro concursante"));
    }

    @Test
    void validarIntegridadConcursanteValido() {
        Concursante concursante = new Concursante();
        concursante.setNombre("Ana Pérez");
        Cuestionario cuest = new Cuestionario();
        cuest.setEstado(Cuestionario.EstadoCuestionario.grabado);
        concursante.setCuestionario(cuest);
        Combo combo = new Combo();
        combo.setEstado(Combo.EstadoCombo.grabado);
        concursante.setCombo(combo);

        ValidationService.ValidationResult result = validationService.validarIntegridadConcursante(concursante);

        assertTrue(result.isValid());
    }

    @Test
    void validarIntegridadJornadaLimitesFechasYEstados() {
        Jornada jornada = new Jornada();
        Set<Cuestionario> cuestionarios = new HashSet<Cuestionario>();
        for (int i = 1; i <= 6; i++) {
            Cuestionario c = new Cuestionario();
            c.setId((long) i);
            c.setEstado(Cuestionario.EstadoCuestionario.borrador);
            cuestionarios.add(c);
        }
        jornada.setCuestionarios(cuestionarios);
        Set<Combo> combos = new HashSet<Combo>();
        for (int i = 1; i <= 6; i++) {
            Combo c = new Combo();
            c.setId((long) i);
            c.setEstado(Combo.EstadoCombo.borrador);
            combos.add(c);
        }
        jornada.setCombos(combos);
        jornada.setFechaJornada(LocalDate.now().minusYears(2));

        ValidationService.ValidationResult result = validationService.validarIntegridadJornada(jornada);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("5 cuestionarios"));
        assertTrue(result.getErrorsAsString().contains("5 combos"));
        assertTrue(result.getWarningsAsString().contains("antigua"));
        assertTrue(result.getWarningsAsString().contains("adjudicado"));
    }

    @Test
    void validarIntegridadJornadaFechaFutura() {
        Jornada jornada = new Jornada();
        jornada.setFechaJornada(LocalDate.now().plusYears(3));

        ValidationService.ValidationResult result = validationService.validarIntegridadJornada(jornada);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("2 años"));
    }

    @Test
    void validarIntegridadJornadaValida() {
        Jornada jornada = new Jornada();
        jornada.setFechaJornada(LocalDate.now().plusDays(1));
        Cuestionario c = new Cuestionario();
        c.setId(1L);
        c.setEstado(Cuestionario.EstadoCuestionario.adjudicado);
        jornada.setCuestionarios(CollectionsSingleton(c));
        Combo combo = new Combo();
        combo.setId(1L);
        combo.setEstado(Combo.EstadoCombo.adjudicado);
        Set<Combo> combos = new HashSet<Combo>();
        combos.add(combo);
        jornada.setCombos(combos);

        ValidationService.ValidationResult result = validationService.validarIntegridadJornada(jornada);

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void validarIntegridadProgramaDuplicadoYRangos() {
        Programa programa = new Programa();
        programa.setTemporada(2019);
        when(programaRepository.countByTemporada(2019)).thenReturn(1L);
        programa.setFechaEmision(LocalDate.now().minusYears(6));
        programa.setDuracionAcumulada(LocalTime.of(5, 0));

        ValidationService.ValidationResult result = validationService.validarIntegridadPrograma(programa);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("Ya existe un programa"));
        assertTrue(result.getErrorsAsString().contains("2020"));
        assertTrue(result.getWarningsAsString().contains("antigua"));
        assertTrue(result.getWarningsAsString().contains("4 horas"));
    }

    @Test
    void validarIntegridadProgramaUpdateDuplicadoYFechaFutura() {
        stubEntityManager(1L);
        Programa programa = new Programa();
        programa.setId(3L);
        programa.setTemporada(LocalDate.now().getYear());
        programa.setFechaEmision(LocalDate.now().plusYears(3));

        ValidationService.ValidationResult result = validationService.validarIntegridadPrograma(programa);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("otro programa"));
        assertTrue(result.getErrorsAsString().contains("2 años"));
    }

    @Test
    void validarIntegridadProgramaTemporadaDemasiadoFutura() {
        Programa programa = new Programa();
        programa.setTemporada(LocalDate.now().getYear() + 6);
        when(programaRepository.countByTemporada(any())).thenReturn(0L);

        ValidationService.ValidationResult result = validationService.validarIntegridadPrograma(programa);

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("temporada"));
    }

    @Test
    void validarIntegridadProgramaValido() {
        Programa programa = new Programa();
        programa.setTemporada(LocalDate.now().getYear());
        when(programaRepository.countByTemporada(programa.getTemporada())).thenReturn(0L);
        programa.setFechaEmision(LocalDate.now().plusDays(10));
        programa.setDuracionAcumulada(LocalTime.of(1, 0));

        ValidationService.ValidationResult result = validationService.validarIntegridadPrograma(programa);

        assertTrue(result.isValid());
    }

    @Test
    void validarSistemaCompletoSinInconsistencias() {
        stubEntityManager(0L);

        ValidationService.ValidationResult result = validationService.validarSistemaCompleto();

        assertTrue(result.isValid());
        assertTrue(result.getWarningsAsString().contains("completada"));
    }

    @Test
    void validarSistemaCompletoConInconsistencias() {
        stubEntityManager(2L);

        ValidationService.ValidationResult result = validationService.validarSistemaCompleto();

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("pregunta-cuestionario"));
        assertTrue(result.getErrorsAsString().contains("pregunta-combo"));
        assertTrue(result.getErrorsAsString().contains("adjudicado"));
    }

    @Test
    void validarSistemaCompletoCapturaExcepcion() {
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenThrow(new RuntimeException("jpql"));

        ValidationService.ValidationResult result = validationService.validarSistemaCompleto();

        assertFalse(result.isValid());
        assertTrue(result.getErrorsAsString().contains("Error durante la validación"));
    }

    private Pregunta preguntaValida() {
        Pregunta pregunta = new Pregunta();
        pregunta.setPregunta("¿Cuál es la capital de España?");
        pregunta.setRespuesta("Madrid");
        pregunta.setTematica("Geografía");
        pregunta.setNivel(Pregunta.NivelPregunta._1LS);
        pregunta.setEstado(Pregunta.EstadoPregunta.borrador);
        pregunta.setEstadoDisponibilidad(Pregunta.EstadoDisponibilidad.disponible);
        return pregunta;
    }

    private PreguntaCombo preguntaCombo(Pregunta.NivelPregunta nivel, String factor) {
        Pregunta pregunta = preguntaValida();
        pregunta.setNivel(nivel);
        PreguntaCombo pc = new PreguntaCombo();
        pc.setPregunta(pregunta);
        pc.setFactorMultiplicacion(factor);
        return pc;
    }

    private Set<Cuestionario> CollectionsSingleton(Cuestionario c) {
        Set<Cuestionario> set = new HashSet<Cuestionario>();
        set.add(c);
        return set;
    }
}
