package com.notiurba.reporte;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.notiurba.foto.AlmacenFotos;
import com.notiurba.foto.FotoGuardada;
import com.notiurba.foto.TipoImagen;
import com.notiurba.usuario.UsuarioRepository;
import com.notiurba.web.error.ApiException;
import com.notiurba.web.error.Codigos;

/**
 * Sube la foto de un reporte propio: valida el archivo, la guarda en el almacén (Cloudinary) y deja
 * su enlace y su identificador en evidencia_reporte. Hasta 3 fotos por reporte (orden 1 a 3).
 *
 * La fila del reporte queda bloqueada mientras se sube: dos subidas a la vez del mismo reporte no
 * pueden pasarse de 3 ni repetir el orden. El costo es una conexión ocupada durante la subida.
 */
@Service
public class FotoServicio {

    private static final Logger log = LoggerFactory.getLogger(FotoServicio.class);

    static final long MAXIMO_BYTES = 1_048_576;   // 1 MB (RF-06)
    static final int MAXIMO_FOTOS = 3;

    private final ReporteRepositorio repositorio;
    private final UsuarioRepository usuarios;
    private final AlmacenFotos almacen;

    public FotoServicio(ReporteRepositorio repositorio, UsuarioRepository usuarios, AlmacenFotos almacen) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.almacen = almacen;
    }

    @Transactional
    public FotoSubidaResponse subir(String correoUsuario, String codigo, MultipartFile foto) {
        String normalizado = ReporteServicio.normalizarCodigo(codigo);
        long idUsuario = usuarios.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, Codigos.NO_AUTENTICADO,
                        Codigos.MSG_NO_AUTENTICADO))
                .getIdUsuario();
        // Primero el dueño: quien no es el autor recibe el mismo 404 que si el reporte no existiera
        long idReporte = repositorio.bloquearReportePropio(idUsuario, normalizado)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, Codigos.NO_ENCONTRADO,
                        ReporteServicio.NO_ENCONTRADO));

        byte[] contenido = leer(foto);
        String tipo = TipoImagen.detectar(contenido).orElseThrow(() -> new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, Codigos.FORMATO_NO_SOPORTADO,
                "La foto debe ser JPG o PNG", "foto"));
        int orden = primerOrdenLibre(repositorio.ordenesDeFotos(idReporte));

        FotoGuardada guardada = almacen.guardar(contenido, tipo);
        try {
            repositorio.insertarFoto(idReporte, idUsuario, guardada.url(), guardada.idPublico(), orden);
        } catch (RuntimeException fallo) {
            // La foto ya está en Cloudinary pero no quedó registrada: se borra para no dejarla huérfana
            try {
                almacen.borrar(guardada.idPublico());
            } catch (RuntimeException alBorrar) {
                log.error("No se pudo borrar la foto huérfana {}", guardada.idPublico(), alBorrar);
            }
            throw fallo;
        }
        return new FotoSubidaResponse(normalizado, new ReporteDetalleResponse.Foto(orden, guardada.url()));
    }

    private static byte[] leer(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "Elige una foto", "foto");
        }
        if (foto.getSize() > MAXIMO_BYTES) {
            throw tamano();
        }
        try {
            byte[] contenido = foto.getBytes();
            if (contenido.length > MAXIMO_BYTES) {
                throw tamano();
            }
            return contenido;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, Codigos.VALIDACION, "No pudimos leer la foto", "foto");
        }
    }

    private static ApiException tamano() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, Codigos.ARCHIVO_MUY_GRANDE,
                "La foto pesa más de 1 MB. Elige una más liviana.", "foto");
    }

    /** El menor orden de 1 a 3 que no esté usado; si los tres están ocupados, 409. */
    static int primerOrdenLibre(List<Integer> usados) {
        for (int orden = 1; orden <= MAXIMO_FOTOS; orden++) {
            if (!usados.contains(orden)) {
                return orden;
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, Codigos.FOTOS_MAXIMO,
                "Este reporte ya tiene " + MAXIMO_FOTOS + " fotos", "foto");
    }
}
