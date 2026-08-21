package com.lsnls.service;

import com.lsnls.entity.ConfiguracionGlobal;
import com.lsnls.repository.ConfiguracionGlobalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionGlobalServiceTest {

    @Mock
    private ConfiguracionGlobalRepository configuracionRepository;

    @InjectMocks
    private ConfiguracionGlobalService service;

    @Test
    void findAll() {
        when(configuracionRepository.findAll()).thenReturn(Collections.emptyList());
        List<ConfiguracionGlobal> result = service.findAll();
        assertEquals(0, result.size());
    }

    @Test
    void obtenerValor_existenteYDefault() {
        ConfiguracionGlobal config = new ConfiguracionGlobal("K", "V", "d");
        when(configuracionRepository.findByClave("K")).thenReturn(Optional.of(config));
        assertEquals("V", service.obtenerValor("K", "def"));

        when(configuracionRepository.findByClave("NO")).thenReturn(Optional.empty());
        assertEquals("def", service.obtenerValor("NO", "def"));
    }

    @Test
    void actualizarConfiguracion_existente() {
        ConfiguracionGlobal config = new ConfiguracionGlobal("K", "old", "desc");
        when(configuracionRepository.findByClave("K")).thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionGlobal.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionGlobal saved = service.actualizarConfiguracion("K", "new", "nueva desc");
        assertEquals("new", saved.getValor());
        assertEquals("nueva desc", saved.getDescripcion());
    }

    @Test
    void actualizarConfiguracion_existenteSinDescripcion() {
        ConfiguracionGlobal config = new ConfiguracionGlobal("K", "old", "desc");
        when(configuracionRepository.findByClave("K")).thenReturn(Optional.of(config));
        when(configuracionRepository.save(any(ConfiguracionGlobal.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionGlobal saved = service.actualizarConfiguracion("K", "new", null);
        assertEquals("desc", saved.getDescripcion());
    }

    @Test
    void actualizarConfiguracion_nueva() {
        when(configuracionRepository.findByClave("NUEVA")).thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionGlobal.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionGlobal saved = service.actualizarConfiguracion("NUEVA", "v", "d");
        assertEquals("NUEVA", saved.getClave());
        assertEquals("v", saved.getValor());
    }

    @Test
    void getDuracionObjetivo_default() {
        when(configuracionRepository.findByClave("DURACION_OBJETIVO_PROGRAMA")).thenReturn(Optional.empty());
        assertEquals("45m", service.getDuracionObjetivo());
    }

    @Test
    void setDuracionObjetivo_valida() {
        when(configuracionRepository.findByClave("DURACION_OBJETIVO_PROGRAMA")).thenReturn(Optional.empty());
        when(configuracionRepository.save(any(ConfiguracionGlobal.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setDuracionObjetivo("45m");
        service.setDuracionObjetivo("1h 5m");

        ArgumentCaptor<ConfiguracionGlobal> captor = ArgumentCaptor.forClass(ConfiguracionGlobal.class);
        verify(configuracionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
    }

    @Test
    void setDuracionObjetivo_invalida() {
        assertThrows(IllegalArgumentException.class, () -> service.setDuracionObjetivo(null));
        assertThrows(IllegalArgumentException.class, () -> service.setDuracionObjetivo("abc"));
        assertThrows(IllegalArgumentException.class, () -> service.setDuracionObjetivo("  "));
    }
}
