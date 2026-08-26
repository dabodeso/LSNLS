package com.lsnls.config;

import com.lsnls.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorResponseAdviceWebTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FalloController())
                .setControllerAdvice(new ErrorResponseAdvice())
                .build();
    }

    @Test
    void cuerpoStringConSqlSeSustituye() throws Exception {
        mockMvc.perform(get("/test-error/sql-string"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("SQLException"))))
                .andExpect(content().string(not(containsString("Duplicate entry"))))
                .andExpect(content().string(MensajesUsuario.RELACIONADOS));
    }

    @Test
    void apiResponseConHibernateSeSustituye() throws Exception {
        mockMvc.perform(get("/test-error/sql-api"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value(MensajesUsuario.RELACIONADOS))
                .andExpect(jsonPath("$.mensaje").value(not(containsString("hibernate"))));
    }

    @RestController
    @RequestMapping("/test-error")
    static class FalloController {

        @GetMapping("/sql-string")
        public ResponseEntity<String> sqlString() {
            return ResponseEntity.status(500).body("java.sql.SQLException: Duplicate entry");
        }

        @GetMapping("/sql-api")
        public ResponseEntity<ApiResponse<Void>> sqlApi() {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("org.hibernate.exception.ConstraintViolationException: duplicate"));
        }
    }
}
