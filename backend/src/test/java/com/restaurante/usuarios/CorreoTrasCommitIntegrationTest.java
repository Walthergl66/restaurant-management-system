package com.restaurante.usuarios;

import com.restaurante.AbstractIntegrationTest;
import com.restaurante.SoporteRegistro;
import com.restaurante.usuarios.infrastructure.correo.EnvioCorreoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El correo sale después de confirmar la transacción (RF-45): una operación
 * revertida no envía nada y un proveedor caído no rompe una operación ya
 * confirmada. El emisor es un mock para observar cuándo se invoca.
 */
@AutoConfigureMockMvc
class CorreoTrasCommitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EmisorCorreo emisorCorreo;

    @Test
    @DisplayName("un evento publicado en una transacción revertida no envía correo")
    void noEnviaCorreoSiLaTransaccionSeRevierte() {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado -> {
            eventPublisher.publishEvent(new CodigoVerificacionGenerado("revertido@test.com", "123456"));
            estado.setRollbackOnly();
        });

        verify(emisorCorreo, never()).enviarCodigoVerificacion(eq("revertido@test.com"), anyString());
    }

    @Test
    @DisplayName("un evento publicado en una transacción confirmada sí envía el correo")
    void enviaCorreoAlConfirmarLaTransaccion() {
        new TransactionTemplate(transactionManager).executeWithoutResult(estado ->
                eventPublisher.publishEvent(
                        new CodigoVerificacionGenerado("confirmado@test.com", "654321")));

        verify(emisorCorreo).enviarCodigoVerificacion("confirmado@test.com", "654321");
    }

    @Test
    @DisplayName("un proveedor caído no rompe el registro y el reenvío vuelve a intentarlo")
    void unProveedorCaidoNoRompeElRegistro() throws Exception {
        doThrow(new EnvioCorreoException("Brevo respondió 500"))
                .when(emisorCorreo).enviarCodigoVerificacion(anyString(), anyString());
        String correo = registrar();

        // La cuenta existe y sigue pendiente de verificar: el fallo del correo
        // no revierte el alta.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Clave#123"}
                                """.formatted(correo)))
                .andExpect(status().isForbidden());

        // El usuario puede pedir el reenvío aunque el proveedor siga caído.
        mockMvc.perform(post("/api/v1/auth/reenviar-verificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted(correo)))
                .andExpect(status().isNoContent());

        verify(emisorCorreo, times(2)).enviarCodigoVerificacion(eq(correo), anyString());
    }

    @Test
    @DisplayName("la recuperación envía el token con la vigencia y el token restablece la contraseña")
    void laRecuperacionEnviaElTokenConSuVigencia() throws Exception {
        String correo = registrar();
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emisorCorreo).enviarCodigoVerificacion(eq(correo), codigo.capture());
        mockMvc.perform(post("/api/v1/auth/verificar-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","codigo":"%s"}
                                """.formatted(correo, codigo.getValue())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/solicitar-recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s"}
                                """.formatted(correo)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emisorCorreo).enviarRecuperacionPassword(
                eq(correo), token.capture(), eq(Duration.ofMinutes(30)));

        // El token que viajó en el correo es el que restablece la contraseña.
        mockMvc.perform(post("/api/v1/auth/restablecer-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","nuevaPassword":"Nueva#Clave1"}
                                """.formatted(token.getValue())))
                .andExpect(status().isNoContent());
    }

    private String registrar() throws Exception {
        long semilla = System.nanoTime();
        String correo = SoporteRegistro.correo(semilla);
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","nombre":"Cliente Correo","cedula":"%s","celular":"%s","password":"Clave#123"}
                                """.formatted(correo, SoporteRegistro.cedula(semilla),
                                SoporteRegistro.celular(semilla))))
                .andExpect(status().isCreated());
        verify(emisorCorreo, times(1)).enviarCodigoVerificacion(eq(correo), any());
        return correo;
    }
}
