package com.lsnls.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonConfigTest {

    private JacksonConfig config;

    @BeforeEach
    void setUp() {
        config = new JacksonConfig();
    }

    @Test
    void objectMapper_registraJavaTimeYNoUsaTimestamps() throws Exception {
        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        String json = mapper.writeValueAsString(LocalDate.of(2024, 6, 15));
        assertTrue(json.contains("2024-06-15"));
    }

    @Test
    void mappingJackson2HttpMessageConverter_usaElMapperYJsonUtf8() {
        ObjectMapper mapper = config.objectMapper();
        MappingJackson2HttpMessageConverter converter = config.mappingJackson2HttpMessageConverter(mapper);

        assertEquals(mapper, converter.getObjectMapper());
        assertTrue(converter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));
        assertTrue(converter.getSupportedMediaTypes().contains(MediaType.valueOf("application/json;charset=UTF-8")));
    }
}
