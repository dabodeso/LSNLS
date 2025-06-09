package com.lsnls.config;

import com.lsnls.entity.Cuestionario.EstadoCuestionario;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class EstadoCuestionarioConverter implements Converter<String, EstadoCuestionario> {
    @Override
    public EstadoCuestionario convert(String source) {
        try {
            return EstadoCuestionario.valueOf(source.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
} 