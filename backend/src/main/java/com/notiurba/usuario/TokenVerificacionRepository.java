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

    @Modifying
    @Query("""
            update TokenVerificacion t set t.usadoEn = null
             where t.usuario = :usuario and t.tipoToken = :tipoToken
               and t.tokenHash = :tokenHash and t.usadoEn = :marcaPendiente
               and t.expiraEn > :ahora""")
    int confirmarPendiente(@Param("usuario") Usuario usuario, @Param("tipoToken") TipoToken tipoToken,
            @Param("tokenHash") String tokenHash, @Param("marcaPendiente") OffsetDateTime marcaPendiente,
            @Param("ahora") OffsetDateTime ahora);

    @Modifying
    @Query("""
            update TokenVerificacion t set t.usadoEn = :ahora
             where t.usuario = :usuario and t.tipoToken = :tipoToken
               and t.tokenHash <> :tokenHashActivo and t.usadoEn is null
               and t.expiraEn > :ahora""")
    int invalidarVigentesExcepto(@Param("usuario") Usuario usuario, @Param("tipoToken") TipoToken tipoToken,
            @Param("tokenHashActivo") String tokenHashActivo, @Param("ahora") OffsetDateTime ahora);

    @Modifying
    @Query("""
            delete from TokenVerificacion t
             where t.usuario = :usuario and t.tipoToken = :tipoToken
               and t.tokenHash = :tokenHash and t.usadoEn = :marcaPendiente""")
    int eliminarPendiente(@Param("usuario") Usuario usuario, @Param("tipoToken") TipoToken tipoToken,
            @Param("tokenHash") String tokenHash, @Param("marcaPendiente") OffsetDateTime marcaPendiente);

    /** Reconcilia entregas que agotaron sus reintentos: pendiente se representa por usado_en = expira_en. */
    @Modifying
    @Query("""
            delete from TokenVerificacion t
             where t.usuario = :usuario and t.tipoToken = :tipoToken
               and t.usadoEn = t.expiraEn and t.creadoEn < :limite""")
    int eliminarPendientesAnterioresA(@Param("usuario") Usuario usuario,
            @Param("tipoToken") TipoToken tipoToken, @Param("limite") OffsetDateTime limite);
}
