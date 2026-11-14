package com.notiurba.auth.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.notiurba.correo.CorreoNoEnviadoException;
import com.notiurba.correo.CorreoSaliente;
import com.notiurba.correo.EnviadorCorreo;

class EnvioCorreoVerificacionTest {

    private final EnviadorCorreo correo = mock(EnviadorCorreo.class);
    private final EnvioCorreoVerificacion envio = new EnvioCorreoVerificacion(correo);
    private final CorreoVerificacionPendiente pendiente =
            new CorreoVerificacionPendiente("ana@x.com", "Ana", "https://app.test/verificar?token=abc");

    @Test
    void enviaElEnlaceAlCorreoDeLaCuenta() {
        envio.enviar(pendiente);

        var enviado = ArgumentCaptor.forClass(CorreoSaliente.class);
        verify(correo).enviar(enviado.capture());
        assertThat(enviado.getValue().para()).containsExactly("ana@x.com");
        assertThat(enviado.getValue().texto()).contains("Hola Ana").contains("https://app.test/verificar?token=abc")
                .contains("24 horas");
    }

    @Test
    void siFallaElEnvioNoLanzaNada() {
        when(correo.enviar(any())).thenThrow(new CorreoNoEnviadoException("caído", true));
        assertThatCode(() -> envio.enviar(pendiente)).doesNotThrowAnyException();
    }
}
