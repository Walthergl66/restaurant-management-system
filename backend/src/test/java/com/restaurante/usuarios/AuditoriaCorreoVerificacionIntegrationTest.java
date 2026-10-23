package com.restaurante.usuarios;

import com.restaurante.AbstractIntegracionApi;
import com.restaurante.SoporteRegistro;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RF-45 + RNF-10/RNF-11: el envío (o el fallo) del código de verificación
 * queda en el historial de auditoría con categoría SEGURIDAD, sin exponer
 * jamás el código, y un fallo del proveedor no revierte el registro.
 */
class AuditoriaCorreoVerificacionIntegrationTest extends AbstractIntegracionApi {

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @Test
    void auditoriaElEnvioExitosoSinExponerElCodigo() throws Exception {
        long semilla = System.nanoTime();
        String correo = SoporteRegistro.correo(semilla);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Auditoria","cedula":"%s",
                                 "celular":"%s","password":"Clave#123"}
                                """.formatted(correo, SoporteRegistro.cedula(semilla),
                                SoporteRegistro.celular(semilla))))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emisorCorreo).enviarCodigoVerificacion(eq(correo), codigo.capture());

        JsonNode eventos = auditoriaSeguridad();
        boolean auditado = false;
        for (JsonNode evento : eventos) {
            if (!"CORREO_VERIFICACION_ENVIADO".equals(evento.path("tipo").asText())) {
                continue;
            }
            JsonNode detalle = objectMapper.readTree(evento.path("detalle").asText());
            if (detalle.path("correo").asText().equals(correo)) {
                assertEquals("USUARIO", evento.path("entidad").asText());
                assertEquals("SEGURIDAD", evento.path("categoria").asText());
                auditado = true;
            }
        }
        assertTrue(auditado, "el envío debe quedar en la auditoría con el correo de destino");
        // RNF-10: el código de un solo uso no aparece en ninguna parte del historial.
        assertFalse(eventos.toString().contains(codigo.getValue()),
                "el código no debe aparecer en la auditoría");
    }

    @Test
    void auditoriaElFalloDelCanalSinRevirtirElRegistro() throws Exception {
        long semilla = System.nanoTime();
        String correo = SoporteRegistro.correo(semilla);
        doThrow(new CorreoNoEnviadoException("Resend respondió 503", null))
                .when(emisorCorreo).enviarCodigoVerificacion(any(), any());

        // RNF-11: el registro responde bien aunque el proveedor de correo falle.
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Fallo","cedula":"%s",
                                 "celular":"%s","password":"Clave#123"}
                                """.formatted(correo, SoporteRegistro.cedula(semilla),
                                SoporteRegistro.celular(semilla))))
                .andExpect(status().isCreated());
        assertTrue(usuarioRepository.findByUsername(correo).orElseThrow().isActivo(),
                "la cuenta debe quedar registrada pese al fallo del correo");

        JsonNode eventos = auditoriaSeguridad();
        boolean auditado = false;
        for (JsonNode evento : eventos) {
            if (!"CORREO_VERIFICACION_FALLIDO".equals(evento.path("tipo").asText())) {
                continue;
            }
            JsonNode detalle = objectMapper.readTree(evento.path("detalle").asText());
            if (detalle.path("correo").asText().equals(correo)) {
                assertTrue(detalle.path("motivo").asText().contains("503"),
                        "el motivo del canal debe quedar registrado");
                auditado = true;
            }
        }
        assertTrue(auditado, "el fallo del canal debe quedar en la auditoría");
    }

    private JsonNode auditoriaSeguridad() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", "Bearer " + tokenAdmin())
                        .param("desde", Instant.now().minusSeconds(60).toString())
                        .param("hasta", "2030-01-01T00:00:00Z")
                        .param("categoria", "SEGURIDAD"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }
}
