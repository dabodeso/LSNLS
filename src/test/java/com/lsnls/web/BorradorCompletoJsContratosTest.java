package com.lsnls.web;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BorradorCompletoJsContratosTest {

    @Test
    void combosYCuestionariosNoSalenDeBorradorSinCompleto() throws Exception {
        assumeTrue(nodeDisponible(), "Node no está en el PATH; se omite el contrato JS de borrador");
        Path script = resolverScript();
        assertTrue(Files.exists(script), "Falta " + script);

        ProcessBuilder pb = new ProcessBuilder("node", script.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        assertTrue(finished, "Node no terminó a tiempo. Salida:\n" + output);
        assertEquals(0, process.exitValue(), "Fallo el contrato JS de borrador:\n" + output);
        assertTrue(output.contains("OK borrador completo contratos"));
    }

    private static Path resolverScript() {
        Path desdeModulo = Paths.get("src", "test", "js", "borrador-completo-contratos.test.js");
        if (Files.exists(desdeModulo)) {
            return desdeModulo.toAbsolutePath();
        }
        return Paths.get("LSNLS", "src", "test", "js", "borrador-completo-contratos.test.js").toAbsolutePath();
    }

    private static boolean nodeDisponible() {
        try {
            Process process = new ProcessBuilder("node", "-v").redirectErrorStream(true).start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
