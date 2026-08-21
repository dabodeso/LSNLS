package com.lsnls.config;

import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebConfigTest {

    @Test
    void configuraNegociacionConversoresFormattersYRecursos() {
        WebConfig config = new WebConfig();
        ReflectionTestUtils.setField(config, "uploadDirectory", "uploads");

        ContentNegotiationConfigurer negotiation = new ContentNegotiationConfigurer(new MockServletContext());
        config.configureContentNegotiation(negotiation);

        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        config.configureMessageConverters(converters);
        assertFalse(converters.isEmpty());

        DefaultFormattingConversionService registry = new DefaultFormattingConversionService();
        config.addFormatters(registry);
        assertTrue(registry.canConvert(String.class, com.lsnls.entity.Cuestionario.EstadoCuestionario.class));

        GenericWebApplicationContext ctx = new GenericWebApplicationContext();
        ctx.refresh();
        try {
            ResourceHandlerRegistry resources = new ResourceHandlerRegistry(ctx, new MockServletContext());
            config.addResourceHandlers(resources);
            assertTrue(resources.hasMappingForPattern("/uploads/**"));
        } finally {
            ctx.close();
        }
    }
}
