package com.notiurba.auth.google;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifica el ID token que entrega «Continuar con Google» sin librerías de Google:
 * firma con las JWK públicas de Google, exp, iss, aud = nuestro client-id y email_verified.
 */
@Component
public class GoogleTokenVerificador {

    static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    static final Set<String> EMISORES = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decodificador;

    @Autowired
    public GoogleTokenVerificador(@Value("${notiurba.google.client-id}") String clientId) {
        this(NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build(), clientId);
    }

    /** Las pruebas pasan un decodificador con su propia clave pública; las validaciones son las mismas. */
    GoogleTokenVerificador(NimbusJwtDecoder decodificador, String clientId) {
        decodificador.setJwtValidator(validador(clientId));
        this.decodificador = decodificador;
    }

    private static OAuth2TokenValidator<Jwt> validador(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtClaimValidator<Object>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        iss -> iss != null && EMISORES.contains(iss.toString())),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(clientId)),
                new JwtClaimValidator<Object>("email", Objects::nonNull),
                // Google lo envía como booleano; se acepta también el texto "true"
                new JwtClaimValidator<Object>("email_verified",
                        verificado -> Boolean.TRUE.equals(verificado) || "true".equals(verificado)));
    }

    /**
     * @throws JwtException si la firma, la vigencia o algún claim no es válido
     */
    public PerfilGoogle verificar(String credential) {
        Jwt jwt = decodificador.decode(credential);
        return new PerfilGoogle(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("name"));
    }
}
