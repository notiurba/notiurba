package com.notiurba.auth.recuperacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;

/** El enlace de recuperación sale por correo a la cuenta, con el enlace y su vigencia. */
class EnviadorEnlaceRecuperacionCorreoTest {

    private final EnviadorCorreo correo = mock(EnviadorCorreo.class);
    private final EnviadorEnlaceRecuperacionLog registro = mock(EnviadorEnlaceRecuperacionLog.class);
    private final EnviadorEnlaceRecuperacionCorreo enviador = new EnviadorEnlaceRecuperacionCorreo(correo, registro);

    @Test
    void mandaElEnlaceSoloALaCuenta() {
        enviador.enviar("ana@correo.com", "http://localhost:5173/app/nueva-contrasena?token=abc123");

        ArgumentCaptor<CorreoSaliente> enviado = ArgumentCaptor.forClass(CorreoSaliente.class);
        verify(correo).enviar(enviado.capture());
        CorreoSaliente c = enviado.getValue();
        assertThat(c.para()).containsExactly("ana@correo.com");
        assertThat(c.asunto()).isEqualTo("Restablece tu contraseña de NotiUrba");
        assertThat(c.texto()).contains("?token=abc123", "1 hora", "una vez", "ignora este mensaje");
        verify(registro).enviar("ana@correo.com", "http://localhost:5173/app/nueva-contrasena?token=abc123");
    }

    @Test
    void siElProveedorFallaLaExcepcionSube() {
        doThrow(new CorreoNoEnviadoException("Resend caído", true)).when(correo).enviar(any());

        assertThatThrownBy(() -> enviador.enviar("ana@correo.com", "http://x?token=t"))
                .isInstanceOf(CorreoNoEnviadoException.class);
    }
}
