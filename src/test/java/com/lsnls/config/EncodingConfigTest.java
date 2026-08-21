package com.lsnls.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncodingConfigTest {

    private EncodingConfig config;

    @BeforeEach
    void setUp() {
        config = new EncodingConfig();
    }

    @Test
    void characterEncodingFilter_fuerzaUtf8() {
        CharacterEncodingFilter filter = config.characterEncodingFilter();

        assertEquals("UTF-8", filter.getEncoding());
        assertTrue(filter.isForceRequestEncoding());
        assertTrue(filter.isForceResponseEncoding());
    }

    @Test
    void configureMessageConverters_anadeStringUtf8SinAcceptCharset() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();

        config.configureMessageConverters(converters);

        assertEquals(1, converters.size());
        assertTrue(converters.get(0) instanceof StringHttpMessageConverter);
        StringHttpMessageConverter stringConverter = (StringHttpMessageConverter) converters.get(0);
        assertEquals(StandardCharsets.UTF_8, stringConverter.getDefaultCharset());
        assertFalse((Boolean) org.springframework.test.util.ReflectionTestUtils.getField(stringConverter, "writeAcceptCharset"));
    }
}
