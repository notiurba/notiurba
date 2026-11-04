package com.notiurba.usuario;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TokenVerificacionRepository extends JpaRepository<TokenVerificacion, Long> {

    /** SELECT ... FOR UPDATE: dos usos simultáneos del mismo enlace no pasan ambos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TokenVerificacion> findByTokenHashAndTipoToken(String tokenHash, TipoToken tipoToken);

    long countByUsuarioAndTipoTokenAndCreadoEnAfter(Usuario usuario, TipoToken tipoToken, OffsetDateTime desde);

    /** Deja sin efecto los tokens vigentes del usuario (marcados como usados). */
    @Modifying
    @Query("""
            update TokenVerificacion t set t.usadoEn = :ahora
             where t.usuario = :usuario and t.tipoToken = :tipoToken
               and t.usadoEn is null and t.expiraEn > :ahora""")
    int invalidarVigentes(@Param("usuario") Usuario usuario, @Param("tipoToken") TipoToken tipoToken,
            @Param("ahora") OffsetDateTime ahora);
}
