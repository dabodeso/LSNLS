package com.lsnls.service;

import com.lsnls.entity.ConfiguracionGlobal;
import com.lsnls.repository.ConfiguracionGlobalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConfiguracionGlobalService {

    @Autowired
    private ConfiguracionGlobalRepository configuracionRepository;

    public List<ConfiguracionGlobal> findAll() {
        return configuracionRepository.findAll();
    }

    public String obtenerValor(String clave, String valorPorDefecto) {
        Optional<ConfiguracionGlobal> config = configuracionRepository.findByClave(clave);
        return config.map(ConfiguracionGlobal::getValor).orElse(valorPorDefecto);
    }

    public ConfiguracionGlobal actualizarConfiguracion(String clave, String valor, String descripcion) {
        Optional<ConfiguracionGlobal> existente = configuracionRepository.findByClave(clave);
        
        if (existente.isPresent()) {
            ConfiguracionGlobal config = existente.get();
            config.setValor(valor);
            if (descripcion != null) {
                config.setDescripcion(descripcion);
            }
            return configuracionRepository.save(config);
        } else {
            ConfiguracionGlobal nuevaConfig = new ConfiguracionGlobal(clave, valor, descripcion);
            return configuracionRepository.save(nuevaConfig);
        }
    }

    public String getDuracionObjetivo() {
        return obtenerValor("DURACION_OBJETIVO_PROGRAMA", "1h 5m");
    }

    public void setDuracionObjetivo(String duracion) {
        actualizarConfiguracion("DURACION_OBJETIVO_PROGRAMA", duracion, "Duración objetivo por defecto para programas");
    }
} 